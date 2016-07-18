package com.astuter.capstone.remote;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.astuter.capstone.config.Config;
import com.astuter.capstone.provider.PlaceContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by astuter on 16/07/16.
 */
public class NearbyPlaceService extends IntentService {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;

    private static final String TAG = "NearbyPlaceService";

    public NearbyPlaceService() {
        super(NearbyPlaceService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "Service Started!");

        ResultReceiver receiver = intent.getParcelableExtra(Config.KEY_PLACE_RESULT_RECEIVER);
        Location location = intent.getParcelableExtra(Config.KEY_CURRENT_LOCATION);
        String placeType = intent.getStringExtra(Config.KEY_PLACE_TYPE);

        if (!TextUtils.isEmpty(placeType)) {
            Bundle bundle = new Bundle();
            /* Update UI: Download Service is Running */
            receiver.send(STATUS_RUNNING, Bundle.EMPTY);
            try {
                String url = Config.getPlaceListUrl(location, placeType, Config.API_KEY);
                Log.e(TAG, "URL: " + url);

                int statusCode = getNearbyPlaces(url);

                /* Sending result back to activity */
                if (statusCode == 200) {
                    receiver.send(STATUS_FINISHED, bundle);
                }
            } catch (Exception e) {
                /* Sending error message back to activity */
                bundle.putString(Intent.EXTRA_TEXT, e.toString());
                receiver.send(STATUS_ERROR, bundle);
            }
        }
        Log.e(TAG, "Service Stopping!");
        this.stopSelf();
    }

    private int getNearbyPlaces(String requestUrl) throws IOException, DownloadException {
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;

        /* forming th java.net.URL object */
        URL url = new URL(requestUrl);
        urlConnection = (HttpURLConnection) url.openConnection();

        /* optional request header */
        urlConnection.setRequestProperty("Content-Type", "application/json");

        /* optional request header */
        urlConnection.setRequestProperty("Accept", "application/json");

        /* for Get request */
        urlConnection.setRequestMethod("GET");
        int statusCode = urlConnection.getResponseCode();

        /* 200 represents HTTP OK */
        if (statusCode == 200) {
            inputStream = new BufferedInputStream(urlConnection.getInputStream());
            String response = Config.convertInputStreamToString(inputStream);
            parseResult(response);
        } else {
            throw new DownloadException("Failed to fetch data!!");
        }
        return statusCode;
    }

    private void parseResult(String result) {
        try {
            JSONObject response = new JSONObject(result);
            JSONArray results = response.optJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject post = results.getJSONObject(i);
                String placeId = post.getString("place_id");
                String name = post.getString("name");
                String vicinity = post.getString("vicinity");

                ContentValues contentValues = new ContentValues();
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_NAME, name);
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeId);
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_PHOTO, name);
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_LAT, 0);
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_LNG, 0);
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_VICINITY, vicinity);
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_RATING, name);

                Uri uri = getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);
                Log.e("getContentResolver", "insert" + uri.toString());

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class DownloadException extends Exception {
        public DownloadException(String message) {
            super(message);
        }

        public DownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
