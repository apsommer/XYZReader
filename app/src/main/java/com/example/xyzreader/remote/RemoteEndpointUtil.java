package com.example.xyzreader.remote;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RemoteEndpointUtil {

    // constants
    private static final String TAG = "RemoteEndpointUtil";

    private RemoteEndpointUtil() {}

    public static JSONArray fetchJsonArray() {

        // get content from Udacity server using helper function
        String itemsJson = null;
        try {
            itemsJson = fetchPlainText(Config.BASE_URL);
        } catch (IOException e) {
            Log.e(TAG, "Error fetching JSON.", e);
            return null;
        }

        // parse the JSON into array tokens
        try {
            JSONTokener tokener = new JSONTokener(itemsJson);
            Object val = tokener.nextValue();
            if (!(val instanceof JSONArray)) {
                throw new JSONException("Expected JSONArray.");
            }
            return (JSONArray) val;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON.", e);
        }

        return null;
    }

    // get content from Udacity server
    static String fetchPlainText(URL url) throws IOException {

        // third party library
        OkHttpClient client = new OkHttpClient();

        // formulate URL into library object
        Request request = new Request.Builder().url(url).build();

        // obtain response as string from Udacity server
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
