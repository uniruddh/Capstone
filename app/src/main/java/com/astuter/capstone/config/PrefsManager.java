package com.astuter.capstone.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

/**
 * Created by astuter on 19/07/16.
 */
public class PrefsManager {

    public final String PREF_CURRENT_PLACE_TYPE = "PREF_CURRENT_PLACE_TYPE";
    public final String PREF_CURRENT_LOCATION = "PREF_CURRENT_LOCATION";
    public final String PREF_MAP_PLACE_LOCATION = "PREF_MAP_PLACE_LOCATION";

    private static SharedPreferences preferences;

    private static class Holder {
        static final PrefsManager INSTANCE = new PrefsManager();
    }

    public static PrefsManager instance(Context ctx) {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        }
        return Holder.INSTANCE;
    }

    public void setCurrentPlaceType(String type) {
        preferences.edit().putString(PREF_CURRENT_PLACE_TYPE, type).apply();
    }

    public String getCurrentPlaceType() {
        return preferences.getString(PREF_CURRENT_PLACE_TYPE, "amusement_park");
    }

    public void setCurrentLocation(Location location) {
        String locationJson = new Gson().toJson(location);
        preferences.edit().putString(PREF_CURRENT_LOCATION, locationJson).apply();
    }

    public Location getCurrentLocation() {
        String json = preferences.getString(PREF_CURRENT_LOCATION, "");
        return new Gson().fromJson(json, Location.class);
    }

    public void setMapPlaceLocation(HashMap<String, Location> hashMap) {
        String locationJson = new Gson().toJson(hashMap);
        preferences.edit().putString(PREF_MAP_PLACE_LOCATION, locationJson).apply();
    }

    public HashMap<String, Location> getMapPlaceLocation() {
        String mapJson = preferences.getString(PREF_MAP_PLACE_LOCATION, "");
        java.lang.reflect.Type type = new TypeToken<HashMap<String, Location>>() {
        }.getType();
        return new Gson().fromJson(mapJson, type);
    }
}
