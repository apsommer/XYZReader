package com.example.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

// simple class that stores configuration of Udacity server endpoint
public class Config {

    // constants
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();

    // set URL constant for
    static {

        URL url = null;
        try {

            //alternate url https://go.udacity.com/xyz-reader-json
            url = new URL("https://raw.githubusercontent.com/SuperAwesomeness/XYZReader/master/data.json" );
        } catch (MalformedURLException ignored) {

            // log an error is sufficient for this example app
            Log.e(TAG, "Please check your internet connection.");
        }

        // set app level constant
        BASE_URL = url;
    }
}
