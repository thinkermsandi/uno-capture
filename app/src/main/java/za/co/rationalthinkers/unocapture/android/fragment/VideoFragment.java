package za.co.rationalthinkers.unocapture.android.fragment;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import za.co.rationalthinkers.unocapture.android.R;
import za.co.rationalthinkers.unocapture.android.callback.VideoStateCallback;
import za.co.rationalthinkers.unocapture.android.config.MediaType;
import za.co.rationalthinkers.unocapture.android.config.Settings;
import za.co.rationalthinkers.unocapture.android.listener.VideoPreviewCaptureSessionListener;
import za.co.rationalthinkers.unocapture.android.listener.VideoSurfaceTextureListener;
import za.co.rationalthinkers.unocapture.android.model.MediaFile;
import za.co.rationalthinkers.unocapture.android.observer.VideoFilesObserver;
import za.co.rationalthinkers.unocapture.android.util.CaptureRequestUtils;
import za.co.rationalthinkers.unocapture.android.util.FileUtils;
import za.co.rationalthinkers.unocapture.android.util.Utils;
import za.co.rationalthinkers.unocapture.android.view.CustomTextureView;

import static za.co.rationalthinkers.unocapture.android.config.Constants.TAG_ERROR_DIALOG_FRAGMENT;
import static za.co.rationalthinkers.unocapture.android.util.Utils.chooseOptimalSize;
import static za.co.rationalthinkers.unocapture.android.util.Utils.chooseVideoSize;

public class VideoFragment extends Fragment implements ImageButton.OnClickListener {

    private OnVideoActionListener mListener;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private Context context;
    private Settings settings;

    private String mCameraId; //ID of the current {@link CameraDevice}.
    private ArrayList<String> mDeviceCameras;

    public CameraDevice mCameraDevice; //A reference to the opened {@link CameraDevice}.
    private VideoSurfaceTextureListener mSurfaceTextureListener = new VideoSurfaceTextureListener(this);
    private VideoStateCallback mStateCallback = new VideoStateCallback(this);

    public CameraCaptureSession mPreviewSession; //A {@link CameraCaptureSession } for camera preview.
    public CaptureRequest.Builder mPreviewRequestBuilder; //CaptureRequest.Builder for the camera preview
    public CaptureRequest mPreviewRequest; //CaptureRequest generated by {@link #mPreviewRequestBuilder}

    private Size mPreviewSize; //The {@link android.util.Size} of camera preview.
    private Size mVideoSize; //The {@link android.util.Size} of video recording.
    private MediaRecorder mMediaRecorder;

    private int mSensorOrientation; //Orientation of the camera sensor

    public Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    public Semaphore mCameraOpenCloseLock = new Semaphore(1); //A Semaphore to prevent the app from exiting before closing the camera.

    private boolean mFlashSupported = false;
    private boolean mIsRecordingVideo; //Whether the app is recording video now

    private String mNextVideoAbsolutePath;
    private VideoFilesObserver filesObserver;

    //UI References
    public CustomTextureView mTextureView;
    private ImageButton switchCameraView;
    private ImageButton cameraModeView;
    private ImageButton captureView;
    private ImageButton galleryView;
    private Chronometer recordedTimeView;

    public interface OnVideoActionListener {
        void onSettingsSelected(Object sender);
        void onGallerySelected(Object sender);
        void onCameraSelected();
        void onVideoCaptureStarted();
        void onVideoCaptureFinished(boolean status);
    }

    public VideoFragment() {
        // Required empty public constructor
    }

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getContext();
        settings = new Settings(context);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUI(view);
        setUpListeners();
        setUpFilesObserver();

