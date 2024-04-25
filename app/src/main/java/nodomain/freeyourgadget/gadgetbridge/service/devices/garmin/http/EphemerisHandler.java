package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;

public class EphemerisHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EphemerisHandler.class);
    private final GarminSupport deviceSupport;

    public EphemerisHandler(GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public byte[] handleEphemerisRequest(final String path, final Map<String, String> query) {
        // TODO Return status code 304 (Not Modified) when we don't have newer data and "if-none-match" is set.
        try {
            final File exportDirectory = deviceSupport.getWritableExportDirectory();
            final File ephemerisDataFile = new File(exportDirectory, "CPE.BIN");
            if (!ephemerisDataFile.exists() || !ephemerisDataFile.isFile()) {
                throw new IOException("Cannot locate CPE.BIN file in export/import directory.");
            }
            final byte[] bytes = new byte[(int) ephemerisDataFile.length()];
            final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(ephemerisDataFile));
            final DataInputStream dis = new DataInputStream(bis);
            dis.readFully(bytes);
            return bytes;
        } catch (IOException e) {
            LOG.error("Unable to obtain ephemeris data.", e);
            return null;
        }
    }
}
