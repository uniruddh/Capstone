package com.astuter.capstone.gui;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.astuter.capstone.R;
import com.astuter.capstone.config.Config;
import com.astuter.capstone.config.ItemClickSupport;
import com.astuter.capstone.config.PlaceListAdapter;
import com.astuter.capstone.config.PrefsManager;
import com.astuter.capstone.provider.PlaceContract;
import com.astuter.capstone.remote.ServiceResultReceiver;
import com.astuter.capstone.remote.PlaceListService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;
import java.util.HashMap;

/**
 * An activity representing a list of Places. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link PlaceDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class PlaceListActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ServiceResultReceiver.Receiver,
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private ServiceResultReceiver mServiceResultReceiver;
    private RecyclerView mRecyclerView;
    private PlaceListAdapter mPlaceListAdapter;
    private ProgressDialog progress;

    private Spinner mSpinner;
    private HashMap<String, Location> placeLocationMap;

    private String[] PLACE_TYPE;
    private final int PERMISSION_ACCESS_FINE_LOCATION = 1;
    private final int PLACE_LOADER = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_list);

        PLACE_TYPE = getResources().getStringArray(R.array.nearby_places_key);

        if (findViewById(R.id.place_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Please Wait ...");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add Spinner programmatically to toolbar
        setupSpinner(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                // Pass the Place data and current location to show on map
                Bundle bundle = new Bundle();
                bundle.putParcelable(Config.KEY_CURRENT_LOCATION, PrefsManager.instance(getApplicationContext()).getCurrentLocation());
                bundle.putString(Config.KEY_PLACE_TITLE, mSpinner.getSelectedItem().toString());
                Intent intent = new Intent(PlaceListActivity.this, MapActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_FINE_LOCATION);
        }

        // Setup GoogleApiClient for getting user Location
        setUpGoogleApiClient();

        // create Location Request to get periodic Location updates
        createLocationRequest();

        // setup RecyclerView
        setupRecyclerView();

        // User loader to fetch data from SQLite
        getSupportLoaderManager().initLoader(PLACE_LOADER, null, this);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Config.isLocationEnabled(PlaceListActivity.this)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(PlaceListActivity.this);
            dialog.setMessage(getResources().getString(R.string.no_location_msg));
            dialog.setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // Open Location settings on device
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            dialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();
        }

        if (mGoogleApiClient.isConnected() &&
                ContextCompat.checkSelfPermission(PlaceListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, PlaceListActivity.this);
        }
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                } else {
                    Toast.makeText(this, getResources().getString(R.string.required_location_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void setupSpinner(Toolbar toolbar) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(),
                    R.array.nearby_places,
                    R.layout.spinner_item_layout);
            mSpinner = new Spinner(getSupportActionBar().getThemedContext());
            mSpinner.setAdapter(spinnerAdapter);
            mSpinner.setSelection(Arrays.asList(PLACE_TYPE).indexOf(PrefsManager.instance(getApplicationContext()).getCurrentPlaceType()), false);
            toolbar.addView(mSpinner, 0);

            // Hack to keep onItemSelected from firing off on a newly instantiated Spinner : @check here: http://stackoverflow.com/q/2562248/2571277
            mSpinner.post(new Runnable() {
                @Override
                public void run() {
                    mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            PrefsManager.instance(getApplicationContext()).setCurrentPlaceType(PLACE_TYPE[position]);

                            Log.e("navigationSpinner", "you selected:" + PLACE_TYPE[position]);
                            fetchNearbyPlaces();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                }
            });
        }
    }

    private void setupRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.place_list);
        mRecyclerView.setHasFixedSize(true);
        mPlaceListAdapter = new PlaceListAdapter(PlaceListActivity.this);
        mRecyclerView.setAdapter(mPlaceListAdapter);

        ItemClickSupport.addTo(mRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                PlaceListAdapter.PlaceViewHolder holder = (PlaceListAdapter.PlaceViewHolder) recyclerView.getChildViewHolder(v);
                Bundle arguments = new Bundle();
                arguments.putBoolean(Config.KEY_IS_TWO_PANE, mTwoPane);
                arguments.putString(Config.KEY_PLACE_NAME, holder.name.getText().toString());
                arguments.putString(Config.KEY_PLACE_ID, holder.id.getText().toString());
                arguments.putString(Config.KEY_PLACE_PHOTO, holder.photo.getText().toString());
                arguments.putString(Config.KEY_PLACE_VICINITY, holder.vicinity.getText().toString());

                if (mTwoPane) {
                    PlaceDetailFragment fragment = new PlaceDetailFragment();
                    fragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction().replace(R.id.place_detail_container, fragment).commit();
                } else {
                    Intent intent = new Intent(PlaceListActivity.this, PlaceDetailActivity.class);
                    intent.putExtras(arguments);
                    // Check if we're running on Android 5.0 or higher
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(PlaceListActivity.this, holder.icon, "photo");
                        startActivity(intent, options.toBundle());
                    } else {
                        startActivity(intent);
                    }
                }
            }
        });
    }

    /************************************************************/
    /*      Methods for CursorLoader with callback              */
    /************************************************************/

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Uri CONTENT_URI = PlaceContract.PlaceEntry.CONTENT_URI;
        return new CursorLoader(PlaceListActivity.this,
                CONTENT_URI,
                null,
                PlaceContract.PlaceEntry.COLUMN_TYPE + " = ? ",
                new String[]{PrefsManager.instance(getApplicationContext()).getCurrentPlaceType()},
                null);
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
        mPlaceListAdapter.swapCursor(null);
    }

    /************************************************************/
    /*     Methods for GoogleApiClient with callback            */

    /************************************************************/

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

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mGoogleApiClient.isConnected() && ContextCompat.checkSelfPermission(PlaceListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, PlaceListActivity.this);

            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                Log.e("Location", "Get : " + location.getLatitude() + " , " + location.getLongitude());
                // Keep update to date location in preferences
                PrefsManager.instance(getApplicationContext()).setCurrentLocation(location);
            } else {
                //@todo: Error while getting location, don't move ahead from here
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    /************************************************************/
    /*     Methods for LocationRequest with callback            */

    /************************************************************/

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.e("Location", "Changed: " + location.getLatitude() + " , " + location.getLongitude());
            // Keep update to date location in preferences
            PrefsManager.instance(getApplicationContext()).setCurrentLocation(location);
            fetchNearbyPlaces();
        }
    }

    /************************************************************/
    /*   Methods for Fetching Nearby Places with callback       */

    /************************************************************/

    private void startNearbyPlaceService() {
        if (Config.isNetConnected(PlaceListActivity.this)) {
            /* Starting Download Service */
            mServiceResultReceiver = new ServiceResultReceiver(new Handler());
            mServiceResultReceiver.setReceiver(PlaceListActivity.this);
            Intent intent = new Intent(Intent.ACTION_SYNC, null, this, PlaceListService.class);

            /* Send optional extras to Download IntentService */
            intent.putExtra(Config.KEY_RESULT_RECEIVER, mServiceResultReceiver);
            intent.putExtra(Config.KEY_PLACE_TYPE, PrefsManager.instance(getApplicationContext()).getCurrentPlaceType());
            intent.putExtra(Config.KEY_CURRENT_LOCATION, PrefsManager.instance(getApplicationContext()).getCurrentLocation());

            startService(intent);
        } else {
            Toast.makeText(this, getResources().getString(R.string.no_internet_msg), Toast.LENGTH_SHORT).show();
        }
    }

    private int fetchNearbyPlaces() {
        // Get PlaceLocationMap for fetching nearBy Places
        HashMap<String, Location> defaultMap = PrefsManager.instance(getApplicationContext()).getMapPlaceLocation();
        if (defaultMap != null) {
            placeLocationMap = defaultMap;
        } else {
            placeLocationMap = new HashMap<>();
        }

        String placeType = PrefsManager.instance(getApplicationContext()).getCurrentPlaceType();

        if (placeLocationMap.size() == 0 || !placeLocationMap.keySet().contains(placeType)) {
            // this should be first time user has launched the app, fetch places using default configurations
            startNearbyPlaceService();

            placeLocationMap.put(placeType, PrefsManager.instance(getApplicationContext()).getCurrentLocation());
            PrefsManager.instance(getApplicationContext()).setMapPlaceLocation(placeLocationMap);

            return 1;
        } else if (placeLocationMap.size() > 0 && placeLocationMap.keySet().contains(placeType)) {

            Location locations = placeLocationMap.get(placeType);

            if (PrefsManager.instance(getApplicationContext()).getCurrentLocation().distanceTo(locations) > 2500) {
                // First delete all places of this type
                getContentResolver().delete(PlaceContract.PlaceEntry.CONTENT_URI,
                        PlaceContract.PlaceEntry.COLUMN_TYPE + " = ? ",
                        new String[]{PrefsManager.instance(getApplicationContext()).getCurrentPlaceType()});

                // Then fetch new one for new location
                startNearbyPlaceService();

                placeLocationMap.put(placeType, PrefsManager.instance(getApplicationContext()).getCurrentLocation());
                PrefsManager.instance(getApplicationContext()).setMapPlaceLocation(placeLocationMap);
            } else {
                // We still have Nearby Places in our DB, fetch them
                getSupportLoaderManager().restartLoader(PLACE_LOADER, null, PlaceListActivity.this);
            }
            return 1;
        }
        return 0;
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case PlaceListService.STATUS_RUNNING:
                progress.show();
                break;
            case PlaceListService.STATUS_FINISHED:
                /* Hide progress & extract result from bundle */
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }

                // Update cursor with newly fetched Places
                getSupportLoaderManager().restartLoader(PLACE_LOADER, null, PlaceListActivity.this);
                break;
            case PlaceListService.STATUS_ERROR:
                /* Handle the error */
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
                Log.e("STATUS_ERROR", resultData.getString(Intent.EXTRA_TEXT));

                Toast.makeText(PlaceListActivity.this, getResources().getString(R.string.error_fetch_nearby_place), Toast.LENGTH_LONG).show();
                break;
        }
    }
}
