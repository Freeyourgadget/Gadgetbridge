package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;


public class GarminAgpsFile {
    private static final Logger LOG = LoggerFactory.getLogger(GarminAgpsFile.class);
    public static final int TAR_MAGIC_BYTES_OFFSET = 257;
    public static final byte[] TAR_MAGIC_BYTES = new byte[]{
            'u', 's', 't', 'a', 'r', '\0'
    };
    private final byte[] tarBytes;

    public GarminAgpsFile(final byte[] tarBytes) {
        this.tarBytes = tarBytes;
    }

    public boolean isValid() {
        if (!ArrayUtils.equals(tarBytes, TAR_MAGIC_BYTES, TAR_MAGIC_BYTES_OFFSET)) {
            LOG.debug("Is not TAR file!");
            return false;
        }

        // TODO Add additional checks.
        // Archive usually contains following files:
        // CPE_GLO.BIN
        // CPE_QZSS.BIN
        // CPE_GPS.BIN
        // CPE_GAL.BIN

        return true;
    }

    public byte[] getBytes() {
        return tarBytes.clone();
    }
}
