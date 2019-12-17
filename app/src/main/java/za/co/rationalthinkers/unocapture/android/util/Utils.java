package za.co.rationalthinkers.unocapture.android.util;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Size;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import za.co.rationalthinkers.unocapture.android.config.MediaType;

import static android.content.Context.CAMERA_SERVICE;
import static za.co.rationalthinkers.unocapture.android.config.Constants.APP_NAME;

public class Utils {

    /** Check if this device has a camera */
    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        }
        else {
            // no camera on this device
            return false;
        }
    }

    public static ArrayList<String> getDeviceCameras(Context context){
        ArrayList<String> cameras;

        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        try {
            cameras = new ArrayList<>(Arrays.asList(manager.getCameraIdList()));
        }
        catch (CameraAccessException e) {
                return null;
        }

        return cameras;
    }

    public static boolean hasFrontCamera(Context context, String cameraId) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        try {
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);

            // Does the camera have a forwards facing lens?
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return true;
            }
        }
        catch (CameraAccessException e) {
                return false;
        }

        return false;
    }

    public static boolean hasBackCamera(Context context, String cameraId) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        try {
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);

            // Does the camera have a forwards facing lens?
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return true;
            }
        }
        catch (CameraAccessException e) {
            return false;
        }

        return false;
    }

    public static boolean hasExternalCamera(Context context, String cameraId) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        try {
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);

            // Does the camera have a forwards facing lens?
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                return true;
            }
        }
        catch (CameraAccessException e) {
            return false;
        }

        return false;
    }

    public static boolean hasFlash(Context context, String cameraId) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        try {
            CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);

            // Does the camera have a forwards facing lens?
            Boolean hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

            return hasFlash != null ? hasFlash : false;
        }
        catch (CameraAccessException e) {
            return false;
        }
    }

    public static boolean supportsTextureView(){
        if (GLES20.glGetString(GLES20.GL_RENDERER) == null ||
                GLES20.glGetString(GLES20.GL_VENDOR) == null ||
                GLES20.glGetString(GLES20.GL_VERSION) == null ||
                GLES20.glGetString(GLES20.GL_EXTENSIONS) == null ||
                GLES10.glGetString(GLES10.GL_RENDERER) == null ||
                GLES10.glGetString(GLES10.GL_VENDOR) == null ||
                GLES10.glGetString(GLES10.GL_VERSION) == null ||
                GLES10.glGetString(GLES10.GL_EXTENSIONS) == null) {
            // try to use SurfaceView
            return false;
        }
        else {
            // try to use TextureView
            return true;
        }
    }

    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                //failed to create directory
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MediaType.IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
        }
        else if(type == MediaType.VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4");
        }
        else {
            return null;
        }

        return mediaFile;
    }

    /**
     * Given choices of Size's supported by a camera,
     * choose the smallest one that is at least as large as the respective texture view size,
     * and that is at most as large as the respective max size, and whose aspect ratio matches with the specified value.
     *
     * If such size doesn't exist,
     * choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough.
        // If there is no one big enough, pick the largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }
        else {
            //Couldn't find any suitable preview size
            return choices[0];
        }
    }

    /**
     * Iterate over supported camera video sizes to see which one best fits the
     * dimensions of the given view while maintaining the aspect ratio.
     * If none can, be lenient with the aspect ratio.
     *
     * @param supportedVideoSizes Supported camera video sizes.
     * @param previewSizes Supported camera preview sizes.
     * @param w     The width of the view.
     * @param h     The height of the view.
     * @return Best match camera video size to fit in the view.
     */
    public static Camera.Size getOptimalVideoSize(List<Camera.Size> supportedVideoSizes, List<Camera.Size> previewSizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        List<Camera.Size> videoSizes;
        if (supportedVideoSizes != null) {
            videoSizes = supportedVideoSizes;
        }
        else {
            videoSizes = previewSizes;
        }
        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = h;

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();

        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else {
            //"Couldn't find any suitable preview size"
            return choices[0];
        }
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    public static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }

        //"Couldn't find any suitable video size"
        return choices[choices.length - 1];
    }

    /**
     * @return the default camera on the device. Return null if there is no camera on the device.
     */
    public static Camera getDefaultCameraInstance() {
        return Camera.open();
    }


    /**
     * @return the default rear/back facing camera on the device. Returns null if camera is not
     * available.
     */
    public static Camera getDefaultBackFacingCameraInstance() {
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * @return the default front facing camera on the device. Returns null if camera is not
     * available.
     */
    public static Camera getDefaultFrontFacingCameraInstance() {
        return getDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }


    /**
     *
     * @param position Physical position of the camera i.e Camera.CameraInfo.CAMERA_FACING_FRONT
     *                 or Camera.CameraInfo.CAMERA_FACING_BACK.
     * @return the default camera on the device. Returns null if camera is not available.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static Camera getDefaultCamera(int position) {
        // Find the total number of cameras available
        int  mNumberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the back-facing ("default") camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == position) {
                return Camera.open(i);

            }
        }

        return null;
    }

    /**
     * Get the file path from a Media storage URI.
     */
    public static String getPathFromURI(ContentResolver contentResolver, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};

        Cursor cursor = contentResolver.query(contentUri, proj, null, null, null);

        if (cursor == null) {
            return null;
        }

        try {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            if (!cursor.moveToFirst()) {
                return null;
            }
            else {
                return cursor.getString(columnIndex);
            }
        }
        finally {
            cursor.close();
        }
    }

    /**
     *  Similar view.setRotation(ui_rotation), but achieves this via an animation.
     */
    public static void setViewRotation(View view, float rotation) {
        float rotateBy = rotation - view.getRotation();
        if( rotateBy > 181.0f ){
            rotateBy -= 360.0f;
        }
        else if( rotateBy < -181.0f ){
            rotateBy += 360.0f;
        }

        // view.animate() modifies the view's rotation attribute,
        // so it ends up equivalent to view.setRotation()
        // we use rotationBy() instead of rotation(),
        // so we get the minimal rotation for clockwise vs anti-clockwise
        view.animate().rotationBy(rotateBy).setDuration(100).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    public static void rotateView(View view, int startDeg, int endDeg) {
        view.setRotation(startDeg);
        view.animate().rotation(endDeg).start();
    }

    /**
     * Matches code in MediaProvider.computeBucketValues. Should be a common
     * function.
     */
    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }

}
