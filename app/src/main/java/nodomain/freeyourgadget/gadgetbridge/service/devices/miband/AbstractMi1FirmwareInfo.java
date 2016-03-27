package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

/**
 * Some helper methods for Mi1 and Mi1A firmware.
 */
public abstract class AbstractMi1FirmwareInfo extends AbstractMiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMi1FirmwareInfo.class);

    private static final byte[] SINGLE_FW_HEADER = new byte[]{
            0,
            (byte) 0x98,
            0,
            (byte) 0x20,
            (byte) 0x89,
            4,
            0,
            (byte) 0x20
    };
    private static final int SINGLE_FW_HEADER_OFFSET = 0;

    private static final int MI1_FW_BASE_OFFSET = 1056;

    protected AbstractMi1FirmwareInfo(@NonNull byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
    }

    @Override
    public boolean isSingleMiBandFirmware() {
        return true;
    }

    @Override
    public int getFirmwareOffset() {
        return 0;
    }

    public int getFirmwareLength() {
        return wholeFirmwareBytes.length;
    }

    public int getFirmwareVersion() {
        return (wholeFirmwareBytes[getOffsetFirmwareVersionMajor()] << 24)
                | (wholeFirmwareBytes[getOffsetFirmwareVersionMinor()] << 16)
                | (wholeFirmwareBytes[getOffsetFirmwareVersionRevision()] << 8)
                | wholeFirmwareBytes[getOffsetFirmwareVersionBuild()];
    }

    private int getOffsetFirmwareVersionMajor() {
        return MI1_FW_BASE_OFFSET + 3;
    }

    private int getOffsetFirmwareVersionMinor() {
        return MI1_FW_BASE_OFFSET + 2;
    }

    private int getOffsetFirmwareVersionRevision() {
        return MI1_FW_BASE_OFFSET + 1;
    }

    private int getOffsetFirmwareVersionBuild() {
        return MI1_FW_BASE_OFFSET;
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            if (!isHeaderValid()) {
                LOG.info("unrecognized header");
                return false;
            }
            int majorVersion = getFirmwareVersionMajor();
            if (majorVersion == getSupportedMajorVersion()) {
                return true;
            } else {
                LOG.info("Only major version " + getSupportedMajorVersion() + " is supported: " + majorVersion);
            }
        } catch (IllegalArgumentException ex) {
            LOG.warn("invalid firmware or bug: " + ex.getLocalizedMessage(), ex);
        } catch (IndexOutOfBoundsException ex) {
            LOG.warn("not supported firmware: " + ex.getLocalizedMessage(), ex);
        }
        return false;
    }

    protected boolean isHeaderValid() {
        // TODO: not sure if this is a correct check!
        return ArrayUtils.equals(SINGLE_FW_HEADER, wholeFirmwareBytes, SINGLE_FW_HEADER_OFFSET, SINGLE_FW_HEADER_OFFSET + SINGLE_FW_HEADER.length);
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        super.checkValid();

        if (wholeFirmwareBytes.length < SINGLE_FW_HEADER.length) {
            throw new IllegalArgumentException("firmware too small: " + wholeFirmwareBytes.length);
        }
    }

    protected abstract int getSupportedMajorVersion();
}
