package za.co.rationalthinkers.unocapture.android.util;

import android.media.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Saves a JPEG Image into the specified File.
 */
public class ImageSaver implements Runnable {

    /**
     * The JPEG image
     */
    private final Image mImage;

    /**
     * The file we save the image into.
     */
    private final File mFile;

    public ImageSaver(Image image, File file) {
        mImage = image;
        mFile = file;
    }

    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
        finally {
            mImage.close();

            if (null != output) {

                try {
                    output.close();
                }
                catch (IOException e) {
                    //e.printStackTrace();
                }

            }
        }
    }

}
