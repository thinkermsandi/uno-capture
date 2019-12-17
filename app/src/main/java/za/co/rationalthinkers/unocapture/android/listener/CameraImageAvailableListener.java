package za.co.rationalthinkers.unocapture.android.listener;

import android.media.ImageReader;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.CameraFragment;
import za.co.rationalthinkers.unocapture.android.util.ImageSaver;

/**
 * This a callback object for the ImageReader.
 * "onImageAvailable" will be called when a still image is ready to be saved.
 */
public class CameraImageAvailableListener implements ImageReader.OnImageAvailableListener {

    private WeakReference<CameraFragment> _fragment;

    public CameraImageAvailableListener(CameraFragment fragment){
        _fragment = new WeakReference<>(fragment);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {

        CameraFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), fragment.mFile));
        }

    }

}
