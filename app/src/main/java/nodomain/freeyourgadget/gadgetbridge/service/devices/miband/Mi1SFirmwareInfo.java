package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SFirmwareInfo extends AbstractMi1SFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Mi1SFirmwareInfo.class);

    private static final byte[] DOUBLE_FW_HEADER = new byte[] {
            (byte)0x78,
            (byte)0x75,
            (byte)0x63,
            (byte)0x6b
    };
    private static final int DOUBLE_FW_HEADER_OFFSET = 0;

    private final Mi1SFirmwareInfoFW1 fw1Info;
    private final Mi1SFirmwareInfoFW2 fw2Info;

    private Mi1SFirmwareInfo(byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
        fw1Info = new Mi1SFirmwareInfoFW1(wholeFirmwareBytes);
        fw2Info = new Mi1SFirmwareInfoFW2(wholeFirmwareBytes);
    }

    protected boolean isHeaderValid() {
        // TODO: not sure if this is a correct check!
        return ArrayUtils.equals(DOUBLE_FW_HEADER, wholeFirmwareBytes, DOUBLE_FW_HEADER_OFFSET, DOUBLE_FW_HEADER_OFFSET + DOUBLE_FW_HEADER.length);
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        super.checkValid();
        int firstEndIndex = getFirst().getFirmwareOffset() + getFirst().getFirmwareLength();
        if (getSecond().getFirmwareOffset() < firstEndIndex) {
            throw new IllegalArgumentException("Invalid firmware offsets/lengths: " + getLengthsOffsetsString());
        }
        int secondEndIndex = getSecond().getFirmwareOffset();
        if (wholeFirmwareBytes.length < firstEndIndex || wholeFirmwareBytes.length < secondEndIndex) {
            throw new IllegalArgumentException("Invalid firmware size, or invalid offsets/lengths: " + getLengthsOffsetsString());
        }
        if (getSecond().getFirmwareOffset() < firstEndIndex) {
            throw new IllegalArgumentException("Invalid firmware, second fw starts before first fw ends: " + firstEndIndex + "," + getSecond().getFirmwareOffset());
        }
    }

    protected String getLengthsOffsetsString() {
        return getFirst().getFirmwareOffset() + "," + getFirst().getFirmwareLength()
                + "; "
                + getSecond().getFirmwareOffset() + "," + getSecond().getFirmwareLength();
    }

    @Override
    public AbstractMiFirmwareInfo getFirst() {
        return fw1Info;
    }

    @Override
    public AbstractMiFirmwareInfo getSecond() {
        return fw2Info;
    }

    public static
    @Nullable
    Mi1SFirmwareInfo getInstance(byte[] wholeFirmwareBytes) {
        Mi1SFirmwareInfo info = new Mi1SFirmwareInfo(wholeFirmwareBytes);
        if (info.isGenerallySupportedFirmware()) {
            return info;
        }
        LOG.info("firmware not supported");
        return null;
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            if (!isHeaderValid()) {
                LOG.info("unrecognized header");
                return false;
            }
            return fw1Info.isGenerallySupportedFirmware()
                    && fw2Info.isGenerallySupportedFirmware()
                    && fw1Info.getFirmwareBytes().length > 0
                    && fw2Info.getFirmwareBytes().length > 0;
        } catch (IndexOutOfBoundsException ex) {
            LOG.warn("invalid firmware or bug: " + ex.getLocalizedMessage(), ex);
            return false;
        } catch (IllegalArgumentException ex) {
            LOG.warn("not supported 1S firmware: " + ex.getLocalizedMessage(), ex);
            return false;
        }
    }

    @Override
    public int getFirmwareOffset() {
        throw new UnsupportedOperationException("call this method on getFirmwareXInfo()");
    }

    @Override
    public int getFirmwareLength() {
        throw new UnsupportedOperationException("call this method on getFirmwareXInfo()");
    }

    @Override
    public int getFirmwareVersion() {
        throw new UnsupportedOperationException("call this method on getFirmwareXInfo()");
    }
}
