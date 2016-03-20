package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.NonNull;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SFirmwareInfoFW1 extends AbstractMi1SFirmwareInfo {

    private static final int MI1S_FW_BASE_OFFSET = 1092;

    Mi1SFirmwareInfoFW1(@NonNull byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
    }

    @Override
    public int getFirmwareOffset() {
        return (wholeFirmwareBytes[12] & 255) << 24
                | (wholeFirmwareBytes[13] & 255) << 16
                | (wholeFirmwareBytes[14] & 255) << 8
                | (wholeFirmwareBytes[15] & 255);
    }

    @Override
    public int getFirmwareLength() {
        return (wholeFirmwareBytes[16] & 255) << 24
                | (wholeFirmwareBytes[17] & 255) << 16
                | (wholeFirmwareBytes[18] & 255) << 8
                | (wholeFirmwareBytes[19] & 255);
    }

    @Override
    public int getFirmwareVersion() {
        return (wholeFirmwareBytes[8] & 255) << 24
                | (wholeFirmwareBytes[9] & 255) << 16
                | (wholeFirmwareBytes[10] & 255) << 8
                | wholeFirmwareBytes[11] & 255;
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            int majorVersion = getFirmwareVersionMajor();
            return majorVersion == 4;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
