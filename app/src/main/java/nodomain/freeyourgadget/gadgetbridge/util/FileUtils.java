/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Felix
    Konstantin Maurer, JohnnySun, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.ContentResolver;
import android.content.Context;
import android.icu.util.Output;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;

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
        try (FileInputStream in = new FileInputStream(sourceFile); FileOutputStream out = new FileOutputStream(destFile)) {
            copyFile(in, out);
        }
    }

    private static void copyFile(FileInputStream sourceStream, FileOutputStream destStream) throws IOException {
        try (FileChannel fromChannel = sourceStream.getChannel(); FileChannel toChannel = destStream.getChannel()) {
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        }
    }

    /**
     * Copies the contents of the given input stream to the destination file.
     * @param inputStream the contents to write. Note: the caller has to close the input stream!
     * @param destFile the file to write to
     * @throws IOException
     */
    public static void copyStreamToFile(InputStream inputStream, File destFile) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(destFile)) {
            byte[] buf = new byte[4096];
            while (inputStream.available() > 0) {
                int bytes = inputStream.read(buf);
                fout.write(buf, 0, bytes);
            }
        }
    }

    /**
     * Copies the contents of the given file to the destination output stream.
     * @param src the file from which to read.
     * @param dst the output stream that is written to. Note: the caller has to close the output stream!
     * @throws IOException
     */
    public static void copyFileToStream(File src, OutputStream dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src)) {
            byte[] buf = new byte[4096];
            while(in.available() > 0) {
                int bytes = in.read(buf);
                dst.write(buf, 0, bytes);
            }
        }
    }

    public static void copyURItoFile(Context ctx, Uri uri, File destFile) throws IOException {
        if (uri.getPath().equals(destFile.getPath())) {
            return;
        }

        ContentResolver cr = ctx.getContentResolver();
        InputStream in = cr.openInputStream(uri);
        if (in == null) {
            throw new IOException("unable to open input stream: " + uri);
        }
        try (InputStream fin = new BufferedInputStream(in)) {
            copyStreamToFile(fin, destFile);
            fin.close();
        }
    }

    /**
     * Copies the content of a file to an uri,
     * which for example was retrieved using the storage access framework.
     * @param context the application context.
     * @param src the file from which the content should be copied.
     * @param dst the destination uri.
     * @throws IOException
     */
    public static void copyFileToURI(Context context, File src, Uri dst) throws IOException {
        OutputStream out = context.getContentResolver().openOutputStream(dst);
        if (out == null) {
            throw new IOException("Unable to open output stream for " + dst.toString());
        }
        try (OutputStream bufOut = new BufferedOutputStream(out)) {
            copyFileToStream(src, bufOut);
        }
    }

    /**
     * Returns the textual contents of the given file. The contents is expected to be
     * in UTF-8 encoding.
     * @param file the file to read
     * @return the file contents as a newline-delimited string
     * @throws IOException
     * @see #getStringFromFile(File, String)
     */
    public static String getStringFromFile(File file) throws IOException {
        return getStringFromFile(file, StandardCharsets.UTF_8.name());
    }

    /**
     * Returns the textual contents of the given file. The contents will be interpreted using the
     * given encoding.
     * @param file the file to read
     * @return the file contents as a newline-delimited string
     * @throws IOException
     * @see #getStringFromFile(File)
     */
    public static String getStringFromFile(File file, String encoding) throws IOException {
        FileInputStream fin = new FileInputStream(file);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fin, encoding))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Returns the existing external storage dir. The directory is guaranteed to
     * exist and to be writable.
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
            GB.log("Cannot write to directory: " + dir.getAbsolutePath(), GB.INFO, e);
            return false;
        }
    }

    /**
     * Returns a list of directories to write to. The list is sorted by priority,
     * i.e. the first directory should be preferred, the last one is the least
     * preferred one.
     * <p/>
     * Note that the directories may not exist, so it is not guaranteed that you
     * can actually write to them. But when created, they *should* be writable.
     *
     * @return the list of writable directories
     * @throws IOException
     */
    @NonNull
    private static List<File> getWritableExternalFilesDirs() throws IOException {
        Context context = GBApplication.getContext();
        File[] dirs;
        try {
            dirs = context.getExternalFilesDirs(null);
        } catch (NullPointerException | UnsupportedOperationException ex) {
            // workaround for robolectric 3.1.2 not implementing getExternalFilesDirs()
            // https://github.com/robolectric/robolectric/issues/2531
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                dirs = new File[] { dir };
            } else {
                throw ex;
            }
        }
        if (dirs == null) {
            throw new IOException("Unable to access external files dirs: null");
        }
        List<File> result = new ArrayList<>(dirs.length);
        if (dirs.length == 0) {
            throw new IOException("Unable to access external files dirs: 0");
        }
        for (int i = 0; i < dirs.length; i++) {
            File dir = dirs[i];
            if (dir == null) {
                continue;
            }
            if (!dir.exists() && !dir.mkdirs()) {
                GB.log("Unable to create directories: " + dir.getAbsolutePath(), GB.INFO, null);
                continue;
            }

            // the first directory is also the primary external storage, i.e. the same as Environment.getExternalFilesDir()
            // TODO: check the mount state of *all* dirs when switching to later API level
            if (!GBEnvironment.env().isLocalTest()) { // don't do this with robolectric
                if (i == 0 && !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    GB.log("ignoring unmounted external storage dir: " + dir, GB.INFO, null);
                    continue;
                }
            }
            result.add(dir); // add last
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
        int read;
        long totalRead = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            totalRead += read;
            if (totalRead > maxLen) {
                throw new IOException("Too much data to read into memory. Got already " + totalRead);
            }
        }
        return out.toByteArray();
    }

    public static boolean deleteRecursively(File dir) {
        if (!dir.exists()) {
            return true;
        }
        if (dir.isFile()) {
            return dir.delete();
        }
        for (File sub : dir.listFiles()) {
            if (!deleteRecursively(sub)) {
                return false;
            }
        }
        return dir.delete();
    }

    public static File createTempDir(String prefix) throws IOException {
        File parent = new File(System.getProperty("java.io.tmpdir", "/tmp"));
        for (int i = 1; i < 100; i++) {
            String name = prefix + (int) (Math.random() * 100000);
            File dir = new File(parent, name);
            if (dir.mkdirs()) {
                return dir;
            }
        }
        throw new IOException("Cannot create temporary directory in " + parent);
    }

    /**
     * Replaces some wellknown invalid characters in the given filename
     * to underscrores.
     * @param name the file name to make valid
     * @return the valid file name
     */
    public static String makeValidFileName(String name) {
        return name.replaceAll("\0/:\\r\\n\\\\", "_");
    }
}