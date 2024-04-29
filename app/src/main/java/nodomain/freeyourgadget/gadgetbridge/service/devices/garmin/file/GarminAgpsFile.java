package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.GBTarFile;


public class GarminAgpsFile {
    private static final Logger LOG = LoggerFactory.getLogger(GarminAgpsFile.class);
    private final byte[] tarBytes;

    public GarminAgpsFile(final byte[] tarBytes) {
        this.tarBytes = tarBytes;
    }

    public boolean isValid() {
        if (!GBTarFile.isTarFile(tarBytes)) {
            LOG.debug("Is not TAR file!");
            return false;
        }

        final GBTarFile tarFile = new GBTarFile(tarBytes);
        for (final String fileName: tarFile.listFileNames()) {
            if (!GarminAgpsDataType.isValidAgpsDataFileName(fileName)) {
                LOG.error("Unknown file in TAR archive: {}", fileName);
                return false;
            }
        }

        return true;
    }

    public byte[] getBytes() {
        return tarBytes.clone();
    }
}
