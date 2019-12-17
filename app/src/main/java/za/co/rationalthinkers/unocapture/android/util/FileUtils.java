package za.co.rationalthinkers.unocapture.android.util;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;
import android.support.v4.content.ContextCompat;

import java.io.File;

import za.co.rationalthinkers.unocapture.android.model.MediaFile;

public class FileUtils {

    /** Sends the intents to announce the new file to other Android applications.
     *  E.g., cloud storage applications like OwnCloud use this to listen for new photos/videos to automatically upload.
     *  Note that on Android 7 onwards, these broadcasts are deprecated
     */
    public static void announceUri(Context context, Uri uri, boolean isImage, boolean isVideo) {

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
            //broadcasts deprecated on Android 7 onwards, so don't send them
        }
        else if(isImage) {
            // note, we reference the string directly rather than via Camera.ACTION_NEW_PICTURE,
            // as the latter class is now deprecated - but we still need to broadcast the string for other apps
            context.sendBroadcast(new Intent( "android.hardware.action.NEW_PICTURE" , uri));
            // for compatibility with some apps - apparently this is what used to be broadcast on Android?
            context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
        }
        else if(isVideo) {
            context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));
        }
    }

    /**
     *  Sends a "broadcast" for the new file.
     *  This is necessary so that Android recognises the new file without needing a reboot:
     *  - So that they show up when connected to a PC using MTP.
     *  - For JPEGs, so that they show up in gallery applications.
     *  - This also calls announceUri() on the resultant Uri for the new file.
     *  - Note this should also be called after deleting a file.
     */
    public static void broadcastNewFile(Context context, File file, boolean isImage, boolean isVideo) {

        // note that the new method means that the new folder shows up as a file when connected to a PC via MTP (at least tested on Windows 8)
        if( file.isDirectory() ) {
            //We don't actually need to broadcast anything,
            // the folder and contents appear straight away (both in Gallery on device, and on a PC when connecting via MTP)
        }
        else {
            // Using MediaScannerConnection.scanFile() seems to be preferred over sending an intent

            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            announceUri(context, Uri.fromFile(file), isImage, isVideo);

            /*MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            announceUri(context, uri, isImage, isVideo);

                            Activity activity = (Activity) context;
                            String action = activity.getIntent().getAction();
                            if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) ) {
                                Intent output = new Intent();
                                output.setData(uri);
                                activity.setResult(Activity.RESULT_OK, output);
                                activity.finish();
                            }
                        }
                    }
            );*/
        }
    }

    /**
     * Wrapper for broadcastFile, when we only have a Uri (e.g., for SAF)
     */
    public File broadcastUri(Context context, Uri uri, boolean isImage, boolean isVideo) {
        announceUri(context, uri, isImage, isVideo);
        return null;
    }

    public static MediaFile getLatestMedia(Context context, boolean video) {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            //We don't have READ_EXTERNAL_STORAGE permission
            return null;
        }

        MediaFile media = null;
        ContentResolver contentResolver = context.getContentResolver();

        Uri baseUri = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String [] projection = video ? new String[] {VideoColumns._ID, VideoColumns.DATE_TAKEN, VideoColumns.DATA} :
                new String[] {ImageColumns._ID, ImageColumns.DATE_TAKEN, ImageColumns.DATA};
        String selection = video ? "" : ImageColumns.MIME_TYPE + "='image/jpeg' OR " + ImageColumns.MIME_TYPE + "='image/webp' OR " +
                ImageColumns.MIME_TYPE + "='image/png'";
        String order = video ? VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC" :
                ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";

        Cursor cursor = contentResolver.query(baseUri, projection, selection, null, order);
        if(cursor != null){
            try{
                if(cursor.moveToFirst()){
                    long id = cursor.getLong(0);
                    long date = cursor.getLong(1);
                    String path = cursor.getString(2);
                    Uri uri = ContentUris.withAppendedId(baseUri, id);

                    media = new MediaFile(id, video, path, uri, date);
                }
            }
            catch (Exception e){

            }
            finally {
                cursor.close();
            }
        }

        return media;
    }

}
