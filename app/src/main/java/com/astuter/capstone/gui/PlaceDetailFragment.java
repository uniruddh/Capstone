package com.astuter.capstone.gui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.astuter.capstone.R;
import com.astuter.capstone.config.Config;
import com.astuter.capstone.config.ReviewListAdapter;
import com.astuter.capstone.provider.PlaceContract;
import com.astuter.capstone.remote.PlaceListService;
import com.astuter.capstone.remote.ReviewListService;
import com.astuter.capstone.remote.ServiceResultReceiver;

/**
 * A fragment representing a single Place detail screen.
 * This fragment is either contained in a {@link PlaceListActivity}
 * in two-pane mode (on tablets) or a {@link PlaceDetailActivity}
 * on handsets.
 */
public class PlaceDetailFragment extends Fragment implements ServiceResultReceiver.Receiver, LoaderManager.LoaderCallbacks<Cursor> {

    private ProgressDialog progress;
    private TextView placeVicinity;
    private String vicinity, placeId;
    private Context mContext;
    private RecyclerView mRecyclerView;
    private ReviewListAdapter mReviewListAdapter;

    private final int REVIEW_LOADER = 1;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlaceDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        progress = new ProgressDialog(mContext);
        progress.setTitle("Loading");
        progress.setMessage("Please Wait ...");

        Bundle bundle = getArguments();
        if (bundle.containsKey(Config.KEY_PLACE_ID)) {
            placeId = bundle.getString(Config.KEY_PLACE_ID);
            vicinity = bundle.getString(Config.KEY_PLACE_VICINITY);

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(bundle.getString(Config.KEY_PLACE_NAME));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.place_detail, container, false);
        placeVicinity = (TextView) rootView.findViewById(R.id.place_vicinity);

        if (vicinity != null) {
            placeVicinity.setText(vicinity);
        }

        Cursor reviewCrsr = getActivity().getContentResolver().query(PlaceContract.ReviewEntry.CONTENT_URI,
                new String[]{PlaceContract.ReviewEntry.COLUMN_PLACE_ID},
                PlaceContract.ReviewEntry.COLUMN_PLACE_ID + " = ? ",
                new String[]{placeId},
                null);

        if (reviewCrsr == null || reviewCrsr.getCount() < 1) {
            startNearbyPlaceService();
        }

        // setup RecyclerView
        setupRecyclerView(rootView);

        // User loader to fetch data from SQLite
        getActivity().getSupportLoaderManager().initLoader(REVIEW_LOADER, null, this);

        return rootView;
    }

    private void setupRecyclerView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.review_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setAutoMeasureEnabled(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mReviewListAdapter = new ReviewListAdapter(mContext);
        mRecyclerView.setAdapter(mReviewListAdapter);
    }

    /************************************************************/
    /*      Methods for CursorLoader with callback              */

    /************************************************************/

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Uri CONTENT_URI = PlaceContract.ReviewEntry.CONTENT_URI;
        return new CursorLoader(mContext,
                CONTENT_URI,
                null,
                PlaceContract.ReviewEntry.COLUMN_PLACE_ID + " = ? ",
                new String[]{placeId},
                PlaceContract.ReviewEntry.COLUMN_TIME + " DESC ");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        if (!cursor.isClosed()) {
            cursor.moveToFirst();
            mReviewListAdapter.swapCursor(cursor);
            mReviewListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the Cursor is being placed in a CursorAdapter, you should use the
        // swapCursor(null) method to remove any references it has to the
        // Loader's data.
        mReviewListAdapter.swapCursor(null);
    }

    private void startNearbyPlaceService() {
        if (Config.isNetConnected(mContext)) {
            ServiceResultReceiver mServiceResultReceiver = new ServiceResultReceiver(new Handler());
            mServiceResultReceiver.setReceiver(this);
            Intent intent = new Intent(Intent.ACTION_SYNC, null, mContext, ReviewListService.class);

            /* Send optional extras to Download IntentService */
            intent.putExtra(Config.KEY_RESULT_RECEIVER, mServiceResultReceiver);
            intent.putExtra(Config.KEY_PLACE_ID, placeId);

            getActivity().startService(intent);
        } else {
            Toast.makeText(mContext, getResources().getString(R.string.no_internet_msg), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case PlaceListService.STATUS_RUNNING:
                progress.show();
                break;
            case PlaceListService.STATUS_FINISHED:
                /* Hide progress & extract result from bundle */
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
                // TODO : update reviews here
                // Update cursor with newly fetched Places
                getActivity().getSupportLoaderManager().restartLoader(REVIEW_LOADER, null, this);

                break;
            case PlaceListService.STATUS_ERROR:
                /* Handle the error */
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                }
                Log.e("STATUS_ERROR", resultData.getString(Intent.EXTRA_TEXT));
//              Toast.makeText(mContext, getResources().getString(R.string.error_fetch_nearby_place), Toast.LENGTH_LONG).show();
                break;
        }
    }
}
