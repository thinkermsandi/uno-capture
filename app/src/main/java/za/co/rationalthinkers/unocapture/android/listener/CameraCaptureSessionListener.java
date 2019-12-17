package za.co.rationalthinkers.unocapture.android.listener;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaActionSound;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.CameraFragment;

import static za.co.rationalthinkers.unocapture.android.config.Constants.STATE_PICTURE_TAKEN;
import static za.co.rationalthinkers.unocapture.android.config.Constants.STATE_PREVIEW;
import static za.co.rationalthinkers.unocapture.android.config.Constants.STATE_WAITING_LOCK;
import static za.co.rationalthinkers.unocapture.android.config.Constants.STATE_WAITING_NON_PRECAPTURE;
import static za.co.rationalthinkers.unocapture.android.config.Constants.STATE_WAITING_PRECAPTURE;

/**
 * Handles events related to JPEG capture.
 */
public class CameraCaptureSessionListener extends CameraCaptureSession.CaptureCallback {

    private WeakReference<CameraFragment> _fragment;

    public CameraCaptureSessionListener(CameraFragment fragment){
        _fragment = new WeakReference<>(fragment);
    }

    @Override
    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        process(partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        process(result);
    }

    private void process(CaptureResult result) {
        CameraFragment fragment = _fragment.get();

        if(fragment ==  null){

            return;
        }

        switch (fragment.mState) {
            case STATE_PREVIEW: {
                // We have nothing to do when the camera preview is working normally.
                break;
            }

            case STATE_WAITING_LOCK: {
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                if (afState == null) {
                    fragment.captureStillPicture();
                }
                else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        fragment.mState = STATE_PICTURE_TAKEN;
                        fragment.captureStillPicture();
                    }
                    else {
                        fragment.runPrecaptureSequence();
                    }
                }

                break;
            }

            case STATE_WAITING_PRECAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    fragment.mState = STATE_WAITING_NON_PRECAPTURE;
                }

                break;
            }
            case STATE_WAITING_NON_PRECAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    fragment.mState = STATE_PICTURE_TAKEN;
                    fragment.captureStillPicture();
                }

                break;
            }
        }
    }

}
