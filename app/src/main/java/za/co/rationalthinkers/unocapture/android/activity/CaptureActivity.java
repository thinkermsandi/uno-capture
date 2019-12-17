package za.co.rationalthinkers.unocapture.android.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;
import java.util.List;

import za.co.rationalthinkers.unocapture.android.R;
import za.co.rationalthinkers.unocapture.android.fragment.CameraFragment;
import za.co.rationalthinkers.unocapture.android.fragment.SettingsFragment;
import za.co.rationalthinkers.unocapture.android.fragment.VideoFragment;
import za.co.rationalthinkers.unocapture.android.listener.ActivityOrientationEventListener;
import za.co.rationalthinkers.unocapture.android.model.MediaFile;
import za.co.rationalthinkers.unocapture.android.util.FileUtils;
import za.co.rationalthinkers.unocapture.android.util.Utils;

import static za.co.rationalthinkers.unocapture.android.config.Constants.TAG_FRAGMENT_SETTINGS;

public class CaptureActivity extends AppCompatActivity
        implements CameraFragment.OnCameraActionListener,
        VideoFragment.OnVideoActionListener {

    Context context;
    FragmentManager fragmentManager;
    ActivityOrientationEventListener orientationEventListener;

    //UI Reference
    FrameLayout previewLayout;
    FrameLayout flashLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        context = getApplicationContext();
        fragmentManager = getSupportFragmentManager();
        orientationEventListener = new ActivityOrientationEventListener(this);

        initUI();
        checkCameraAvailability();
        checkPermissions();
    }

    private void initUI(){
        previewLayout = findViewById(R.id.capture_fragment);
        flashLayout = findViewById(R.id.capture_flash);
    }

    private void checkCameraAvailability(){
        if(!Utils.checkCameraHardware(context)){
            Toast.makeText(context, "There is no Camera available on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkPermissions(){
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Permissions.check(this, permissions, null, null, new PermissionHandler() {
            @Override
            public void onGranted() {
                goToCameraFragment();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                Toast.makeText(context, "Please enable the required permissions to continue", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    public void flash(){
        flashLayout.setVisibility(View.VISIBLE);

        AlphaAnimation fade = new AlphaAnimation(1, 0);
        fade.setDuration(900);
        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation anim) {
                flashLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        flashLayout.startAnimation(fade);
    }

    public Fragment getCurrentFragment(){
        List<Fragment> fragments = fragmentManager.getFragments();
        for(Fragment fragment : fragments){
            if(fragment != null && fragment.isVisible())
                return fragment;
        }

        return null;
    }

    private void goToGallery(){
        MediaFile file = null;

        Fragment currentFragment = getCurrentFragment();

        if(currentFragment instanceof CameraFragment){
            file = FileUtils.getLatestMedia(context, false);
        }
        else if(currentFragment instanceof VideoFragment){
            file = FileUtils.getLatestMedia(context, true);
        }

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);

        //intent.setType("image/*");
        if(file != null){
            intent.setDataAndType(file.getUri(), "image/*");
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void goToCameraFragment(){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.capture_fragment, CameraFragment.newInstance());
        fragmentTransaction.commit();
    }

    private void goToVideoFragment(){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.capture_fragment, VideoFragment.newInstance());
        fragmentTransaction.commit();
    }

    private void goToSettingsFragment(){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.capture_fragment, SettingsFragment.newInstance());
        fragmentTransaction.addToBackStack(TAG_FRAGMENT_SETTINGS);
        fragmentTransaction.commit();
    }


    @Override
    public void onSettingsSelected(Object sender) {
        goToSettingsFragment();
    }

    @Override
    public void onGallerySelected(Object sender) {
        goToGallery();
    }

    @Override
    public void onCameraSelected() {
        goToCameraFragment();
    }

    @Override
    public void onVideoSelected() {
        goToVideoFragment();
    }

    @Override
    public void onImageCaptureStarted() {

    }

    @Override
    public void onImageCaptureFinished(boolean status) {
        flash();
    }

    @Override
    public void onVideoCaptureStarted() {

    }

    @Override
    public void onVideoCaptureFinished(boolean status) {
        flash();
    }

    public void onNewOrientation(int orientation){

        if( orientation == OrientationEventListener.ORIENTATION_UNKNOWN ){
            return;
        }

        Fragment currentFragment = getCurrentFragment();

        if(currentFragment instanceof CameraFragment){
            ((CameraFragment) currentFragment).onNewOrientation(orientation);
        }
        else if(currentFragment instanceof VideoFragment){
            ((VideoFragment) currentFragment).onNewOrientation(orientation);
        }

    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0){
            fragmentManager.popBackStackImmediate();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(orientationEventListener.canDetectOrientation()){
            orientationEventListener.disable();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(orientationEventListener.canDetectOrientation()){
            orientationEventListener.enable();
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
}