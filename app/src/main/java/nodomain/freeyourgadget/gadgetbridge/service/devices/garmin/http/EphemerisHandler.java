package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class EphemerisHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EphemerisHandler.class);
    private final GarminSupport deviceSupport;

    public EphemerisHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public byte[] handleEphemerisRequest(final String path, final Map<String, String> query) {
        try {
            final File agpsFile = deviceSupport.getAgpsFile();
            if (!agpsFile.exists() || !agpsFile.isFile()) {
                LOG.info("File with AGPS data does not exist.");
                return null;
            }
            try(InputStream agpsIn = new FileInputStream(agpsFile)) {
                final byte[] rawBytes = FileUtils.readAll(agpsIn, 1024 * 1024); // 1MB, they're usually ~60KB
                LOG.info("Sending new AGPS data to the device.");
                return rawBytes;
            }
        } catch (IOException e) {
            LOG.error("Unable to obtain ephemeris data.", e);
            return null;
        }
    }

    public Callable<Void> getOnDataSuccessfullySentListener() {
        return () -> {
            LOG.info("AGPS data successfully sent to the device.");
            if (deviceSupport.getAgpsFile().delete()) {
                LOG.info("AGPS data was deleted from the cache folder.");
            }
            return null;
        };
    }
}
