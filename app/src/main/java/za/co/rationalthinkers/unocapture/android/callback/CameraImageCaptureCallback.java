package za.co.rationalthinkers.unocapture.android.callback;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.CameraFragment;

public class CameraImageCaptureCallback extends CameraCaptureSession.CaptureCallback {

    private WeakReference<CameraFragment> _fragment;

    public CameraImageCaptureCallback(CameraFragment fragment){
        _fragment = new WeakReference<>(fragment);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

        CameraFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.unlockFocus();
            fragment.onImageSaved();
        }

    }

}
