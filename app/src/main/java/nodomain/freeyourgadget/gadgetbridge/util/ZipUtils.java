package nodomain.freeyourgadget.gadgetbridge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {
   private static final Logger LOG = LoggerFactory.getLogger(ZipUtils.class);

   public static byte[] getFileFromZip(final byte[] zipBytes, final String path) {
      try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

         ZipEntry zipEntry;
         while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            if (!zipEntry.getName().equals(path)) continue; // TODO: is this always a path? The documentation is very vague.

            // Found, but not a file
            if (zipEntry.isDirectory()) {
               LOG.error(String.format("Path in ZIP file is a directory: %s", path));
               return null;
            }

            // Found, and it is a file
            return readAllBytes(zipInputStream);
         }

         // Not found
         LOG.error(String.format("Path in ZIP file was not found: %s", path));
         return null;

      } catch (final IOException e) {
         LOG.error(String.format("Unknown error while reading file from ZIP file: %s", path), e);
         return null;
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
