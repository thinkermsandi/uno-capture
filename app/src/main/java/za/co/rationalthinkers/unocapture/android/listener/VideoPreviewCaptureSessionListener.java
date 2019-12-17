package za.co.rationalthinkers.unocapture.android.listener;

import android.hardware.camera2.CameraCaptureSession;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.VideoFragment;

public class VideoPreviewCaptureSessionListener extends CameraCaptureSession.StateCallback {

    private WeakReference<VideoFragment> _fragment;

    public VideoPreviewCaptureSessionListener(VideoFragment fragment){
        _fragment = new WeakReference<>(fragment);
    }

    @Override
    public void onConfigured(@NonNull CameraCaptureSession session) {
        VideoFragment fragment = _fragment.get();

        if(fragment !=  null){
            // When the session is ready, we start displaying the preview.
            fragment.mPreviewSession = session;
            fragment.updatePreview();
        }
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        VideoFragment fragment = _fragment.get();

        if(fragment !=  null){
            Toast.makeText(fragment.getContext(), "Failed", Toast.LENGTH_SHORT).show();
        }
    }

}
