package za.co.rationalthinkers.unocapture.android.callback;

import android.app.Activity;
import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.VideoFragment;

public class VideoStateCallback extends CameraDevice.StateCallback {

    private WeakReference<VideoFragment> _fragment;

    public VideoStateCallback(VideoFragment fragment){
        _fragment = new WeakReference<>(fragment);
    }

    /**
     * This method is called when the camera is opened.
     * We start camera preview here.
     */
    @Override
    public void onOpened(@NonNull CameraDevice camera) {

        VideoFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.mCameraDevice = camera;
            fragment.startPreview();
            fragment.mCameraOpenCloseLock.release();
        }

    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {

        VideoFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.mCameraOpenCloseLock.release();
            camera.close();
            fragment.mCameraDevice = null;
        }

    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {

        VideoFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.mCameraOpenCloseLock.release();
            camera.close();
            fragment.mCameraDevice = null;

            Activity activity = fragment.getActivity();
            if (activity != null) {
                activity.finish();
            }
        }

    }

}
