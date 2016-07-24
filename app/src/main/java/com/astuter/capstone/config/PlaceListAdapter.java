package com.astuter.capstone.config;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorViewHolder;
import com.astuter.capstone.R;
import com.astuter.capstone.provider.PlaceContract;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

/**
 * Created by Astuter on 21/07/16.
 */
public class PlaceListAdapter extends RecyclerViewCursorAdapter<PlaceListAdapter.PlaceViewHolder> {

    Context mContext;

    public PlaceListAdapter(Context context) {
        super(context);
        mContext = context;
        setHasStableIds(true);
        setupCursorAdapter(null, 0, R.layout.place_list_item, false);
    }

    /**
     * Returns the ViewHolder to use for this adapter.
     */
    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlaceViewHolder(mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent));
    }

    /**
     * Moves the Cursor of the CursorAdapter to the appropriate position and binds the view for that item.
     */
    @Override
    public void onBindViewHolder(PlaceViewHolder holder, int position) {
        // Move cursor to this position
        mCursorAdapter.getCursor().moveToPosition(position);
        // Set the ViewHolder
        setViewHolder(holder);
        // Bind this view
        mCursorAdapter.bindView(null, mContext, mCursorAdapter.getCursor());
    }

    @Override
    public int getItemCount() {
        return mCursorAdapter.getCount();
    }

    /**
     * ViewHolder used to display a Place.
     */
    public class PlaceViewHolder extends RecyclerViewCursorViewHolder {
        public final TextView name;
        public final ImageView icon;
        public final TextView id;
        public final TextView photo;
        public final TextView vicinity;

        public PlaceViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            icon = (ImageView) view.findViewById(R.id.icon);
            id = (TextView) view.findViewById(R.id.place_id);
            photo = (TextView) view.findViewById(R.id.place_photo);
            vicinity = (TextView) view.findViewById(R.id.place_vicinity);
        }

        @Override
        public void bindCursor(Cursor cursor) {
            name.setText(cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_NAME)));
            id.setText(cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
            photo.setText(cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PHOTO)));
            vicinity.setText(cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_VICINITY)));

            Glide.with(icon.getContext())
                    .load(Config.getPlacePhotoUrl("80", cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PHOTO)), Config.API_KEY))
                    .asBitmap().centerCrop().into(new BitmapImageViewTarget(icon) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(mContext.getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    icon.setImageDrawable(circularBitmapDrawable);
                }
            });
//                Log.e("Photo" , Config.getPlacePhotoUrl("80", cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PHOTO)), Config.API_KEY));
        }
    }
}