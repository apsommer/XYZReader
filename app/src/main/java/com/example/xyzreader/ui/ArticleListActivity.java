package com.example.xyzreader.ui;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private ImageView mRefreshButton;
    private boolean mIsTablet;

    // use default locale date formats, most time functions can only handle years 1902 - 2037
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    private DateFormat outputFormat = DateFormat.getDateInstance();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // inflate layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set member references
        mContext = this;
        mIsTablet = getResources().getBoolean(R.bool.is_tablet);

        // get view references
        mRecyclerView = findViewById(R.id.recycler_view);

        // clean up action bar
        setSupportActionBar(findViewById(R.id.toolbar_list));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        // initialize an ArticleLoader
        getSupportLoaderManager().initLoader(0, null, this);

        // update the UI via the service if the app is starting from a destroyed state
        if (savedInstanceState == null) {
            startUpdaterService();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // inflate the single item menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // double check the item ID
        if (item.getItemId() == R.id.menu_item) {

            // define a rotation animation
            Animation rotation = AnimationUtils.loadAnimation(mContext, R.anim.rotation);

            // start the animation on the refresh button
            mRefreshButton.startAnimation(rotation);

            // restart the loader via the update service
            startUpdaterService();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // get references to the custom menu item layout
        final MenuItem menuItem = menu.findItem(R.id.menu_item);
        FrameLayout rootView = (FrameLayout) menuItem.getActionView();
        mRefreshButton = rootView.findViewById(R.id.refresh_icon);

        // this manual call to onOptionsItemSelected is required when using a custom layout
        rootView.setOnClickListener((View view) -> onOptionsItemSelected(menuItem));

        return super.onPrepareOptionsMenu(menu);
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

        // phone UI
        if (!mIsTablet) {

            RecyclerView.LayoutManager layoutManager =
                    new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(layoutManager);

        // tablet UI
        } else {

            // set number of columns in a staggered grid for the recycler view
            int columnCount = getResources().getInteger(R.integer.grid_column_count);
            StaggeredGridLayoutManager layoutManager =
                    new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(layoutManager);
        }
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
            View view = getLayoutInflater().inflate(R.layout.item_main, parent, false);

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

            // setAspectRatio dynamically defines the view bounds in set in xml
            // which allows the card views in a tablet to vary in size
            if (mIsTablet) {
                holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            }
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
