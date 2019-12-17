package za.co.rationalthinkers.unocapture.android.observer;

import android.os.FileObserver;
import android.os.Handler;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.VideoFragment;

public class VideoFilesObserver extends FileObserver {

    private WeakReference<VideoFragment> _fragment;

    public VideoFilesObserver(VideoFragment fragment, String path) {
        super(path, FileObserver.CREATE);
        _fragment = new WeakReference<>(fragment);
    }

    @Override
    public void onEvent(int event, String path) {
        if(path != null){

            VideoFragment fragment = _fragment.get();
            if(fragment != null){
                fragment.updateLatestFile();
            }

        }
    }

}
