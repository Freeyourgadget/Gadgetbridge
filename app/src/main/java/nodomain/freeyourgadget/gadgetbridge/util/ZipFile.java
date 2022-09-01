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

public class ZipFile implements AutoCloseable {
   private static final Logger LOG = LoggerFactory.getLogger(ZipFile.class);
   public static final byte[] ZIP_HEADER = new byte[]{
       0x50, 0x4B, 0x03, 0x04
   };

   private final ZipInputStream zipInputStream;

   /**
    * Open ZIP file from byte array in memory
    * @param zipBytes
    */
   public ZipFile(byte[] zipBytes) {
      zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
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
      try {
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

   /**
    * Tries to obtain file from ZIP file without much hassle, but is not safe.<br>
    * Please only use this in place of old code where correctness of the result is checked only later on.<br>
    * Use getFileFromZip of ZipFile instance instead.
    * @param zipBytes
    * @param path Path of the file in the ZIP file.
    * @return Contents of requested file or null.
    */
   @Deprecated
   @Nullable
   public static byte[] tryReadFileQuick(final byte[] zipBytes, final String path) {
      try (ZipFile zip = new ZipFile(zipBytes)) {
         return zip.getFileFromZip(path);
      } catch (ZipFileException e) {
         LOG.error("Quick ZIP reading failed.", e);
      } catch (Exception e) {
         LOG.error("Unable to close ZipFile.", e);
      }

      return null;
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

   @Override
   public void close() throws Exception {
      zipInputStream.close();
   }
}