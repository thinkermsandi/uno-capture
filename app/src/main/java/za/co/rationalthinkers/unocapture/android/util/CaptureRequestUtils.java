package za.co.rationalthinkers.unocapture.android.util;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.RggbChannelVector;

import za.co.rationalthinkers.unocapture.android.config.FlashMode;

public class CaptureRequestUtils {

    public static void setControlMode(CaptureRequest.Builder builder, int controlMode){
        builder.set(CaptureRequest.CONTROL_MODE, controlMode);
    }

    public static boolean setFlashMode(CaptureRequest.Builder builder, int flashMode) {
        switch(flashMode) {
            case FlashMode.FLASH_MODE_OFF:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                return true;

            case FlashMode.FLASH_MODE_ON:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                return true;

            case FlashMode.FLASH_MODE_AUTO:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                return true;

            case FlashMode.FLASH_MODE_TORCH:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                return true;

            default:
                return false;
        }
    }

    public static void setFocusMode(CaptureRequest.Builder builder, boolean enabled, int focusMode) {
        if( enabled ){
            builder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
        }
        else{
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    /*public static void setCropRegion(CaptureRequest.Builder builder) {
        if( scalar_crop_region != null ) {
            builder.set(CaptureRequest.SCALER_CROP_REGION, scalar_crop_region);
        }
    }

    public static void setFocusDistance(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus_distance);
    }

    public static void setAutoExposureLock(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AE_LOCK, ae_lock);
    }

    public static void setAutoWhiteBalanceLock(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AWB_LOCK, wb_lock);
    }*/

    public static void setFaceDetectMode(CaptureRequest.Builder builder, boolean enabled, int faceDetectMode) {
        if( enabled ){
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectMode);
        }
        else{
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF);
        }
    }

    public static void setVideoStabilizationMode(CaptureRequest.Builder builder, boolean enabled) {
        if(enabled){
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
        }
        else{
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        }
    }

    public static boolean setSceneMode(CaptureRequest.Builder builder, int sceneMode, boolean hasFaceDetectMode) {
        Integer currentSceneMode = builder.get(CaptureRequest.CONTROL_SCENE_MODE);

        if(hasFaceDetectMode) {
            // face detection mode overrides scene mode
            if( currentSceneMode == null || currentSceneMode != CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY ) {
                builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE);
                builder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY);

                return true;
            }
        }
        else if(currentSceneMode == null || currentSceneMode != sceneMode) {
            if(sceneMode == CameraMetadata.CONTROL_SCENE_MODE_DISABLED ) {
                builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            }
            else {
                builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE);
            }
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, sceneMode);

            return true;
        }

        return false;
    }

    public static boolean setColorEffect(CaptureRequest.Builder builder, int colorEffectMode) {
        if( builder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null || builder.get(CaptureRequest.CONTROL_EFFECT_MODE) != colorEffectMode) {
            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, colorEffectMode);

            return true;
        }

        return false;
    }

    /*public static boolean setWhiteBalance(CaptureRequest.Builder builder, int whiteBalanceMode) {
        boolean changed = false;

        if( builder.get(CaptureRequest.CONTROL_AWB_MODE) == null || builder.get(CaptureRequest.CONTROL_AWB_MODE) != whiteBalanceMode ) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, whiteBalanceMode);

            changed = true;
        }

        if(whiteBalanceMode == CameraMetadata.CONTROL_AWB_MODE_OFF ) {
            RggbChannelVector rggbChannelVector = ColorUtils.convertTemperatureToRggb(white_balance_temperature);

            builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbChannelVector);

            changed = true;
        }

        return changed;
    }*/

    public static boolean setAntiBanding(CaptureRequest.Builder builder, boolean enabled, int antiBandingMode) {
        boolean changed = false;

        if(enabled) {
            if( builder.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE) == null || builder.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE) != antiBandingMode ) {
                builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antiBandingMode);

                changed = true;
            }
        }

        return changed;
    }

}
