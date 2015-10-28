package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class FileUtils {
    // Don't use slf4j here -- would be a bootstrapping problem
    private static final String TAG = "FileUtils";

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
        List<File> dirs = getWritableExternalFilesDirs();
        for (File dir : dirs) {
            if (canWriteTo(dir)) {
                return dir;
            }
        }
        throw new IOException("no writable external directory found");
    }

    private static boolean canWriteTo(File dir) {
        File file = new File(dir, "gbtest");
        try {
            FileOutputStream test = new FileOutputStream(file);
            try {
                test.close();
            } catch (IOException e) {
                // ignore
            }
            file.delete();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    private static List<File> getWritableExternalFilesDirs() throws IOException {
        Context context = GBApplication.getContext();
        File[] dirs = context.getExternalFilesDirs(null);
        List<File> result = new ArrayList<>(dirs.length);
        if (dirs == null) {
            throw new IOException("Unable to access external files dirs: null");
        }
        if (dirs.length == 0) {
            throw new IOException("Unable to access external files dirs: 0");
        }
        for (int i = 0; i < dirs.length; i++) {
            File dir = dirs[i];
            if (!dir.exists() && !dir.mkdirs()) {
                continue;
            }
//            if (!dir.canWrite() || !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(dir))) {
            if (!dir.canWrite() || (i == 0 && !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))) {
                Log.i(TAG, "ignoring non-writable external storage dir: " + dir);
                continue;
            }
//            if (Environment.isExternalStorageEmulated(dir)) {
            if (i == 0 && Environment.isExternalStorageEmulated()) {
                result.add(dir); // add last
            } else {
                result.add(0, dir); // add first
            }
        }
        return result;
    }

    /**
     * Reads the contents of the given InputStream into a byte array, but does not
     * read more than maxLen bytes. If the stream provides more than maxLen bytes,
     * an IOException is thrown.
     *
     * @param in     the stream to read from
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