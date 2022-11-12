package nodomain.freeyourgadget.gadgetbridge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import androidx.annotation.Nullable;

/**
 * Utility class for recognition and reading of ZIP archives.
 */
public class ZipFile {
    private static final Logger LOG = LoggerFactory.getLogger(ZipFile.class);
    public static final byte[] ZIP_HEADER = new byte[]{
        0x50, 0x4B, 0x03, 0x04
    };

    private final byte[] zipBytes;

    /**
     * Open ZIP file from byte array already in memory.
     * @param zipBytes data to handle as a ZIP file.
     */
    public ZipFile(byte[] zipBytes) {
        this.zipBytes = zipBytes;
    }

    /**
     * Open ZIP file from InputStream.<br>
     * This will read the entire file into memory at once.
     * @param inputStream data to handle as a ZIP file.
     */
    public ZipFile(InputStream inputStream) throws IOException {
        this.zipBytes = readAllBytes(inputStream);
    }

    /**
     * Checks if data resembles a ZIP file.<br>
     * The check is not infallible: it may report self-extracting or other exotic ZIP archives as not a ZIP file, and it may report a corrupted ZIP file as a ZIP file.
     * @param data The data to check.
     * @return Whether data resembles a ZIP file.
     */
    public static boolean isZipFile(byte[] data) {
        return ArrayUtils.equals(data, ZIP_HEADER, 0);
    }

    /**
     * Reads the contents of file at path into a byte array.
     * @param path Path of the file in the ZIP file.
     * @return byte array contatining the contents of the requested file.
     * @throws ZipFileException If the specified path does not exist or references a directory, or if some other I/O error occurs. In other words, if return value would otherwise be null.
     */
    public byte[] getFileFromZip(final String path) throws ZipFileException {
        try (InputStream is = new ByteArrayInputStream(zipBytes); ZipInputStream zipInputStream = new ZipInputStream(is)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.getName().equals(path)) continue; // TODO: is this always a path? The documentation is very vague.

                if (zipEntry.isDirectory()) {
                    throw new ZipFileException(String.format("Path in ZIP file is a directory: %s", path));
                }

                return readAllBytes(zipInputStream);
            }

            throw new ZipFileException(String.format("Path in ZIP file was not found: %s", path));

        } catch (ZipException e) {
            throw new ZipFileException("The ZIP file might be corrupted");
        } catch (IOException e) {
            throw new ZipFileException("General IO error");
        }
    }

    private static byte[] readAllBytes(final InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int n;
        byte[] buf = new byte[16384];

        while ((n = is.read(buf, 0, buf.length)) != -1) {
            buffer.write(buf, 0, n);
        }

        return buffer.toByteArray();
    }
}