package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

// Loaders are depreciates as of API 28. Leave them in this app as they are integral to its function
// and the purpose of this exercise is to implement Google Material Design principles, not to
// refactor Loaders to the ViewModel / LiveData pattern.
@SuppressWarnings("deprecation")
public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // member variables
    private Cursor mCursor;
    private long mStartId;
    private ViewPager mPager;
    private DetailPagerAdapter mPagerAdapter;
    private TabLayout mTabLayout;
    private Context mContext;
    private ImageView mRefreshButton;
    private Animation mRotation;
    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // inflate layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // set member references
        mContext = this;

        // get view references
        mPager = findViewById(R.id.pager);
        mTabLayout = findViewById(R.id.tabs);

        // clean up action bar
        setSupportActionBar(findViewById(R.id.toolbar_detail));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(0, null, this);

        // associate the adapter and tabs to the viewpager
        mPagerAdapter = new DetailPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        // move cursor when page is changed
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    mPosition = position;
                }
            }
        });

        // get the article ID the first time activity is run
        if (savedInstanceState == null && getIntent() != null && getIntent().getData() != null) {
            mStartId = ItemsContract.Items.getItemId(getIntent().getData());
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

            // TODO comment
            getContentResolver().notifyChange(ItemsContract.Items.buildDirUri(), null);

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

    // return a new ArticleLoader
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        // set the cursor and update the UI
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // article ID is always > 0
        if (mStartId > 0) {

            // move to the first row and iterate through cursor
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {

                // current page ID matches article ID
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {

                    // record position and set pager to this item
                    mPosition = mCursor.getPosition();
                    mPager.setCurrentItem(mPosition, false);
                    break;
                }
                mCursor.moveToNext();
            }

            // only need to perform this search first time through coming from ArticleDetailActivity
            mStartId = 0;
        }

        // setup custom tabs in toolbar
        setupTabs();
    }

    // remove all data from cursor and UI
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private void setupTabs() {

        mTabLayout.setupWithViewPager(mPager);

        // iterate through tabs setting custom view on each
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            tab.setCustomView(mPagerAdapter.getTabView(i));
        }

        // when the activity starts the first tab has to be formatted manually as state "selected"
        TabLayout.Tab tab = mTabLayout.getTabAt(mPosition);
        TextView textView = (TextView) tab.getCustomView();
        textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.accent_A200));
        textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);

        // tab click listener
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            // selected tab is accent color and bold
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                TextView textView = (TextView) tab.getCustomView();

                if (textView != null) {
                    textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.accent_A200));
                    textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
                }
            }

            // unselected tab is white with normal style
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

                TextView textView = (TextView) tab.getCustomView();

                if (textView != null) {
                    textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                    textView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                }
            }

            // do nothing, same as selected state
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // simple adapter for viewpager
    private class DetailPagerAdapter extends FragmentStatePagerAdapter {

        // constructor calls superclass
        public DetailPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // get new article detail fragment
        @Override
        public Fragment getItem(int position) {

            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        // return the total number of articles
        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

        // supplies labels for tablayout, called by viewpager
        @Override
        public CharSequence getPageTitle(int position) {

            // use simple integers for tab titles
            // add one due to zero based indexing
            return String.valueOf(position + 1);
        }

        // returns an inflated custom tab layout textview
        public View getTabView(int position) {

            // root view is the single TextView
            View tabView = LayoutInflater.from(mContext).inflate(R.layout.tab_detail, null);
            TextView textView = tabView.findViewById(R.id.tabTextView);

            // set the tab text, font, and capitalization
            textView.setText(getPageTitle(position));

            return tabView;
        }

    }
}
