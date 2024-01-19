package com.arcvideo.vehicletrack.presentation;

import android.app.Presentation;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class ArcPresentation extends Presentation {

    private RelativeLayout mPresentationLayout = null;
    private View mViewLayout = null;
    private int mLayoutId = -1;
    private int mPresentationViewId = -1;
    private Context mContext = null;

    public ArcPresentation(Context context, Display display){
        super(context,display);
        mContext = context;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                this.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }else
            {
                this.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }
        }
    }

    public void setContentViewLayoutId(int layoutId){
        mLayoutId = layoutId;
    }

    public void setPresentationRelativeLayoutId(int viewId){
        mPresentationViewId = viewId;
    }

    public static Display[] getPresentationDisplays(Context context){
        DisplayManager dispManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] presentationDisplays = dispManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        return presentationDisplays;
    }

    @Override
    public void show(){
        if (mLayoutId == -1){
            throw new IllegalArgumentException("ArcPresentation: Content view layout id is invalid, please set again by call setPresentationLayoutViewId method !");
        }

        if (mPresentationViewId == -1){
            throw new IllegalArgumentException("ArcPresentation: PresentationLayout is invalid, please set again by call setPresentationLayout method !");
        }

        super.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Get the resources for the context of the presentation.
        // Notice that we are getting the resources from the context of the presentation.
        Resources r = getContext().getResources();
        // Inflate the layout.
        setContentView(mLayoutId);
        mPresentationLayout = (RelativeLayout)this.findViewById(mPresentationViewId);

        //Assign the correct layout
        //mViewLayout = getLayoutInflater().inflate(R.layout.presentation, null);
        //setContentView(mViewLayout);
        //mPresentationLayout = (RelativeLayout)findViewById(R.id.presviews);
        //We need to wait till the view is created so we can flip it and set the width & height dynamically
        //mViewLayout.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener());
    }

    public void addChildView(View childView){
        if (mPresentationLayout != null){
            mPresentationLayout.addView(childView);
        }
    }

    public int getChildViewCount(){
        int count = 0;
        if (mPresentationLayout != null){
            count = mPresentationLayout.getChildCount();
        }
        return count;
    }

    private class OnGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener{

        @Override
        public void onGlobalLayout() {
            //height is ready
            mViewLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            int width = mViewLayout.getWidth();
            int height = mViewLayout.getHeight();

            mViewLayout.setTranslationX((width - height)/2);
            mViewLayout.setTranslationY(0);

            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams( height, width);

            // Inflate the layout.
            setContentView(mViewLayout, lp);
        }
    }
}
