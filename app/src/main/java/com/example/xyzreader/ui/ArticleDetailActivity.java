package com.example.xyzreader.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // member variables
    private Cursor mCursor;
    private long mStartId;
    private ViewPager mPager;
    private DetailPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate the detail layout
        setContentView(R.layout.activity_detail_material_design);

        // get reference to children views
        mPager = findViewById(R.id.pager);

        // initialize an ArticleLoader
        // leave depreciated getSupportLoaderManager as it is integral to the app's function and the
        // purpose of this exercise is implementing Google Material Design Principles, not refactoring Loaders
        //noinspection deprecation:
        getSupportLoaderManager().initLoader(0, null, this);

        // create a custom adapter and associate it to the viewpager
        mPagerAdapter = new DetailPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        // convert margin from dp to raw px and set it on the viewpager
        // TODO make zero permanent?
        int marginBetweenPagesPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics());
        mPager.setPageMargin(marginBetweenPagesPx);

        // set color of the page margin
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        // TODO this is all messed, not clear there are even pages!
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);

                // TODO animate?
            }

            // move cursor
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }

            }
        });

        // no saved state bundle the first time activity is run
        if (savedInstanceState == null) {

            // check the intent exists and has data in it
            if (getIntent() != null && getIntent().getData() != null) {

                // save the selected article to member variables
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());

            }
        }
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

        // select the start ID
        if (mStartId > 0) {

            // move to first row
            mCursor.moveToFirst();

            // TODO: optimize ... why loop at all just set to zero?
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    // remove all data from cursor and UI
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
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
    }
}
