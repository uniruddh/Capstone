package com.astuter.capstone.provider;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by astuter on 15/07/16.
 */
public class PlaceProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private PlaceDbHelper mOpenHelper;

    static final int PLACE = 100;
    static final int PLACE_BY_ID = 101;
    static final int REVIEW = 200;
    static final int REVIEW_BY_ID = 201;


    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = PlaceContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, PlaceContract.PATH_PLACE, PLACE);
        matcher.addURI(authority, PlaceContract.PATH_PLACE + "/#", PLACE_BY_ID);

        matcher.addURI(authority, PlaceContract.PATH_REVIEW, REVIEW);
        matcher.addURI(authority, PlaceContract.PATH_REVIEW + "/#", REVIEW_BY_ID);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new PlaceDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        switch (sUriMatcher.match(uri)) {
            // Student: Uncomment and fill out these two cases
            case PLACE:
                return PlaceContract.PlaceEntry.CONTENT_TYPE;
            case PLACE_BY_ID:
                return PlaceContract.PlaceEntry.CONTENT_ITEM_TYPE;
            case REVIEW:
                return PlaceContract.ReviewEntry.CONTENT_TYPE;
            case REVIEW_BY_ID:
                return PlaceContract.ReviewEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor retCursor;

        switch (sUriMatcher.match(uri)) {
            case PLACE:
                retCursor = db.query(
                        PlaceContract.PlaceEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case PLACE_BY_ID:
                long _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        PlaceContract.PlaceEntry.TABLE_NAME,
                        projection,
                        PlaceContract.PlaceEntry.COLUMN_PLACE_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;
            case REVIEW:
                retCursor = db.query(
                        PlaceContract.ReviewEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case REVIEW_BY_ID:
                _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        PlaceContract.ReviewEntry.TABLE_NAME,
                        projection,
                        PlaceContract.ReviewEntry.COLUMN_PLACE_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Set the notification URI for the cursor to the one passed into the function. This
        // causes the cursor to register a content observer to watch for changes that happen to
        // this URI and any of it's descendants. By descendants, we mean any URI that begins
        // with this path.
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long _id;
        Uri returnUri;
        switch(sUriMatcher.match(uri)){
            case PLACE:
                _id = db.insert(PlaceContract.PlaceEntry.TABLE_NAME, null, values);
                if(_id > 0){
                    returnUri =  PlaceContract.PlaceEntry.buildPlaceUri(_id);
                } else{
                    throw new UnsupportedOperationException("Unable to insert rows into: " + uri);
                }
                break;
            case REVIEW:
                _id = db.insert(PlaceContract.ReviewEntry.TABLE_NAME, null, values);
                if(_id > 0){
                    returnUri = PlaceContract.ReviewEntry.buildReviewUri(_id);
                } else{
                    throw new UnsupportedOperationException("Unable to insert rows into: " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Use this on the URI passed into the function to notify any observers that the uri has changed.
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsDeleted; // Number of rows effected
        switch(sUriMatcher.match(uri)){
            case PLACE:
                rowsDeleted = db.delete(PlaceContract.PlaceEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case REVIEW:
                rowsDeleted = db.delete(PlaceContract.ReviewEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because null could delete all rows:
        if(selection == null || rowsDeleted != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsUpdated;
        switch(sUriMatcher.match(uri)){
            case PLACE:
                rowsUpdated = db.update(PlaceContract.PlaceEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case REVIEW:
                rowsUpdated = db.update(PlaceContract.ReviewEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PLACE:
                db.beginTransaction();
                int placeCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(PlaceContract.PlaceEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            placeCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return placeCount;
            case REVIEW:
                db.beginTransaction();
                int reviewCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(PlaceContract.PlaceEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            reviewCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return reviewCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}