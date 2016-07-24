package com.astuter.capstone.gui;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.astuter.capstone.R;
import com.astuter.capstone.config.Config;
import com.astuter.capstone.config.PrefsManager;
import com.astuter.capstone.provider.PlaceContract;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, LoaderManager.LoaderCallbacks<Cursor> {

    private GoogleMap mMap;
    private Location mLocation;
//    private final String[] PROJECTION = new String[] {PlaceContract.PlaceEntry., "text_column" };

    private final int PLACE_MAP_LOADER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            toolbar.setTitle(extras.getString(Config.KEY_PLACE_TITLE));
            // id: 750 248 329
            // pass: s1dw52
            mLocation = extras.getParcelable(Config.KEY_CURRENT_LOCATION);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // User loader to fetch data from SQLite
        getSupportLoaderManager().initLoader(PLACE_MAP_LOADER, null, MapActivity.this);

        Drawable circle = ContextCompat.getDrawable(MapActivity.this, R.drawable.ic_been_here);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(circle.getIntrinsicWidth(), circle.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        circle.setBounds(0, 0, circle.getIntrinsicWidth(), circle.getIntrinsicHeight());
        circle.draw(canvas);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(bitmap);


        // Add a marker in Sydney and move the camera
        LatLng currentLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(currentLocation).title("Here you are!").icon(icon));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13.0f)); // zoom Range is 2.0 to 21.0

    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Uri CONTENT_URI = PlaceContract.PlaceEntry.CONTENT_URI;
        return new CursorLoader(MapActivity.this,
                CONTENT_URI,
                null,
                PlaceContract.PlaceEntry.COLUMN_TYPE + " = ? ",
                new String[]{PrefsManager.instance(getApplicationContext()).getCurrentPlaceType()},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst() && !cursor.isClosed()) {

            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_NAME));
                String vicinity = cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_VICINITY));
                vicinity = vicinity.replaceAll(", ", ",\n");
                double lat = cursor.getDouble(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_LNG));
                Log.e("onLoadFinished", "NAME: " + name + " LAT: " + lat + " LNG: " + lng);
                // Add a marker for Places
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(name)
                        .snippet(vicinity));
                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    @Override
                    public View getInfoWindow(Marker arg0) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        LinearLayout info = new LinearLayout(MapActivity.this);
                        info.setOrientation(LinearLayout.VERTICAL);

                        TextView title = new TextView(MapActivity.this);
                        title.setTextColor(Color.BLACK);
                        title.setGravity(Gravity.LEFT);
                        title.setTypeface(null, Typeface.BOLD);
                        title.setText(marker.getTitle());

                        TextView snippet = new TextView(MapActivity.this);
                        snippet.setTextColor(Color.GRAY);
                        snippet.setText(marker.getSnippet());

                        info.addView(title);
                        info.addView(snippet);

                        return info;
                    }
                });
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the Cursor is being placed in a CursorAdapter, you should use the
        // swapCursor(null) method to remove any references it has to the
        // Loader's data.
    }
}
