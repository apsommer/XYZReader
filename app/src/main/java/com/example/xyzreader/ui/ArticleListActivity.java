package com.example.xyzreader.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
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

// Loaders are depreciates as of API 28. Leave them in this app as they are integral to its function
// and the purpose of this exercise is to implement Google Material Design principles, not to
// refactor Loaders to the ViewModel / LiveData pattern.
@SuppressWarnings("deprecation")
public class ArticleListActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // member variables
    private static final String TAG = ArticleListActivity.class.toString();
    private RecyclerView mRecyclerView;
    private Context mContext;
    private ImageButton mRefreshButton;
    private Animation mRotation;
    private boolean mIsRefreshing;

    // use default locale date formats, most time functions can only handle years 1902 - 2037
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    private DateFormat outputFormat = DateFormat.getDateInstance();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // inflate layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_material_design);

        // set member references
        mContext = this;

        // get view references
        mRecyclerView = findViewById(R.id.recycler_view);
        mRefreshButton = findViewById(R.id.refresh_main);

        // clean up action bar
        setSupportActionBar(findViewById(R.id.toolbar_list));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        // set a rotating animation on the refresh button
        mRotation = AnimationUtils.loadAnimation(mContext, R.anim.rotate_refresh);
        mRefreshButton.startAnimation(mRotation);

        // clicking the refresh button starts the rotation animation and updates the UI
        mRefreshButton.setOnClickListener((View view) -> {
            mRefreshButton.startAnimation(mRotation);
            startUpdaterService();
        });

        // initialize an ArticleLoader
        getSupportLoaderManager().initLoader(0, null, this);

        // update the UI via the service if the app is starting from a destroyed state
        if (savedInstanceState == null) {
            startUpdaterService();
        }
    }

    // simple helper function starts updater service class
    private void startUpdaterService() {
        Intent intentUpdaterService = new Intent(this, UpdaterService.class);
        startService(intentUpdaterService);
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
        RecyclerAdapter recyclerAdapter = new RecyclerAdapter(cursor);

        // stable IDs allow for memory usage optimization
        recyclerAdapter.setHasStableIds(true);

        // associate adapter to recycler view
        mRecyclerView.setAdapter(recyclerAdapter);

        // set number of columns in a staggered grid for the recycler view
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

    }

    // refresh the UI
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // results in refreshing the UI
        mRecyclerView.setAdapter(null);
    }

    // simple adapter using a local DB cursor and viewholder
    private class RecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {

        // member variable
        private Cursor mCursor;

        // instantiate adapter with a database cursor
        public RecyclerAdapter(Cursor cursor) {
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
            View view = getLayoutInflater().inflate(R.layout.list_item_material_design, parent, false);

            // create a new viewholder
            final ViewHolder viewHolder = new ViewHolder(view);

            // set a click listener on the item layout that starts the ArticleDetailActivity
            view.setOnClickListener((View clickedView) -> {

                // URI of specific article
                Uri itemUri = ItemsContract.Items.buildItemUri(getItemId(viewHolder.getAdapterPosition()));

                // start ArticleDetailActivity with the selected article
                Intent intent = new Intent(mContext, ArticleDetailActivity.class);
                intent.setData(itemUri);
                startActivity(intent);

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
                Log.e(TAG, "Error parsing today's date.");
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
