package za.co.rationalthinkers.unocapture.android.config;

import android.content.Context;
import android.content.SharedPreferences;

import za.co.rationalthinkers.unocapture.android.BuildConfig;

public class Settings {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public Settings(Context context){
        this.context = context;
        sharedPreferences = context.getSharedPreferences(Constants.SettingsFile, Context.MODE_PRIVATE);
    }

    //Getters

    public boolean isFirstRun(){
        return sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_FIRST_RUN, true);
    }

    public int getLastUpdatedVersion(){
        return sharedPreferences.getInt(Constants.SHARED_PREFERENCES_CURRENT_VERSION, 0);
    }

    public int getCurrentVersion(){
        return BuildConfig.VERSION_CODE;
    }

    public boolean isFaceDetection(){
        return sharedPreferences.getBoolean(Constants.SHARED_PREFERENCES_FACE_DETECTION, true);
    }

    public int getFlashMode(){
        return sharedPreferences.getInt(Constants.SHARED_PREFERENCES_FLASH_MODE, 0);
    }

    public int getSceneMode(){
        return sharedPreferences.getInt(Constants.SHARED_PREFERENCES_SCENE_MODE, 0);
    }

    //Setters

    public void setFirstRun(boolean opened){
        editor = sharedPreferences.edit();
        editor.putBoolean(Constants.SHARED_PREFERENCES_FIRST_RUN, opened);
        editor.apply();
    }

    public void setLastUpdatedVersion(int version){
        editor = sharedPreferences.edit();
        editor.putInt(Constants.SHARED_PREFERENCES_CURRENT_VERSION, version);
        editor.apply();
    }

    public void setFaceDetection(boolean enabled){
        editor = sharedPreferences.edit();
        editor.putBoolean(Constants.SHARED_PREFERENCES_FACE_DETECTION, enabled);
        editor.apply();
    }

    public void setFlashMode(int mode){
        editor = sharedPreferences.edit();
        editor.putInt(Constants.SHARED_PREFERENCES_FLASH_MODE, mode);
        editor.apply();
    }

    public void setSceneMode(int mode){
        editor = sharedPreferences.edit();
        editor.putInt(Constants.SHARED_PREFERENCES_SCENE_MODE, mode);
        editor.apply();
    }

}
