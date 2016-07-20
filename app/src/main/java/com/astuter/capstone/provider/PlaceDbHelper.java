package com.astuter.capstone.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.astuter.capstone.provider.PlaceContract.*;

/**
 * Created by astuter on 15/07/16.
 */
public class PlaceDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "place.db";

    public PlaceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold locations.  A location consists of the string supplied in the
        // location setting, the city name, and the latitude and longitude

        final String SQL_CREATE_PLACE_TABLE = "CREATE TABLE " + PlaceEntry.TABLE_NAME + " (" +
                PlaceEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PlaceEntry.COLUMN_PLACE_ID + " TEXT NOT NULL, " +
                PlaceEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                PlaceEntry.COLUMN_TYPE + " TEXT NOT NULL, " +
                PlaceEntry.COLUMN_PHOTO + " TEXT NOT NULL, " +
                PlaceEntry.COLUMN_LAT + " REAL NOT NULL, " +
                PlaceEntry.COLUMN_LNG + " REAL NOT NULL, " +
                PlaceEntry.COLUMN_VICINITY + " TEXT NOT NULL, " +
                PlaceEntry.COLUMN_RATING + " TEXT NOT NULL " +
                ");";

        final String SQL_CREATE_REVIEW_TABLE = "CREATE TABLE " + ReviewEntry.TABLE_NAME + " (" +
                ReviewEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                ReviewEntry.COLUMN_PLACE_ID + " TEXT NOT NULL, " +
                ReviewEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                ReviewEntry.COLUMN_PHOTO + " TEXT NOT NULL, " +
                ReviewEntry.COLUMN_TEXT + " TEXT NOT NULL, " +
                ReviewEntry.COLUMN_TIME + " REAL NOT NULL, " +
                ReviewEntry.COLUMN_RATING + " TEXT NOT NULL " +
                ");";

        sqLiteDatabase.execSQL(SQL_CREATE_PLACE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_REVIEW_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PlaceEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ReviewEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}