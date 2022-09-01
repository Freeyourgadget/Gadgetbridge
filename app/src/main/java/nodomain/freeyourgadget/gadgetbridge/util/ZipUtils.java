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
            if (zipEntry.getName().equals(path)) {
               return readAllBytes(zipInputStream);
            }
         }
      } catch (final IOException e) {
         LOG.error(String.format("Failed to read %s from zip", path), e);
         return null;
      }

      LOG.debug("{} not found in zip", path);

      return null;
   }

   public static byte[] readAllBytes(final InputStream is) throws IOException {
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      int n;
      byte[] buf = new byte[16384];

      while ((n = is.read(buf, 0, buf.length)) != -1) {
         buffer.write(buf, 0, n);
      }

      return buffer.toByteArray();
   }
}
