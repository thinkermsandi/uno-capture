package za.co.rationalthinkers.unocapture.android.listener;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import java.lang.ref.WeakReference;

import za.co.rationalthinkers.unocapture.android.fragment.VideoFragment;

public class VideoSurfaceTextureListener implements TextureView.SurfaceTextureListener {

    private WeakReference<VideoFragment> _fragment;

    public VideoSurfaceTextureListener(VideoFragment fragment){
        _fragment = new WeakReference<>(fragment);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        VideoFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.openCamera(width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        VideoFragment fragment = _fragment.get();

        if(fragment !=  null){
            fragment.configureTransform(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

}
