package com.astuter.capstone.config;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AlphaAnimation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by astuter on 16/07/16.
 */
public class Config {

    //    public static final String API_BASE = "https://maps.googleapis.com/maps/api/place/";
//    public static final String API_PLACE_LIST = API_BASE + "nearbysearch/json?location=51.503186,-0.126446&radius=5000&type=museum&key=";
//    public static final String API_PLACE_DETAIL = API_BASE + "details/json?placeid=ChIJN1t_tDeuEmsRUsoyG83frY4&key=";
//    public static final String API_PLACE_PHOTO = API_BASE + "photo?maxwidth=400&photoreference=CnRtAAAATLZNl354RwP_9UKbQ_5Psy40texXePv4oAlgP4qNEkdIrkyse7rPXYGd9D_Uj1rVsQdWT4oRz4QrYAJNpFX7rzqqMlZw2h2E2y5IKMUZ7ouD_SlcHxYq1yL4KbKUv3qtWgTK0A6QbGh87GB3sscrHRIQiG2RrmU_jF4tENr9wGS_YxoUSSDrYjWmrNfeEHSGSc3FyhNLlBU&key=";
    public static final String API_KEY = "AIzaSyAHWzZVpYn4shTmyKHnEuX6_78VLN-TbW8";

    public static final String KEY_CURRENT_LOCATION = "KEY_CURRENT_LOCATION";
    public static final String KEY_PLACE_TYPE = "KEY_PLACE_TYPE";
    public static final String KEY_PLACE_PHOTO = "KEY_PLACE_PHOTO";
    public static final String KEY_PLACE_ID = "KEY_PLACE_ID";
    public static final String KEY_PLACE_NAME = "KEY_PLACE_NAME";
    public static final String KEY_PLACE_VICINITY = "KEY_PLACE_VICINITY";
    public static final String KEY_RESULT_RECEIVER = "KEY_RESULT_RECEIVER";
    public static final String KEY_IS_TWO_PANE = "KEY_IS_TWO_PANE";
    public static final String KEY_PLACE_TITLE = "KEY_PLACE_TITLE";

    public static String getPlaceListUrl(Location location, String type, String key) {
        return new Uri.Builder()
                .scheme("https")
                .authority("maps.googleapis.com")
                .appendPath("maps")
                .appendPath("api")
                .appendPath("place")
                .appendPath("nearbysearch")
                .appendPath("json")
                .appendQueryParameter("radius", "5000")
                .appendQueryParameter("location", location.getLatitude() + "," + location.getLongitude())
                .appendQueryParameter("type", type)
                .appendQueryParameter("key", key)
                .build().toString();
    }

    public static String getPlaceDetailUrl(String placeId, String key) {
        return new Uri.Builder()
                .scheme("https")
                .authority("maps.googleapis.com")
                .appendPath("maps")
                .appendPath("api")
                .appendPath("place")
                .appendPath("details")
                .appendPath("json")
                .appendQueryParameter("placeid", placeId)
                .appendQueryParameter("key", key)
                .build().toString();
    }

    public static String getPlacePhotoUrl(String width, String photoReference, String key) {
        return new Uri.Builder()
                .scheme("https")
                .authority("maps.googleapis.com")
                .appendPath("maps")
                .appendPath("api")
                .appendPath("place")
                .appendPath("photo")
                .appendQueryParameter("maxwidth", width)
                .appendQueryParameter("photoreference", photoReference)
                .appendQueryParameter("key", key)
                .build().toString();
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    /*
     * Usage: Check if device have internet connection or not, before communicating with server.
     * @param ctx: Context form which this method will be called
     * @return Boolean: If internet is available or not.
    */
    public static boolean isNetConnected(Context ctx) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int dpToPx(Context ctx, int dp) {
        DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }

    /*
    * Covert convert InputStream to String
    * @param: inputStream
    * */
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";

        while ((line = bufferedReader.readLine()) != null) {
            result += line;
        }
            /* Close Stream */
        if (inputStream != null) {
            inputStream.close();
        }
        return result;
    }
}
