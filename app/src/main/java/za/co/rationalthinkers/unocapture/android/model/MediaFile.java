package za.co.rationalthinkers.unocapture.android.model;

import android.net.Uri;

public class MediaFile {

    private long id;
    private boolean video;
    private Uri uri;
    private long date;
    private String path;

    public MediaFile(long id, boolean video, String path, Uri uri, long date) {
        this.id = id;
        this.video = video;
        this.path = path;
        this.uri = uri;
        this.date = date;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setVideo(boolean video) {
        this.video = video;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public boolean isVideo() {
        return video;
    }

    public String getPath() {
        return path;
    }

    public Uri getUri() {
        return uri;
    }

    public long getDate() {
        return date;
    }

}
