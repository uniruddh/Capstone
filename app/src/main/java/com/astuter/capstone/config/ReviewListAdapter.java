package com.astuter.capstone.config;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorViewHolder;
import com.astuter.capstone.R;
import com.astuter.capstone.provider.PlaceContract;
import com.bumptech.glide.Glide;

/**
 * Created by Astuter on 21/07/16.
 */
public class ReviewListAdapter extends RecyclerViewCursorAdapter<ReviewListAdapter.ReviewViewHolder> {

    Context mContext;

    public ReviewListAdapter(Context context) {
        super(context);
        mContext = context;
        setHasStableIds(true);
        setupCursorAdapter(null, 0, R.layout.review_list_card, false);
    }

    /**
     * Returns the ViewHolder to use for this adapter.
     */
    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ReviewViewHolder(mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent));
    }

    /**
     * Moves the Cursor of the CursorAdapter to the appropriate position and binds the view for that item.
     */
    @Override
    public void onBindViewHolder(ReviewViewHolder holder, int position) {
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
     * ViewHolder used to display a Review.
     */
    public class ReviewViewHolder extends RecyclerViewCursorViewHolder {
        public final ImageView icon;
        public final TextView author;
        public final TextView time;
        public final TextView review;
        public final RatingBar rating;

        public ReviewViewHolder(View view) {
            super(view);
            icon = (ImageView) view.findViewById(R.id.icon);
            author = (TextView) view.findViewById(R.id.author);
            time = (TextView) view.findViewById(R.id.time);
            review = (TextView) view.findViewById(R.id.review);
            rating = (RatingBar) view.findViewById(R.id.ratings);
        }

        @Override
        public void bindCursor(Cursor cursor) {

            Glide.with(icon.getContext())
                    .load("https:" + cursor.getString(cursor.getColumnIndex(PlaceContract.ReviewEntry.COLUMN_PHOTO)))
                    .placeholder(R.drawable.ic_author)
                    .into(icon);

            review.setText(cursor.getString(cursor.getColumnIndex(PlaceContract.ReviewEntry.COLUMN_TEXT)));
            author.setText(cursor.getString(cursor.getColumnIndex(PlaceContract.ReviewEntry.COLUMN_NAME)));
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    cursor.getLong(cursor.getColumnIndex(PlaceContract.ReviewEntry.COLUMN_TIME)) * 1000L);
            time.setText(relativeTime.toString());

            String rate = cursor.getString(cursor.getColumnIndex(PlaceContract.ReviewEntry.COLUMN_RATING));
            rating.setRating(Float.parseFloat(rate));


//            Glide.with(icon.getContext())
//                    .load(Config.getPlacePhotoUrl("80", cursor.getString(cursor.getColumnIndex(PlaceContract.ReviewEntry.COLUMN_PHOTO)), Config.API_KEY))
//                    .asBitmap().centerCrop().into(new BitmapImageViewTarget(icon) {
//                @Override
//                protected void setResource(Bitmap resource) {
//                    RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(mContext.getResources(), resource);
//                    circularBitmapDrawable.setCircular(true);
//                    icon.setImageDrawable(circularBitmapDrawable);
//                }
//            });
//                Log.e("Photo" , Config.getPlacePhotoUrl("80", cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PHOTO)), Config.API_KEY));
        }
    }
}