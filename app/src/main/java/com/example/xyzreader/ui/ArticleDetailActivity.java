package com.example.xyzreader.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

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
    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;
    private ViewPager mPager;
    private DetailPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate the detail layout
        setContentView(R.layout.activity_detail_material_design);

        // get reference to children views
        mPager = findViewById(R.id.pager);
//        mUpButton = findViewById(R.id.action_up);
//        mUpButtonContainer = findViewById(R.id.up_container);

        // for SDK > Lollipop
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//
//            // expand layout to full screen
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//
//            // customize the position of the up button
//            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
//                @Override
//                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
//
//                    // superclass
//                    view.onApplyWindowInsets(windowInsets);
//
//                    // get the top position of the window and set the up button to it
//                    mTopInset = windowInsets.getSystemWindowInsetTop();
//                    mUpButtonContainer.setTranslationY(mTopInset);
//                    updateUpButtonPosition();
//                    return windowInsets;
//                }
//            });
//        }

        // instantiate a loader
        getSupportLoaderManager().initLoader(0, null, this);

        // create a custom adapter and associate it to the viewpager
        mPagerAdapter = new DetailPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        // convert margin from dp to raw px and set it on the viewpager
        // TODO make zero?
        int marginBetweenPagesPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        mPager.setPageMargin(marginBetweenPagesPx);

        // set color of the page margin
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        // TODO this is all messed, not clear there are even pages!
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            // animate up button
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);

                // TODO fix this animation, currently produces a fade, make it spin
//                mUpButton.animate()
//                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
//                        .setDuration(300);
            }

            // move cursor
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
//                updateUpButtonPosition();
            }
        });

        // normal up button behavior
//        mUpButton.setOnClickListener((View view) -> onSupportNavigateUp());

        // no saved state bundle the first time activity is run
        if (savedInstanceState == null) {

            // check the intent exists and has data in it
            if (getIntent() != null && getIntent().getData() != null) {

                // save the selected article to member variables
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
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

    // callback from the ActivityDetailFragment
//    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
//
//        // update the up button position
//        if (itemId == mSelectedItemId) {
//            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
//            updateUpButtonPosition();
//        }
//    }

    // move the up button to correct position in top left
//    private void updateUpButtonPosition() {
//        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
//        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
//    }

    // simple adapter for viewpager
    private class DetailPagerAdapter extends FragmentStatePagerAdapter {

        // constructor calls superclass
        public DetailPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // move up button to correct location
        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
//                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
//                updateUpButtonPosition();
            }
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
