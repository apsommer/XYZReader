package com.example.xyzreader.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

// Loaders are depreciates as of API 28. Leave them in this app as they are integral to its function
// and the purpose of this exercise is to implement Google Material Design principles, not to
// refactor Loaders to the ViewModel / LiveData pattern.
@SuppressWarnings("deprecation")
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // constants
    private static final String TAG = "ArticleDetailFragment";
    public static final String ARG_ITEM_ID = "item_id";

    // member variables
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private ImageView mPhotoView;

    // use default locale date formats
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    private DateFormat outputFormat = DateFormat.getDateInstance();

    // most time functions can only handle years 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    // bundle an ID with the fragment and return it
    public static ArticleDetailFragment newInstance(long itemId) {

        // put ID into bundle
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);

        // put bundle into fragment and return it
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the fragment ID
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    // initialize the loader
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // initialize an ArticleLoader
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate the layout
        mRootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // get view references
        mPhotoView = mRootView.findViewById(R.id.photo);

        mRootView.findViewById(R.id.share_fab).setOnClickListener((View view) -> {

            startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                    .setType("text/plain")
                    .setText(getActivity().getString(R.string.share_intent_sample_text))
                    .getIntent(), getString(R.string.action_share)));
        });

        bindViews();
        return mRootView;
    }

    // reformat date string
    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, "Error parsing today's date.");
            return new Date();
        }
    }

    private void bindViews() {

        // check that the root layout is valid
        if (mRootView == null) {
            return;
        }

        // get view references
        TextView titleView = mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        TextView bodyView = mRootView.findViewById(R.id.article_body);

        // custom font for article body
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "font/rosario.ttf"));

        if (mCursor != null) {

            // loader is finished so show the fragment with a simple fade animation
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            // display the title
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            // display the date and author using <html> tag format
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));
            } else {
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));
            }

            // display the article body using <html> tag format
            bodyView.setText(Html.fromHtml(
                    mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));

            // set the article image using the 'volley' library
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {

                        // set the image
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        // do nothing
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {}
                    });

        // the loader is not finished, or there is no article, so hide everything
        } else {

            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    // create a new ArticleLoader
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        // if the fragment is not added yet then close the cursor
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        // check that the cursor loaded correctly
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        // update UI with the finished cursor
        bindViews();
    }

    // remove the cursor and update the UI
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
