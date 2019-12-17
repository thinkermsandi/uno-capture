package za.co.rationalthinkers.unocapture.android.config;

import za.co.rationalthinkers.unocapture.android.BuildConfig;

public class Constants {

    public static String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static String APP_NAME = "UnoCapture";

    public static String SettingsFile = PACKAGE_NAME + ".USER_SETTINGS";

    public static String SHARED_PREFERENCES_FIRST_RUN = "first_run";
    public static String SHARED_PREFERENCES_CURRENT_VERSION = "current_version";
    public static String SHARED_PREFERENCES_FACE_DETECTION = "face_detection";
    public static String SHARED_PREFERENCES_FLASH_MODE = "flash_mode";
    public static String SHARED_PREFERENCES_SCENE_MODE = "scene_mode";

    public static String TAG_ERROR_DIALOG_FRAGMENT = "error_fragment_dialog";
    public static String TAG_FRAGMENT_CAMERA = "camera_fragment";
    public static String TAG_FRAGMENT_VIDEO = "video_fragment";
    public static String TAG_FRAGMENT_SETTINGS = "settings_fragment";

    public static final int STATE_PREVIEW = 0; //Showing camera preview.
    public static final int STATE_WAITING_LOCK = 1; //Waiting for the focus to be locked.
    public static final int STATE_WAITING_PRECAPTURE = 2; //Waiting for the exposure to be precapture state.
    public static final int STATE_WAITING_NON_PRECAPTURE = 3; //Waiting for the exposure state to be something other than precapture.
    public static final int STATE_PICTURE_TAKEN = 4; //Picture was taken.

}
