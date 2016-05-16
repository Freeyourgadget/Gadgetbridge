package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Mi firmware info class with two child info instances.
 */
public abstract class CompositeMiFirmwareInfo<T extends AbstractMiFirmwareInfo> extends AbstractMiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeMiFirmwareInfo.class);

    private final T fw1Info;
    private final T fw2Info;

    protected CompositeMiFirmwareInfo(byte[] wholeFirmwareBytes, T info1, T info2) {
        super(wholeFirmwareBytes);
        fw1Info = info1;
        fw2Info = info2;
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        super.checkValid();

        if (getFirst().getFirmwareOffset() == getSecond().getFirmwareOffset()) {
            throw new IllegalArgumentException("Illegal firmware offsets: " + getLengthsOffsetsString());
        }
        if (getFirst().getFirmwareOffset() < 0 || getSecond().getFirmwareOffset() < 0
                || getFirst().getFirmwareLength() <= 0 || getSecond().getFirmwareLength() <= 0) {
            throw new IllegalArgumentException("Illegal firmware offsets/lengths: " + getLengthsOffsetsString());
        }

        int firstEndIndex = getFirst().getFirmwareOffset() + getFirst().getFirmwareLength();
        if (getSecond().getFirmwareOffset() < firstEndIndex) {
            throw new IllegalArgumentException("Invalid firmware, second fw starts before first fw ends: " + firstEndIndex + "," + getSecond().getFirmwareOffset());
        }
        int secondEndIndex = getSecond().getFirmwareOffset();
        if (wholeFirmwareBytes.length < firstEndIndex || wholeFirmwareBytes.length < secondEndIndex) {
            throw new IllegalArgumentException("Invalid firmware size, or invalid offsets/lengths: " + getLengthsOffsetsString());
        }

        getFirst().checkValid();
        getSecond().checkValid();
    }

    protected String getLengthsOffsetsString() {
        return getFirst().getFirmwareOffset() + "," + getFirst().getFirmwareLength()
                + "; "
                + getSecond().getFirmwareOffset() + "," + getSecond().getFirmwareLength();
    }

    @Override
    public T getFirst() {
        return fw1Info;
    }

    @Override
    public T getSecond() {
        return fw2Info;
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
