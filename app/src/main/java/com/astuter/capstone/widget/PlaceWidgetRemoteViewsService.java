package com.astuter.capstone.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.astuter.capstone.R;
import com.astuter.capstone.config.Config;
import com.astuter.capstone.config.PrefsManager;
import com.astuter.capstone.provider.PlaceContract;
import com.bumptech.glide.Glide;

/**
 * Created by Astuter on 25/07/16.
 */
public class PlaceWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new PlaceWidgetRemoteViewsServiceFactory(this.getApplicationContext(), intent);
    }
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class PlaceWidgetRemoteViewsServiceFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;

    public PlaceWidgetRemoteViewsServiceFactory(Context context, Intent intent) {
        mContext = context;
    }

    public void onCreate() {
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
    }

    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider

        final RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        if (mCursor.moveToPosition(position)) {
            final String name = mCursor.getString(mCursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_NAME));
            final String photo = mCursor.getString(mCursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PHOTO));

            rv.setTextViewText(R.id.name, name);

            new AsyncTask<Void, Void, Void>() {
                Bitmap bitmap;

                @Override
                protected Void doInBackground(Void... params) {
                    Looper.prepare();
                    try {
                        bitmap = Glide.with(mContext)
                                .load(Config.getPlacePhotoUrl("80", photo, Config.API_KEY))
                                .asBitmap()
                                .centerCrop()
                                .into(100, 100)
                                .get();
                    } catch (Exception e) {
                        Log.e("PlaceWidgetRemoteView", e.getMessage());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void dummy) {
                    if (bitmap != null) {
                        // The full bitmap should be available here
                        rv.setImageViewBitmap(R.id.icon, bitmap);
                        Log.d("PlaceWidgetRemoteView", "Image loaded");
                    }
                }
            }.execute();

        }

        return rv;
    }

    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    public int getViewTypeCount() {
        // Technically, we have two types of views (the dark and light background views)
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // Refresh the cursor
        if (mCursor != null) {
            mCursor.close();
        }

        final long token = Binder.clearCallingIdentity();
        try {
            mCursor = mContext.getContentResolver().query(PlaceContract.PlaceEntry.CONTENT_URI,
                    null,
                    PlaceContract.PlaceEntry.COLUMN_TYPE + " = ? ",
                    new String[]{PrefsManager.instance(mContext.getApplicationContext()).getCurrentPlaceType()},
                    null);
        } finally {
            Binder.restoreCallingIdentity(token);
        }

    }
}