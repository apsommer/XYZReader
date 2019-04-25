package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

// implementation of the volley library
public class ImageLoaderHelper {

    // member variables
    private static ImageLoaderHelper sInstance;

    // singleton pattern
    public static ImageLoaderHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoaderHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    // LRU = least recently used
    private final LruCache<String, Bitmap> mImageCache = new LruCache<>(20);

    // instantiate the loading object
    private ImageLoader mImageLoader;

    // constructor sets the loading object
    private ImageLoaderHelper(Context applicationContext) {

        // this queue lasts for the app lifetime
        RequestQueue queue = Volley.newRequestQueue(applicationContext);

        // create a new image cache
        ImageLoader.ImageCache imageCache = new ImageLoader.ImageCache() {

            // add images to the LRU cache
            @Override
            public void putBitmap(String key, Bitmap value) {
                mImageCache.put(key, value);
            }

            // retreive an image from the LRU cache
            @Override
            public Bitmap getBitmap(String key) {
                return mImageCache.get(key);
            }
        };

        // set the loading object
        mImageLoader = new ImageLoader(queue, imageCache);
    }

    // expose member variable
    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
