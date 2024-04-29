package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.file.GarminAgpsDataType;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBTarFile;

public class EphemerisHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EphemerisHandler.class);
    private static final String QUERY_CONSTELLATIONS = "constellations";
    private final GarminSupport deviceSupport;

    public EphemerisHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public byte[] handleEphemerisRequest(final String path, final Map<String, String> query) {
        try {
            if (!query.containsKey(QUERY_CONSTELLATIONS)) {
                LOG.debug("Query does not contain information about constellations; skipping request.");
                return null;
            }
            final File agpsFile = deviceSupport.getAgpsFile();
            if (!agpsFile.exists() || !agpsFile.isFile()) {
                LOG.info("File with AGPS data does not exist.");
                return null;
            }
            try(InputStream agpsIn = new FileInputStream(agpsFile)) {
                final byte[] rawBytes = FileUtils.readAll(agpsIn, 1024 * 1024); // 1MB, they're usually ~60KB
                final GBTarFile tarFile = new GBTarFile(rawBytes);
                final String[] requestedConstellations = Objects.requireNonNull(query.get(QUERY_CONSTELLATIONS)).split(",");
                for (final String constellation: requestedConstellations) {
                    try {
                        final GarminAgpsDataType garminAgpsDataType = GarminAgpsDataType.valueOf(constellation);
                        if (!tarFile.containsFile(garminAgpsDataType.getFileName())) {
                            LOG.error("AGPS archive is missing requested file: {}", garminAgpsDataType.getFileName());
                            return null;
                        }
                    } catch (IllegalArgumentException e) {
                        LOG.error("Device requested unsupported AGPS data type: {}", constellation);
                        return null;
                    }
                }
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
