package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

/**
 * FW1 is Mi Band firmware
 * FW2 is heartrate firmware
 */
public class Mi1SInfo {

    public static int getFirmware2OffsetIn(byte[] wholeFirmwareBytes)
    {
        return (wholeFirmwareBytes[26] & 255) << 24
                | (wholeFirmwareBytes[27] & 255) << 16
                | (wholeFirmwareBytes[28] & 255) << 8
                | (wholeFirmwareBytes[29] & 255);
    }

    public static int getFirmware2LengthIn(byte[] wholeFirmwareBytes)
    {
        return (wholeFirmwareBytes[30] & 255) << 24
                | (wholeFirmwareBytes[31] & 255) << 16
                | (wholeFirmwareBytes[32] & 255) << 8
                | (wholeFirmwareBytes[33] & 255);
    }

    public static int getFirmware1OffsetIn(byte[] wholeFirmwareBytes)
    {
        return (wholeFirmwareBytes[12] & 255) << 24
                | (wholeFirmwareBytes[13] & 255) << 16
                | (wholeFirmwareBytes[14] & 255) << 8
                | (wholeFirmwareBytes[15] & 255);
    }

    public static int getFirmware1LengthIn(byte[] wholeFirmwareBytes)
    {
        return (wholeFirmwareBytes[16] & 255) << 24
                | (wholeFirmwareBytes[17] & 255) << 16
                | (wholeFirmwareBytes[18] & 255) << 8
                | (wholeFirmwareBytes[19] & 255);
    }

    public static int getFirmware1VersionFrom(byte[] wholeFirmwareBytes)
    {
        return (wholeFirmwareBytes[8] & 255) << 24
                | (wholeFirmwareBytes[9] & 255) << 16
                | (wholeFirmwareBytes[10] & 255) << 8
                | wholeFirmwareBytes[11] & 255;
    }

    public static int getFirmware2VersionFrom(byte[] wholeFirmwareBytes)
    {
        return (wholeFirmwareBytes[22] & 255) << 24
                | (wholeFirmwareBytes[23] & 255) << 16
                | (wholeFirmwareBytes[24] & 255) << 8
                | wholeFirmwareBytes[25] & 255;
    }

    // FIXME: this method is wrong. We don't know a way to check if a firmware file
    // contains one or more firmwares.
    public static boolean isSingleMiBandFirmware(byte[] wholeFirmwareBytes) {
        if ((wholeFirmwareBytes[7] & 255) != 1) {
            return false;
        }
        return true;
    }

}
