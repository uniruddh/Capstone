package com.astuter.capstone.config;

import android.location.Location;
import android.net.Uri;

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
    public static final String KEY_PLACE_RESULT_RECEIVER = "KEY_PLACE_RESULT_RECEIVER";


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
                .appendPath("json")
                .appendQueryParameter("maxwidth", width)
                .appendQueryParameter("photoreference", photoReference)
                .appendQueryParameter("key", key)
                .build().toString();
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
        if (null != inputStream) {
            inputStream.close();
        }
        return result;
    }

}
