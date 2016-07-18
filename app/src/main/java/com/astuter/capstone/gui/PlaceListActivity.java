package com.astuter.capstone.gui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorViewHolder;
import com.astuter.capstone.R;
import com.astuter.capstone.config.Config;
import com.astuter.capstone.provider.PlaceContract;
import com.astuter.capstone.remote.NearbyPlaceResultReceiver;
import com.astuter.capstone.remote.NearbyPlaceService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;

;

/**
 * An activity representing a list of Places. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link PlaceDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class PlaceListActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, NearbyPlaceResultReceiver.Receiver,
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private NearbyPlaceResultReceiver mNearbyPlaceResultReceiver;
    private SharedPreferences preferences;
    private RecyclerView mRecyclerView;
    private PlaceListAdapter mPlaceListAdapter;
    private ProgressDialog progress;

    private String[] PLACE_TYPE;
    private final int PERMISSION_ACCESS_FINE_LOCATION = 1;
    private final int PLACE_LOADER = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_list);

        preferences = PreferenceManager.getDefaultSharedPreferences(PlaceListActivity.this);
        PLACE_TYPE = getResources().getStringArray(R.array.nearby_places_key);

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Please Wait ...");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add Spinner programmatically to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(),
                    R.array.nearby_places,
                    R.layout.spinner_item_layout);
            Spinner navigationSpinner = new Spinner(getSupportActionBar().getThemedContext());
            navigationSpinner.setAdapter(spinnerAdapter);
            navigationSpinner.setSelection(Arrays.asList(PLACE_TYPE).indexOf(preferences.getString(Config.PREF_PLACE_TYPE, "amusement_park")));
            toolbar.addView(navigationSpinner, 0);

            navigationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    preferences.edit().putString(Config.PREF_PLACE_TYPE, PLACE_TYPE[position]).apply();

                    Log.e("navigationSpinner", "you selected:" + PLACE_TYPE[position]);
                    startNearbyPlaceService();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                // Pass the Place data and current location to show on map
                Bundle bundle = new Bundle();
                bundle.putParcelable(Config.KEY_CURRENT_LOCATION, mCurrentLocation);
                Intent intent = new Intent(PlaceListActivity.this, MapActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_FINE_LOCATION);
        }
        setUpGoogleApiClient();
        createLocationRequest();

        mRecyclerView = (RecyclerView) findViewById(R.id.place_list);
        mRecyclerView.setHasFixedSize(true);
        mPlaceListAdapter = new PlaceListAdapter(PlaceListActivity.this);
        mRecyclerView.setAdapter(mPlaceListAdapter);

        // User loader to fetch data from SQLite
        getSupportLoaderManager().initLoader(PLACE_LOADER, null, PlaceListActivity.this);

        if (findViewById(R.id.place_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Uri CONTENT_URI = PlaceContract.PlaceEntry.CONTENT_URI;

        return new CursorLoader(PlaceListActivity.this, CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        if (!cursor.isClosed()) {
            cursor.moveToFirst();
            mPlaceListAdapter.swapCursor(cursor);
            mPlaceListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the Cursor is being placed in a CursorAdapter, you should use the
        // swapCursor(null) method to remove any references it has to the
        // Loader's data.
//        mPlaceListAdapter.swapCursor(null);
    }

    private void setUpGoogleApiClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(PlaceListActivity.this)
                    .addConnectionCallbacks(PlaceListActivity.this)
                    .addOnConnectionFailedListener(PlaceListActivity.this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void startNearbyPlaceService() {
        /* Starting Download Service */
        mNearbyPlaceResultReceiver = new NearbyPlaceResultReceiver(new Handler());
        mNearbyPlaceResultReceiver.setReceiver(PlaceListActivity.this);
        Intent intent = new Intent(Intent.ACTION_SYNC, null, this, NearbyPlaceService.class);

        /* Send optional extras to Download IntentService */
        intent.putExtra(Config.KEY_PLACE_RESULT_RECEIVER, mNearbyPlaceResultReceiver);
        intent.putExtra(Config.KEY_PLACE_TYPE, preferences.getString(Config.PREF_PLACE_TYPE, "amusement_park"));
        intent.putExtra(Config.KEY_CURRENT_LOCATION, mCurrentLocation);

        startService(intent);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case NearbyPlaceService.STATUS_RUNNING:
                progress.show();
                break;
            case NearbyPlaceService.STATUS_FINISHED:
                /* Hide progress & extract result from bundle */
                if (progress.isShowing()) {
                    progress.dismiss();
                }
//                mPlaceListAdapter = new PlaceListAdapter(PlaceListActivity.this);
                mPlaceListAdapter.notifyDataSetChanged();
                break;
            case NearbyPlaceService.STATUS_ERROR:
                /* Handle the error */
                if (progress.isShowing()) {
                    progress.dismiss();
                }

                String error = resultData.getString(Intent.EXTRA_TEXT);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() &&
                ContextCompat.checkSelfPermission(PlaceListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ContextCompat.checkSelfPermission(PlaceListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {
                Log.e("onConnected", "mCurrentLocation: " + mCurrentLocation.getLatitude() + " , " + mCurrentLocation.getLongitude());

                //@todo: To get nearby places via intent service

            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (mCurrentLocation != null) {
            Log.e("onConnected", "mCurrentLocation changed: " + mCurrentLocation.getLatitude() + " , " + mCurrentLocation.getLongitude());
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (mGoogleApiClient.isConnected() && ContextCompat.checkSelfPermission(PlaceListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, PlaceListActivity.this);
        }
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                } else {
                    Toast.makeText(this, "Need Location permission to get Nearby Places!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    public class PlaceListAdapter extends RecyclerViewCursorAdapter<PlaceListAdapter.PlaceViewHolder> {

        public PlaceListAdapter(Context context) {
            super(context);
            setHasStableIds(true);
            setupCursorAdapter(null, 0, R.layout.place_list_content, false);
        }

        /**
         * Returns the ViewHolder to use for this adapter.
         */
        @Override
        public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PlaceViewHolder(mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent));
        }

        /**
         * Moves the Cursor of the CursorAdapter to the appropriate position and binds the view for
         * that item.
         */
        @Override
        public void onBindViewHolder(PlaceViewHolder holder, int position) {
            // Move cursor to this position
            mCursorAdapter.getCursor().moveToPosition(position);
            // Set the ViewHolder
            setViewHolder(holder);
            // Bind this view
            mCursorAdapter.bindView(null, mContext, mCursorAdapter.getCursor());
        }

        @Override
        public int getItemCount() {
            return mCursorAdapter.getCount();
        }

        /**
         * ViewHolder used to display a Place.
         */
        public class PlaceViewHolder extends RecyclerViewCursorViewHolder {
            public final TextView name;

            public PlaceViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(R.id.name);
            }

            @Override
            public void bindCursor(Cursor cursor) {
                name.setText(cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_NAME)));
            }
        }
    }
}
