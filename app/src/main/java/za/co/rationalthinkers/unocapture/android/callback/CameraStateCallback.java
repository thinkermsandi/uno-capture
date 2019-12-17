package za.co.rationalthinkers.unocapture.android.callback;

import android.app.Activity;
import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.CameraFragment;

/**
 * CameraDevice.StateCallback is called when CameraDevice changes its state.
 */
public class CameraStateCallback extends CameraDevice.StateCallback {

    private WeakReference<CameraFragment> _fragment;

    public CameraStateCallback(CameraFragment fragment){
        _fragment = new WeakReference<>(fragment);
    }

    /**
     * This method is called when the camera is opened.
     * We start camera preview here.
     */
    @Override
    public void onOpened(@NonNull CameraDevice camera) {

        CameraFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.mCameraDevice = camera;
            fragment.startPreview();
            fragment.mCameraOpenCloseLock.release();
        }

    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {

        CameraFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.mCameraOpenCloseLock.release();
            camera.close();
            fragment.mCameraDevice = null;
        }

    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {

        CameraFragment fragment = _fragment.get();

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
