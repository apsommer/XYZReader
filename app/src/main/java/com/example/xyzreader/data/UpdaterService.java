package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.example.xyzreader.remote.RemoteEndpointUtil;

public class UpdaterService extends IntentService {

    // string constants
    private static final String TAG = "UpdaterService";
    public static final String BROADCAST_ACTION_STATE_CHANGE = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING = "com.example.xyzreader.intent.extra.REFRESHING";

    // required constructor
    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // TODO the passed intent is not used, this service updates the database every time its started
        // TODO a check against the existing database values should be implemented first

        // get current time
        Time time = new Time();

        // get system network connection manager
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        // get the active network information
        if (connectivityManager != null) {

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            // exit for no network or network disconnected
            if (networkInfo == null || !networkInfo.isConnected()) {
                Log.w(TAG, "Not online, not refreshing.");
                return;
            }
        }

        // send a system broadcast that the service state has changed to "refreshing"
        Intent intentStateChange = new Intent(BROADCAST_ACTION_STATE_CHANGE);
        intentStateChange.putExtra(EXTRA_REFRESHING, true);
        sendBroadcast(intentStateChange);

        // path to items endpoint
        Uri dirUri = ItemsContract.Items.buildDirUri();

        // array of content provider operations
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();

        // delete all the articles
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {

            // get JSON payload from Udacity server
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed JSON array." );
            }

            // loop through the payload and store in persistent database
            for (int i = 0; i < array.length(); i++) {

                // new row
                JSONObject object = array.getJSONObject(i);
                ContentValues values = new ContentValues();

                // extract article metadata
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id" ));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author" ));
                values.put(ItemsContract.Items.TITLE, object.getString("title" ));
                values.put(ItemsContract.Items.BODY, object.getString("body" ));
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb" ));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo" ));
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio" ));
                values.put(ItemsContract.Items.PUBLISHED_DATE, object.getString("published_date"));

                // add row to "operations holder"
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            // put all rows into the database simulatenously
            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        }

        // broadcast to the system that refreshing is complete
        intentStateChange.putExtra(EXTRA_REFRESHING, false);
        sendBroadcast(intentStateChange);
    }
}
