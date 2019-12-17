package za.co.rationalthinkers.unocapture.android.listener;

import android.view.OrientationEventListener;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.activity.CaptureActivity;

public class ActivityOrientationEventListener extends OrientationEventListener {

    private WeakReference<CaptureActivity> _activity;

    public ActivityOrientationEventListener(CaptureActivity activity){
        super(activity.getApplicationContext());

        _activity = new WeakReference<>(activity);
    }

    @Override
    public void onOrientationChanged(int orientation) {

        CaptureActivity activity = _activity.get();
        if(activity != null){
            activity.onNewOrientation(orientation);
        }

    }

}
