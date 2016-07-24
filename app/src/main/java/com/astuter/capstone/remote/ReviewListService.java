package com.astuter.capstone.remote;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
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
public class ReviewListService extends IntentService {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;

    private String placeId;

    private static final String TAG = "ReviewListService";

    public ReviewListService() {
        super(ReviewListService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "Service Started!");

        ResultReceiver receiver = intent.getParcelableExtra(Config.KEY_RESULT_RECEIVER);
        placeId = intent.getStringExtra(Config.KEY_PLACE_ID);

        if (!TextUtils.isEmpty(placeId)) {
            Bundle bundle = new Bundle();
            /* Update UI: Download Service is Running */
            receiver.send(STATUS_RUNNING, Bundle.EMPTY);
            try {
                String url = Config.getPlaceDetailUrl(placeId, Config.API_KEY);
                Log.e(TAG, "URL: " + url);

                int statusCode = getPlaceDetail(url);

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

    private int getPlaceDetail(String requestUrl) throws IOException, DownloadException {
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
            JSONArray reviews = response.optJSONObject("result").optJSONArray("reviews");

            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);

                ContentValues reviewValue = new ContentValues();

                reviewValue.put(PlaceContract.ReviewEntry.COLUMN_PLACE_ID, placeId);
                reviewValue.put(PlaceContract.ReviewEntry.COLUMN_NAME, review.optString("author_name"));
                reviewValue.put(PlaceContract.ReviewEntry.COLUMN_PHOTO, review.optString("profile_photo_url"));
                reviewValue.put(PlaceContract.ReviewEntry.COLUMN_TEXT, review.optString("text"));
                reviewValue.put(PlaceContract.ReviewEntry.COLUMN_RATING, review.optString("rating"));
                reviewValue.put(PlaceContract.ReviewEntry.COLUMN_TIME, review.optLong("time"));

                Uri uri = getContentResolver().insert(PlaceContract.ReviewEntry.CONTENT_URI, reviewValue);
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