        checkPermissions();
    }

    private void initUI(View view){
        mTextureView = view.findViewById(R.id.video_view);
        switchCameraView = view.findViewById(R.id.config_switch_camera);
        cameraModeView = view.findViewById(R.id.action_camera_mode);
        captureView = view.findViewById(R.id.action_capture);
        galleryView = view.findViewById(R.id.action_gallery);
        recordedTimeView = view.findViewById(R.id.video_recorded_time);
    }

    private void setUpListeners(){
        switchCameraView.setOnClickListener(this);
        cameraModeView.setOnClickListener(this);
        captureView.setOnClickListener(this);
        galleryView.setOnClickListener(this);
    }

    private void setUpFilesObserver(){
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();

        filesObserver = new VideoFilesObserver(this, path);
        filesObserver.startWatching();
    }

    private void setupTimer(boolean isRecording){
        recordedTimeView.setBase(SystemClock.elapsedRealtime());
        recordedTimeView.stop();

        if(isRecording){
            recordedTimeView.setVisibility(View.VISIBLE);
            recordedTimeView.start();
        }
        else{
            recordedTimeView.stop();
            recordedTimeView.setVisibility(View.GONE);
        }
    }

    public void updateLatestFile(){
        MediaFile file = FileUtils.getLatestMedia(context, true);

        Glide.with(context)
                .load(file.getUri())
                .into(galleryView);
    }

    private void checkPermissions(){
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Permissions.check(getContext(), permissions, null, null, new PermissionHandler() {
            @Override
            public void onGranted() {
                onRequiredPermissionsGranted();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                onRequiredPermissionsNotGranted();
            }
        });
    }

    private void onRequiredPermissionsGranted(){
        mDeviceCameras = Utils.getDeviceCameras(context);

        updateLatestFile();
    }

    private void onRequiredPermissionsNotGranted(){
        Toast.makeText(context, "Please enable the required permissions to continue", Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    /**
     * Starts a background thread and its Handler.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();

        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its Handler.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();

        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void reopenCamera() {
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        }
        else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    /**
     * Opens the camera specified by mCameraId.
     */
    public void openCamera(int width, int height) {
        Activity activity = getActivity();

        if(activity != null){
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
            Permissions.check(activity.getApplicationContext(), permissions, null, null, new PermissionHandler() {
                @Override
                public void onGranted() {
                    openCameraPermissionGranted(width, height);
                }

                @Override
                public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                    ErrorDialog.newInstance(getString(R.string.error_camera_permission_required))
                            .show(getChildFragmentManager(), TAG_ERROR_DIALOG_FRAGMENT);
                }
            });
        }

    }

    @SuppressWarnings({"MissingPermission"})
    private void openCameraPermissionGranted(int width, int height){
        Activity activity = getActivity();

        if(activity != null){
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

            setUpCameraOutputs(width, height);
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();

            try {
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }

                manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            }
            catch (CameraAccessException e) {
                // An NPE is thrown when the Camera2API is used but not supported on the device this code runs.
                ErrorDialog.newInstance(getString(R.string.error_camera_not_supported)).show(getChildFragmentManager(), TAG_ERROR_DIALOG_FRAGMENT);
            }
            catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }

    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        if(activity == null){
            return;
        }

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        try {
            if(TextUtils.isEmpty(mCameraId)){
                mCameraId = mDeviceCameras.get(0);
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);

            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

            // Choose the sizes for camera preview and video recording
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                //Cannot get available preview/video sizes
                throw new RuntimeException("Cannot get available preview/video sizes");
            }

            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }
            else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
        }
        catch (CameraAccessException e) {
            ErrorDialog.newInstance(getString(R.string.error_camera_access))
                    .show(getChildFragmentManager(), TAG_ERROR_DIALOG_FRAGMENT);
        }
        catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the device this code runs.
            ErrorDialog.newInstance(getString(R.string.error_camera_not_supported))
                    .show(getChildFragmentManager(), TAG_ERROR_DIALOG_FRAGMENT);
        }
    }

    /**
     * Configures the necessary Matrix transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in openCamera, or
     * until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    public void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            float scale = Math.max((float) viewHeight / mPreviewSize.getHeight(), (float) viewWidth / mPreviewSize.getWidth());
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());

            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }

        mTextureView.setTransform(matrix);
    }

    /**
     * Creates a new CameraCaptureSession for camera preview.
     * Start the camera preview.
     */
    public void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            closePreviewSession();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            if(texture != null){
                // We configure the size of default buffer to be the size of camera preview we want.
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                // This is the output Surface we need to start preview.
                Surface surface = new Surface(texture);

                // We set up a CaptureRequest.Builder with the output Surface.
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(surface);

                // Here, we create a CameraCaptureSession for camera preview.
                mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                        new VideoPreviewCaptureSessionListener(this),
                        mBackgroundHandler
                );
            }

        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {startPreview()} needs to be called in advance.
     */
    public void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }

        try {
            setUpCaptureRequestBuilder();
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();

            // Finally, we start displaying the camera preview.
            mPreviewRequest = mPreviewRequestBuilder.build();
            mPreviewSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder() {
        CaptureRequestUtils.setControlMode(mPreviewRequestBuilder, CameraMetadata.CONTROL_MODE_AUTO);

        if(mFlashSupported){
            CaptureRequestUtils.setFlashMode(mPreviewRequestBuilder, settings.getFlashMode());
        }
    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }

        //TODO: Video quality from settings
        mNextVideoAbsolutePath = Utils.getOutputMediaFileUri(MediaType.VIDEO).getPath();
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P); //Record in HD

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }

        mMediaRecorder.prepare();
    }

    private void toggleRecordingButton(boolean isRecordingVideo){
        if(captureView == null){
            return;
        }

        if(isRecordingVideo){
            captureView.setImageResource(R.drawable.ic_stop_white_24dp);
        }
        else{
            captureView.setImageResource(R.drawable.ic_capture_video_button);
        }

    }

    public void onNewOrientation(int newOrientation){

        if(newOrientation == OrientationEventListener.ORIENTATION_UNKNOWN ){
            return;
        }

        float rotation = 0f;
        if(newOrientation == 0){
            rotation = 0f;
        }
        else if(newOrientation == 90){
            rotation = 90f;
        }
        else if(newOrientation == 180){
            rotation = 180f;
        }
        else if(newOrientation == 270){
            rotation = 270f;
        }

        Utils.setViewRotation(switchCameraView, rotation);
        Utils.setViewRotation(cameraModeView, rotation);
        Utils.setViewRotation(galleryView, rotation);
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();

            closePreviewSession();
            closeCameraDevice();
            closeMediaRecorder();

        }
        catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        }
        finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void closeCameraDevice() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void closeMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    /**
     *
     * OnButtonClick Events
     *
     */

    public void switchCamera() {
        int currentCameraIndex = mDeviceCameras.indexOf(mCameraId);

        //Cycle the cameras in a circular manner
        int i = 1;
        int index = (i + currentCameraIndex) % mDeviceCameras.size();

        mCameraId = mDeviceCameras.get(index);
        closeCamera();
        reopenCamera();
    }

    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            closePreviewSession();
            setUpMediaRecorder();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            if(texture != null){
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                List<Surface> surfaces = new ArrayList<>();

                // Set up Surface for the camera preview
                Surface previewSurface = new Surface(texture);
                surfaces.add(previewSurface);
                mPreviewRequestBuilder.addTarget(previewSurface);

                // Set up Surface for the MediaRecorder
                Surface recorderSurface = mMediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                mPreviewRequestBuilder.addTarget(recorderSurface);

                // Start a capture session
                // Once the session starts, we can update the UI and start recording
                mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        mPreviewSession = cameraCaptureSession;
                        updatePreview();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIsRecordingVideo = true;

                                MediaActionSound sound = new MediaActionSound();
                                sound.play(MediaActionSound.START_VIDEO_RECORDING);

                                toggleRecordingButton(mIsRecordingVideo);
                                setupTimer(mIsRecordingVideo);

                                mMediaRecorder.start(); // Start recording
                            }
                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Activity activity = getActivity();
                        if (null != activity) {
                            Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, mBackgroundHandler);
            }

        }
        catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    private void stopRecordingVideo() {
        mIsRecordingVideo = false;
        toggleRecordingButton(mIsRecordingVideo);
        setupTimer(mIsRecordingVideo);

        try{
            //mPreviewSession.stopRepeating();
            //mPreviewSession.abortCaptures();

            MediaActionSound sound = new MediaActionSound();
            sound.play(MediaActionSound.STOP_VIDEO_RECORDING);

            // Stop recording
            mMediaRecorder.stop();
            mMediaRecorder.reset();

            onVideoSaved();
        }
        /*catch (CameraAccessException e) {
            e.printStackTrace();
        }*/
        catch (IllegalStateException i){
            //MediaRecorder not started yet
        }
        catch (RuntimeException r){
            //No file saved. delete file
        }

        mNextVideoAbsolutePath = null;
        startPreview();
    }

    public void onVideoSaved(){
        FileUtils.announceUri(context, Uri.parse(mNextVideoAbsolutePath), false, true);

        //TODO: Convert mNextVideoAbsolutePath to file, then send broadcast
        //FileUtils.broadcastNewFile(context, mFile, false, true);
        Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show();
    }

    private void goToGalleryActivity(){
        if(mListener != null){
            mListener.onGallerySelected(this);
        }
    }

    private void goToSettingsFragment(){
        if(mListener != null){
            mListener.onSettingsSelected(this);
        }
    }

    private void goToCameraFragment(){
        if(mListener != null){
            mListener.onCameraSelected();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.config_settings:
                goToSettingsFragment();
                break;

            case R.id.config_switch_camera:
                switchCamera();
                break;

            case R.id.action_camera_mode:
                goToCameraFragment();
                break;

            case R.id.action_capture:
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                }
                else {
                    startRecordingVideo();
                }
                break;

            case R.id.action_gallery:
                goToGalleryActivity();
                break;

        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnVideoActionListener) {
            mListener = (OnVideoActionListener) context;
        }
        else {
            throw new RuntimeException(context.toString() + " must implement OnVideoActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        startBackgroundThread();
        reopenCamera();

        super.onResume();
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();

        super.onPause();
    }

}