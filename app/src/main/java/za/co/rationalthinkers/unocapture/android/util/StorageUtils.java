package za.co.rationalthinkers.unocapture.android.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//import android.content.ContentValues;
//import android.location.Location;

/** Provides access to the filesystem. Supports both standard and Storage
 *  Access Framework.
 */
public class StorageUtils {
    private static final String TAG = "StorageUtils";

    static final int MEDIA_TYPE_IMAGE = 1;
    static final int MEDIA_TYPE_VIDEO = 2;
    static final int MEDIA_TYPE_PREFS = 3;
    static final int MEDIA_TYPE_GYRO_INFO = 4;

    private final Context context;
    private Uri last_media_scanned;

    private final static File base_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

    StorageUtils(Context context) {
        this.context = context;
    }

    Uri getLastMediaScanned() {
        return last_media_scanned;
    }

    void clearLastMediaScanned() {
        last_media_scanned = null;
    }

    /** Sends the intents to announce the new file to other Android applications.
     *  E.g., cloud storage applications like OwnCloud use this to listen for new photos/videos to automatically upload.
     *  Note that on Android 7 onwards, these broadcasts are deprecated and won't have any effect - see:
     *  https://developer.android.com/reference/android/hardware/Camera.html#ACTION_NEW_PICTURE
     *  Listeners like OwnCloud should instead be using
     *  https://developer.android.com/reference/android/app/job/JobInfo.Builder.html#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)
     *  See https://github.com/owncloud/android/issues/1675 for OwnCloud's discussion on this.
     */
    void announceUri(Uri uri, boolean is_new_picture, boolean is_new_video) {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
            //Log.d(TAG, "broadcasts deprecated on Android 7 onwards, so don't send them");
            // see note above; the intents won't be delivered, so might as well save the trouble of trying to send them
        }
        else if( is_new_picture ) {
            // note, we reference the string directly rather than via Camera.ACTION_NEW_PICTURE, as the latter class is now deprecated - but we still need to broadcast the string for other apps
            context.sendBroadcast(new Intent( "android.hardware.action.NEW_PICTURE" , uri));
            // for compatibility with some apps - apparently this is what used to be broadcast on Android?
            context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
        }
        else if( is_new_video ) {
            context.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", uri));
        }
    }

    /** Sends a "broadcast" for the new file.
     *  This is necessary so that Android recognises the new file without needing a reboot:
     *  - So that they show up when connected to a PC using MTP.
     *  - For JPEGs, so that they show up in gallery applications.
     *  - This also calls announceUri() on the resultant Uri for the new file.
     *  - Note this should also be called after deleting a file.
     *  - Note that for DNG files, MediaScannerConnection.scanFile() doesn't result in the files being shown in gallery applications.
     *    This may well be intentional, since most gallery applications won't read DNG files anyway.
     *    But it's still important to call this function for DNGs, so that they show up on MTP.
     */
    public void broadcastFile(final File file, final boolean is_new_picture, final boolean is_new_video, final boolean set_last_scanned) {
        // note that the new method means that the new folder shows up as a file when connected to a PC via MTP (at least tested on Windows 8)
        if( file.isDirectory() ) {
            //this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
            // ACTION_MEDIA_MOUNTED no longer allowed on Android 4.4! Gives: SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.MEDIA_MOUNTED
            // note that we don't actually need to broadcast anything, the folder and contents appear straight away (both in Gallery on device, and on a PC when connecting via MTP)
            // also note that we definitely don't want to broadcast ACTION_MEDIA_SCANNER_SCAN_FILE or use scanFile() for folders, as this means the folder shows up as a file on a PC via MTP (and isn't fixed by rebooting!)
        }
        else {
            // both of these work fine, but using MediaScannerConnection.scanFile() seems to be preferred over sending an intent
            //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

            MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            if( set_last_scanned ) {
                                last_media_scanned = uri;
                            }
                            announceUri(uri, is_new_picture, is_new_video);
                            //applicationInterface.scannedFile(file, uri);

                            // it seems caller apps seem to prefer the content:// Uri rather than one based on a File
                            // update for Android 7: seems that passing file uris is now restricted anyway, see https://code.google.com/p/android/issues/detail?id=203555
                            Activity activity = (Activity)context;
                            String action = activity.getIntent().getAction();
                            if( MediaStore.ACTION_VIDEO_CAPTURE.equals(action) ) {
                                Intent output = new Intent();
                                output.setData(uri);
                                activity.setResult(Activity.RESULT_OK, output);
                                activity.finish();
                            }
                        }
                    }
            );
        }
    }

    /** Wrapper for broadcastFile, when we only have a Uri (e.g., for SAF)
     */
    public File broadcastUri(final Uri uri, final boolean is_new_picture, final boolean is_new_video, final boolean set_last_scanned) {
        File real_file = getFileFromDocumentUriSAF(uri, false);
        if( real_file != null ) {
            broadcastFile(real_file, is_new_picture, is_new_video, set_last_scanned);
            return real_file;
        }
        else {
            //Log.d(TAG, "announce SAF uri");
            announceUri(uri, is_new_picture, is_new_video);
        }
        return null;
    }

    boolean isUsingSAF() {
        // check Android version just to be safe
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            return false;
        }

        return false;
    }

    // only valid if !isUsingSAF()
    String getSaveLocation() {
        return "UnoCapture";
    }

    // only valid if isUsingSAF()
    String getSaveLocationSAF() {
        return "";
    }

    // only valid if isUsingSAF()
    private Uri getTreeUriSAF() {
        String folder_name = getSaveLocationSAF();
        return Uri.parse(folder_name);
    }

    File getSettingsFolder() {
        return new File(context.getExternalFilesDir(null), "backups");
    }

    // valid if whether or not isUsingSAF()
    // but note that if isUsingSAF(), this may return null - it can't be assumed that there is a File corresponding to the SAF Uri
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    File getImageFolder() {
        File file;
        if( isUsingSAF() ) {
            Uri uri = getTreeUriSAF();
            file = getFileFromDocumentUriSAF(uri, true);
        }
        else {
            String folder_name = getSaveLocation();
            file = getImageFolder(folder_name);
        }
        return file;
    }

    public static File getBaseFolder() {
        return base_folder;
    }

    // only valid if !isUsingSAF()
    private static File getImageFolder(String folder_name) {
        File file;
        if( folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length()-1 ) {
            // ignore final '/' character
            folder_name = folder_name.substring(0, folder_name.length()-1);
        }
        //if( folder_name.contains("/") ) {
        if( folder_name.startsWith("/") ) {
            file = new File(folder_name);
        }
        else {
            file = new File(getBaseFolder(), folder_name);
        }
        return file;
    }

    // only valid if isUsingSAF()
    // This function should only be used as a last resort - we shouldn't generally assume that a Uri represents an actual File, and instead.
    // However this is needed for a workaround to the fact that deleting a document file doesn't remove it from MediaStore.
    // See:
    // http://stackoverflow.com/questions/21605493/storage-access-framework-does-not-update-mediascanner-mtp
    // http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework/
    // only valid if isUsingSAF()
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public File getFileFromDocumentUriSAF(Uri uri, boolean is_folder) {
        String authority = uri.getAuthority();
        File file = null;
        if( "com.android.externalstorage.documents".equals(authority) ) {
            final String id = is_folder ? DocumentsContract.getTreeDocumentId(uri) : DocumentsContract.getDocumentId(uri);
            String [] split = id.split(":");
            if( split.length >= 2 ) {
                String type = split[0];
                String path = split[1];
				/*if( MyDebug.LOG ) {
					Log.d(TAG, "type: " + type);
					Log.d(TAG, "path: " + path);
				}*/
                File [] storagePoints = new File("/storage").listFiles();

                if( "primary".equalsIgnoreCase(type) ) {
                    final File externalStorage = Environment.getExternalStorageDirectory();
                    file = new File(externalStorage, path);
                }
                for(int i=0;storagePoints != null && i<storagePoints.length && file==null;i++) {
                    File externalFile = new File(storagePoints[i], path);
                    if( externalFile.exists() ) {
                        file = externalFile;
                    }
                }
                if( file == null ) {
                    // just in case?
                    file = new File(path);
                }
            }
        }
        else if( "com.android.providers.downloads.documents".equals(authority) ) {
            if( !is_folder ) {
                final String id = DocumentsContract.getDocumentId(uri);
                if( id.startsWith("raw:") ) {
                    // unclear if this is needed for Open Camera, but on Vibrance HDR
                    // on some devices (at least on a Chromebook), I've had reports of id being of the form
                    // "raw:/storage/emulated/0/Download/..."
                    String filename = id.replaceFirst("raw:", "");
                    file = new File(filename);
                }
                else {
                    try {
                        final Uri contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                        String filename = getDataColumn(contentUri, null, null);
                        if( filename != null )
                            file = new File(filename);
                    }
                    catch(NumberFormatException e) {
                        // have had crashes from Google Play from Long.parseLong(id)
                        Log.e(TAG,"failed to parse id: " + id);
                        e.printStackTrace();
                    }
                }
            }
            else {
                // This codepath can be reproduced by enabling SAF and selecting Downloads.
                // DocumentsContract.getDocumentId() throws IllegalArgumentException for
                // this (content://com.android.providers.downloads.documents/tree/downloads).
                // If we use DocumentsContract.getTreeDocumentId() for folders, it returns
                // "downloads" - not clear how to parse this!
            }
        }
        else if( "com.android.providers.media.documents".equals(authority) ) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            switch (type) {
                case "image":
                    contentUri = Images.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "video":
                    contentUri = Video.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "audio":
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    break;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[] {
                    split[1]
            };

            String filename = getDataColumn(contentUri, selection, selectionArgs);
            if( filename != null )
                file = new File(filename);
        }

        return file;
    }

    private String getDataColumn(Uri uri, String selection, String [] selectionArgs) {
        final String column = "_data";
        final String[] projection = {
                column
        };

        Cursor cursor = null;
        try {
            cursor = this.context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private String createMediaFilename(int type, String suffix, int count, String extension, Date current_date) {
        String index = "";
        if( count > 0 ) {
            index = "_" + count; // try to find a unique filename
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(current_date);
        String mediaFilename;

        switch (type) {
            case MEDIA_TYPE_GYRO_INFO: // gyro info files have same name as the photo (but different extension)

            case MEDIA_TYPE_IMAGE: {
                mediaFilename = "IMG_" + timeStamp + suffix + index + extension;
                break;
            }

            case MEDIA_TYPE_VIDEO: {
                mediaFilename = "VID_" + timeStamp + suffix + index + extension;
                break;
            }

            case MEDIA_TYPE_PREFS: {
                // good to use a prefix that sorts before IMG_ and VID_: annoyingly when using SAF, it doesn't seem possible to
                // only show the xml files, and it always defaults to sorting alphabetically...
                String prefix = "BACKUP_OC_";
                mediaFilename = prefix + timeStamp + suffix + index + extension;
                break;
            }

            default:
                // throw exception as this is a programming error
                throw new RuntimeException();

        }

        return mediaFilename;
    }

    // only valid if !isUsingSAF()
    File createOutputMediaFile(int type, String suffix, String extension, Date current_date) throws IOException {
        File mediaStorageDir = getImageFolder();
        return createOutputMediaFile(mediaStorageDir, type, suffix, extension, current_date);
    }

    /** Create the folder if it does not exist.
     */
    void createFolderIfRequired(File folder) throws IOException {
        if( !folder.exists() ) {
            if( !folder.mkdirs() ) {
                //Log.e(TAG, "failed to create directory");
                throw new IOException();
            }

            broadcastFile(folder, false, false, false);
        }
    }

    // only valid if !isUsingSAF()
    @SuppressLint("SimpleDateFormat")
    File createOutputMediaFile(File mediaStorageDir, int type, String suffix, String extension, Date current_date) throws IOException {
        createFolderIfRequired(mediaStorageDir);

        // Create a media file name
        File mediaFile = null;
        for(int count = 0; count < 100; count++) {
        	/*final boolean use_burst_folder = true;
        	if( use_burst_folder ) {
				String burstFolderName = createMediaFilename(type, "", count, "", current_date);
				File burstFolder = new File(mediaStorageDir.getPath() + File.separator + burstFolderName);
				if( !burstFolder.exists() ) {
					if( !burstFolder.mkdirs() ) {
						if( MyDebug.LOG )
							Log.e(TAG, "failed to create burst sub-directory");
						throw new IOException();
					}
					broadcastFile(burstFolder, false, false, false);
				}

				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
				String prefix = sharedPreferences.getString(PreferenceKeys.getSavePhotoPrefixPreferenceKey(), "IMG_");
				//String mediaFilename = prefix + suffix + "." + extension;
				String suffix_alt = suffix.substring(1);
				String mediaFilename = suffix_alt + prefix + suffix_alt + "BURST" + "." + extension;
				mediaFile = new File(burstFolder.getPath() + File.separator + mediaFilename);
			}
			else*/ {
                String mediaFilename = createMediaFilename(type, suffix, count, "." + extension, current_date);
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + mediaFilename);
            }

            if( !mediaFile.exists() ) {
                break;
            }
        }

        if( mediaFile == null )
            throw new IOException();
        return mediaFile;
    }

    // only valid if isUsingSAF()
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Uri createOutputFileSAF(String filename, String mimeType) throws IOException {
        try {
            Uri treeUri = getTreeUriSAF();
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            // note that DocumentsContract.createDocument will automatically append to the filename if it already exists
            Uri fileUri = DocumentsContract.createDocument(context.getContentResolver(), docUri, mimeType, filename);
            /*if( true )
				throw new SecurityException(); // test*/
            if( fileUri == null )
                throw new IOException();
            return fileUri;
        }
        catch(IllegalArgumentException e) {
            // DocumentsContract.getTreeDocumentId throws this if URI is invalid
            e.printStackTrace();
            throw new IOException();
        }
        catch(IllegalStateException e) {
            // Have reports of this from Google Play for DocumentsContract.createDocument - better to fail gracefully and tell user rather than crash!
            e.printStackTrace();
            throw new IOException();
        }
        catch(SecurityException e) {
            // Have reports of this from Google Play - better to fail gracefully and tell user rather than crash!
            e.printStackTrace();
            throw new IOException();
        }
    }

    // only valid if isUsingSAF()
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Uri createOutputMediaFileSAF(int type, String suffix, String extension, Date current_date) throws IOException {
        String mimeType;
        switch (type) {

            case MEDIA_TYPE_IMAGE:
                switch (extension) {
                    case "dng":
                        mimeType = "image/dng";
                        //mimeType = "image/x-adobe-dng";
                        break;
                    case "webp":
                        mimeType = "image/webp";
                        break;
                    case "png":
                        mimeType = "image/png";
                        break;
                    default:
                        mimeType = "image/jpeg";
                        break;
                }
                break;

            case MEDIA_TYPE_VIDEO:
                switch( extension ) {
                    case "3gp":
                        mimeType = "video/3gpp";
                        break;
                    case "webm":
                        mimeType = "video/webm";
                        break;
                    default:
                        mimeType = "video/mp4";
                        break;
                }
                break;

            case MEDIA_TYPE_PREFS:
                mimeType = "text/xml";
                break;

            case MEDIA_TYPE_GYRO_INFO:
                mimeType = "text/xml";
                break;

            default:
                // throw exception as this is a programming error
                throw new RuntimeException();
        }

        // note that DocumentsContract.createDocument will automatically append to the filename if it already exists
        String mediaFilename = createMediaFilename(type, suffix, 0, "." + extension, current_date);
        return createOutputFileSAF(mediaFilename, mimeType);
    }

    static class Media {
        final long id;
        final boolean video;
        final Uri uri;
        final long date;
        final int orientation;
        final String path;

        Media(long id, boolean video, Uri uri, long date, int orientation, String path) {
            this.id = id;
            this.video = video;
            this.uri = uri;
            this.date = date;
            this.orientation = orientation;
            this.path = path;
        }
    }

    private Media getLatestMedia(boolean video) {
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            // needed for Android 6, in case users deny storage permission, otherwise we get java.lang.SecurityException from ContentResolver.query()
            // see https://developer.android.com/training/permissions/requesting.html
            // we now request storage permission before opening the camera, but keep this here just in case
            // we restrict check to Android 6 or later just in case, see note in LocationSupplier.setupLocationListener()
            return null;
        }

        Media media = null;
        Uri baseUri = video ? Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final int column_id_c = 0;
        final int column_date_taken_c = 1;
        final int column_data_c = 2; // full path and filename, including extension
        final int column_orientation_c = 3; // for images only
        String [] projection = video ? new String[] {VideoColumns._ID, VideoColumns.DATE_TAKEN, VideoColumns.DATA} : new String[] {ImageColumns._ID, ImageColumns.DATE_TAKEN, ImageColumns.DATA, ImageColumns.ORIENTATION};
        // for images, we need to search for JPEG/etc and RAW, to support RAW only mode (even if we're not currently in that mode, it may be that previously the user did take photos in RAW only mode)
        String selection = video ? "" : ImageColumns.MIME_TYPE + "='image/jpeg' OR " +
                ImageColumns.MIME_TYPE + "='image/webp' OR " +
                ImageColumns.MIME_TYPE + "='image/png' OR " +
                ImageColumns.MIME_TYPE + "='image/x-adobe-dng'";
        String order = video ? VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC" : ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(baseUri, projection, selection, null, order);
            if( cursor != null && cursor.moveToFirst() ) {
                boolean found = false;
                File save_folder = getImageFolder(); // may be null if using SAF
                String save_folder_string = save_folder == null ? null : save_folder.getAbsolutePath() + File.separator;
                do {
                    String path = cursor.getString(column_data_c);
                    // path may be null on Android 4.4!: http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
                    if( save_folder_string == null || (path != null && path.contains(save_folder_string) ) ) {
                        // we filter files with dates in future, in case there exists an image in the folder with incorrect datestamp set to the future
                        // we allow up to 2 days in future, to avoid risk of issues to do with timezone etc
                        long date = cursor.getLong(column_date_taken_c);
                        long current_time = System.currentTimeMillis();
                        if( date > current_time + 172800000 ) {
                            //Log.d(TAG, "skip date in the future!");
                        }
                        else {
                            found = true;
                            break;
                        }
                    }
                }
                while( cursor.moveToNext() );
                if( found ) {
                    // make sure we prefer JPEG/etc (non RAW) if there's a JPEG/etc version of this image
                    // this is because we want to support RAW only and JPEG+RAW modes
                    String path = cursor.getString(column_data_c);
                    // path may be null on Android 4.4, see above!
                    if( path != null && path.toLowerCase(Locale.US).endsWith(".dng") ) {
                        int dng_pos = cursor.getPosition();
                        boolean found_non_raw = false;
                        String path_without_ext = path.toLowerCase(Locale.US);
                        if( path_without_ext.indexOf(".") > 0 )
                            path_without_ext = path_without_ext.substring(0, path_without_ext.lastIndexOf("."));
                        while( cursor.moveToNext() ) {
                            String next_path = cursor.getString(column_data_c);
                            if( next_path == null )
                                break;
                            String next_path_without_ext = next_path.toLowerCase(Locale.US);
                            if( next_path_without_ext.indexOf(".") > 0 )
                                next_path_without_ext = next_path_without_ext.substring(0, next_path_without_ext.lastIndexOf("."));
                            if( !path_without_ext.equals(next_path_without_ext) )
                                break;
                            // so we've found another file with matching filename - is it a JPEG/etc?
                            if( next_path.toLowerCase(Locale.US).endsWith(".jpg") ) {
                                found_non_raw = true;
                                break;
                            }
                            else if( next_path.toLowerCase(Locale.US).endsWith(".webp") ) {
                                found_non_raw = true;
                                break;
                            }
                            else if( next_path.toLowerCase(Locale.US).endsWith(".png") ) {
                                found_non_raw = true;
                                break;
                            }
                        }
                        if( !found_non_raw ) {
                            cursor.moveToPosition(dng_pos);
                        }
                    }
                }
                if( !found ) {
                    //Log.d(TAG, "can't find suitable in Open Camera folder, so just go with most recent");
                    cursor.moveToFirst();
                }
                long id = cursor.getLong(column_id_c);
                long date = cursor.getLong(column_date_taken_c);
                int orientation = video ? 0 : cursor.getInt(column_orientation_c);
                Uri uri = ContentUris.withAppendedId(baseUri, id);
                String path = cursor.getString(column_data_c);
                media = new Media(id, video, uri, date, orientation, path);
            }
        }
        catch(Exception e) {
            // have had exceptions such as SQLiteException, NullPointerException reported on Google Play from within getContentResolver().query() call
            e.printStackTrace();
        }
        finally {
            if( cursor != null ) {
                cursor.close();
            }
        }

        return media;
    }

    public Media getLatestMedia() {
        Media image_media = getLatestMedia(false);
        Media video_media = getLatestMedia(true);
        Media media = null;

        if( image_media != null && video_media == null ) {
            media = image_media;
        }
        else if( image_media == null && video_media != null ) {
            media = video_media;
        }
        else if( image_media != null && video_media != null ) {
            if( image_media.date >= video_media.date ) {
                media = image_media;
            }
            else {
                media = video_media;
            }
        }

        return media;
    }
}
