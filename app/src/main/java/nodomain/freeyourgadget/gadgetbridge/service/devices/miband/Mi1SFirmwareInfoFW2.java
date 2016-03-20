package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.NonNull;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SFirmwareInfoFW2 extends AbstractMi1SFirmwareInfo {

    Mi1SFirmwareInfoFW2(@NonNull byte[] wholeFirmwareBytes) {
        super(wholeFirmwareBytes);
    }

    @Override
    public int getFirmwareOffset()
    {
        return (wholeFirmwareBytes[26] & 255) << 24
                | (wholeFirmwareBytes[27] & 255) << 16
                | (wholeFirmwareBytes[28] & 255) << 8
                | (wholeFirmwareBytes[29] & 255);
    }

    @Override
    public int getFirmwareLength()
    {
        return (wholeFirmwareBytes[30] & 255) << 24
                | (wholeFirmwareBytes[31] & 255) << 16
                | (wholeFirmwareBytes[32] & 255) << 8
                | (wholeFirmwareBytes[33] & 255);
    }

    @Override
    protected boolean isGenerallySupportedFirmware() {
        try {
            int majorVersion = getFirmwareVersionMajor();
            return majorVersion == 1;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public int getFirmwareVersion()
    {
        return (wholeFirmwareBytes[22] & 255) << 24
                | (wholeFirmwareBytes[23] & 255) << 16
                | (wholeFirmwareBytes[24] & 255) << 8
                | wholeFirmwareBytes[25] & 255;
    }
}
