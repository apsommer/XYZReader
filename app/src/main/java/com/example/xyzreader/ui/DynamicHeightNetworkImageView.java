package com.example.xyzreader.ui;

import android.content.Context;
import android.icu.util.Measure;
import android.util.AttributeSet;
import android.util.Log;

import com.android.volley.toolbox.NetworkImageView;

// custom UI element
public class DynamicHeightNetworkImageView extends NetworkImageView {

    // aspect ratio = width / height
    private float mAspectRatio = 1f;

    // constructors defer to superclass
    public DynamicHeightNetworkImageView(Context context) {super(context);}
    public DynamicHeightNetworkImageView(Context context, AttributeSet attrs) {super(context, attrs);}
    public DynamicHeightNetworkImageView(Context context, AttributeSet attrs, int defStyle) {super(context, attrs, defStyle);}

    // set aspect ratio of the image
    public void setAspectRatio(float aspectRatio) {

        mAspectRatio = aspectRatio;

        // redraw layout calls onMeasure
        requestLayout();
    }

    // enforce aspect ratio
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {

        int heightScaled = (int) (MeasureSpec.getSize(widthSpec) / mAspectRatio);
        int widthScaled = MeasureSpec.getSize(widthSpec);
        int heightScaledSpec = MeasureSpec.makeMeasureSpec(heightScaled, MeasureSpec.EXACTLY);

        super.onMeasure(widthSpec, heightScaledSpec);
    }
}
