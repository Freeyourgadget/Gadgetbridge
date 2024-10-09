package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.agps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBTarFile;


public class GarminAgpsFile {
    private static final Logger LOG = LoggerFactory.getLogger(GarminAgpsFile.class);
    private final byte[] bytes;

    public static final byte[] GZ_HEADER = new byte[]{(byte) 0x1f, (byte) 0x8b};
    public static final byte[] CPE_RXNETWORKS_HEADER = new byte[]{(byte) 0x01, (byte) 0x00};
    public static final byte[] CPE_SONY_HEADER = new byte[]{(byte) 0x2a, (byte) 0x12, (byte) 0xa0, (byte) 0x02};

    public GarminAgpsFile(final byte[] bytes) {
        this.bytes = bytes;
    }

    public boolean isValidTar(final String[] constellations) {
        if (!GBTarFile.isTarFile(bytes)) {
            return false;
        }

        final GBTarFile tarFile = new GBTarFile(bytes);
        for (final String constellation : constellations) {
            try {
                final GarminAgpsDataType garminAgpsDataType = GarminAgpsDataType.valueOf(constellation);
                if (!tarFile.containsFile(garminAgpsDataType.getFileName())) {
                    LOG.error("AGPS archive is missing requested file: {}", garminAgpsDataType.getFileName());
                    return false;
                }
            } catch (final IllegalArgumentException e) {
                LOG.error("Device requested unsupported AGPS data type: {}", constellation);
                return false;
            }
        }

        return true;
    }

    public boolean isValidRxNetworks() {
        if (!ArrayUtils.startsWith(bytes, GZ_HEADER)) {
            return false;
        }

        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            final byte[] header = new byte[CPE_RXNETWORKS_HEADER.length];
            int read = gzis.read(header);
            if (read != CPE_RXNETWORKS_HEADER.length || !Arrays.equals(header, CPE_RXNETWORKS_HEADER)) {
                LOG.error("Header in gz file is not agps rxnetworks: {}", GB.hexdump(header));
                return false;
            }
            final byte[] timestampBytes = new byte[4];
            read = gzis.read(timestampBytes);
            if (read != timestampBytes.length) {
                LOG.error("Failed to read rxnetworks timestamp");
                return false;
            }
            final int agpsTimestamp = ByteBuffer.wrap(timestampBytes).order(ByteOrder.BIG_ENDIAN).getInt();
            final int currentTimestamp = (int) (System.currentTimeMillis() / 1000L);
            final int agpsAge = currentTimestamp - agpsTimestamp;
            if (agpsAge < 0) {
                LOG.error("rxnetworks AGPS timestamp {} is in the future", agpsTimestamp);
                return false;
            }
            if (agpsAge > 604800) {
                LOG.error("rxnetworks AGPS timestamp {} is older than 7 days", agpsTimestamp);
                return false;
            }
            LOG.debug("rx networks AGPS age: {}", agpsAge);
            return true;
        } catch (final IOException e) {
            LOG.error("Failed to decompress file as gzip", e);
        }

        return false;
    }

    public boolean isValidSonyCpe() {
        return ArrayUtils.startsWith(bytes, CPE_SONY_HEADER);
    }

    public byte[] getBytes() {
        return bytes.clone();
    }
}
