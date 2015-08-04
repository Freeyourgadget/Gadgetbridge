package nodomain.freeyourgadget.gadgetbridge.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class FileUtils {
    /**
     * Copies the the given sourceFile to destFile, overwriting it, in case it exists.
     *
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            throw new IOException("Does not exist: " + sourceFile.getAbsolutePath());
        }
        copyFile(new FileInputStream(sourceFile), new FileOutputStream(destFile));
    }

    private static void copyFile(FileInputStream sourceStream, FileOutputStream destStream) throws IOException {
        try (FileChannel fromChannel = sourceStream.getChannel(); FileChannel toChannel = destStream.getChannel()) {
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        }
    }

    /**
     * Returns the existing external storage dir.
     *
     * @throws IOException when the directory is not available
     */
    public static File getExternalFilesDir() throws IOException {
        File dir = GBApplication.getContext().getExternalFilesDir(null);
        if (dir == null) {
            throw new IOException("Unable to access external files dir: null");
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to access external files dir: does not exist");
        }
        return dir;
    }

    /**
     * Reads the contents of the given InputStream into a byte array, but does not
     * read more than maxLen bytes.
     * @param in the stream to read from
     * @param maxLen the maximum number of bytes to read/return
     * @return the bytes read from the InputStream
     * @throws IOException when reading failed or when maxLen was exceeded
     */
    public static byte[] readAll(InputStream in, long maxLen) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, in.available()));
        byte[] buf = new byte[8192];
        int read = 0;
        long totalRead = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            totalRead += read;
            if (totalRead > maxLen) {
                throw new IOException("Too much data to read into memory. Got already " + totalRead + buf);
            }
        }
        return out.toByteArray();
    }
}