package za.co.rationalthinkers.unocapture.android.observer;

import android.os.FileObserver;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.CameraFragment;

public class ImageFilesObserver extends FileObserver {

    private WeakReference<CameraFragment> _fragment;

    public ImageFilesObserver(CameraFragment fragment, String path) {
        super(path, FileObserver.CREATE);
        _fragment = new WeakReference<>(fragment);
    }

    @Override
    public void onEvent(int event, String path) {
        if(path != null){

            CameraFragment fragment = _fragment.get();
            if(fragment != null){
                fragment.updateLatestFile();
            }

        }
    }
}
