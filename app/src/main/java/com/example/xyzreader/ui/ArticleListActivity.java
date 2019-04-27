package com.example.xyzreader.ui;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // member variables
    private static final String TAG = ArticleListActivity.class.toString();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    // use default locale date formats
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    private DateFormat outputFormat = DateFormat.getDateInstance();

    // most time functions can only handle years 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    // track the status of the UI "refreshing"
    private boolean mIsRefreshing;

    // a broadcast receiver updates the UI if necessary
    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            // double check the broadcast has the "state change" key
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {

                // update flag and refresh UI
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate layout
        setContentView(R.layout.activity_article_list_material_design);

        // get view references
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = findViewById(R.id.recycler_view);

        // initialize an ArticleLoader
        getSupportLoaderManager().initLoader(0, null, this);

        // if needed, update the UI via the service
        if (savedInstanceState == null) {
            Intent intentUpdaterService = new Intent(this, UpdaterService.class);
            startService(intentUpdaterService);
        }
    }

    // associate the "state change" key to the receiver and register it
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilterStateChange = new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);

        // results in system broadcast and system call to UpdaterService onHandleIntent()
        registerReceiver(mRefreshingReceiver, intentFilterStateChange);
    }

    // unregister the receiver
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    // returns an ArticleLoader that pulls from the local persistent database
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    // populate UI using local DB contents
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        // create a default framework adapter
        Adapter adapter = new Adapter(cursor);

        // stable IDs allow for memory usage optimization
        adapter.setHasStableIds(true);

        // associate adapter to recycler view
        mRecyclerView.setAdapter(adapter);

        // set number of columns in a staggered grid for the recycler view
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);

        // GridLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), columnCount);

        mRecyclerView.setLayoutManager(layoutManager);
    }

    // refresh the UI
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // results in refreshing the UI
        mRecyclerView.setAdapter(null);
    }

    // simple adapter using a local DB cursor and viewholder
    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        // member variable
        private Cursor mCursor;

        // instantiate adapter with a database cursor
        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        // move cursor the desired row and extract the article ID
        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            // inflate an article item layout
            View view = getLayoutInflater().inflate(R.layout.list_item_article_material_design, parent, false);

            // create a new viewholder
            final ViewHolder viewHolder = new ViewHolder(view);

            // set a click listener on the item layout that TODO ...
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // URI of specific article
                    Uri itemUri = ItemsContract.Items.buildItemUri(getItemId(viewHolder.getAdapterPosition()));

                    // TODO this intent somehow calls ArticleDetailActivity
                    Intent intent = new Intent(Intent.ACTION_VIEW, itemUri);
                    startActivity(intent);
                }
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            // move to correct row
            mCursor.moveToPosition(position);

            // set the title
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            // reformat the date using helper function
            Date publishedDate = parsePublishedDate();

            // display the date and author using <html> tag format
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            // set the article image using the 'volley' library
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());

            // this call overrides the aspect ratio set in DynamicHeightNetworkImageView
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        // reformat the date string
        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "Error passing today's date.");
                return new Date();
            }
        }

        // number of articles in the cursor
        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }


    // associate the views in the item layout to member variables
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }
}
