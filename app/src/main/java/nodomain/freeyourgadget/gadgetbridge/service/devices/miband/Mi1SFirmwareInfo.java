package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SFirmwareInfo extends AbstractMi1SFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMi1FirmwareInfo.class);

    private final Mi1SFirmwareInfoFW1 fw1Info;
    private final Mi1SFirmwareInfoFW2 fw2Info;

    private Mi1SFirmwareInfo(byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
        fw1Info = new Mi1SFirmwareInfoFW1(wholeFirmwareBytes);
        fw2Info = new Mi1SFirmwareInfoFW2(wholeFirmwareBytes);
    }

    @Override
    public AbstractMiFirmwareInfo getFirst() {
        return fw1Info;
    }

    @Override
    public AbstractMiFirmwareInfo getSecond() {
        return fw2Info;
    }

    public static @Nullable Mi1SFirmwareInfo getInstance(byte[] wholeFirmwareBytes) {
        Mi1SFirmwareInfo info = new Mi1SFirmwareInfo(wholeFirmwareBytes);
        if (info.isGenerallySupportedFirmware()) {
            return info;
        }
        LOG.info("firmware not supported");
        return null;
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        if (isSingleMiBandFirmware()) {
            return false;
        }
        try {
            return fw1Info.isGenerallySupportedFirmware()
                    && fw2Info.isGenerallySupportedFirmware()
                    && fw1Info.getFirmwareBytes().length > 0
                    && fw2Info.getFirmwareBytes().length > 0;
        } catch (IndexOutOfBoundsException ex) {
            return false;
        } catch (IllegalArgumentException ex) {
            LOG.warn("not supported 1S firmware: ", ex);
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
