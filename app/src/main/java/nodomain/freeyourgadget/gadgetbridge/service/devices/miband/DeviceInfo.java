package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

import java.util.Locale;

public class DeviceInfo extends AbstractInfo {
    public final String deviceId;
    public final int profileVersion;
    public final int fwVersion;
    public final int hwVersion;
    public final int feature;
    public final int appearance;


    private boolean isChecksumCorrect(byte[] data) {
        int crc8 = CheckSums.getCRC8(new byte[]{data[0], data[1], data[2], data[3], data[4], data[5], data[6]});
        return data[7] == (crc8 ^ data[3] & 255);
    }

    public DeviceInfo(byte[] data) {
        super(data);

        if ((data.length == 16 || data.length == 20) && isChecksumCorrect(data)) {
            deviceId = String.format("%02X%02X%02X%02X%02X%02X%02X%02X", data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
            profileVersion = getInt(data, 8);
            fwVersion = getInt(data, 12);
            hwVersion = Integer.decode("0x" + deviceId.substring(12, 14)).intValue();
            feature = Integer.decode("0x" + deviceId.substring(8, 10)).intValue();
            appearance = Integer.decode("0x" + deviceId.substring(10, 12)).intValue();
        } else {
            deviceId = "crc error";
            profileVersion = -1;
            fwVersion = -1;
            hwVersion = -1;
            feature = -1;
            appearance = -1;
        }
    }

    public static int getInt(byte[] data, int from, int len) {
        int ret = 0;
        for(int i = 0; i < len; ++i) {
            ret |= (data[from + i] & 255) << i * 8;
        }
        return ret;
    }

    private int getInt(byte[] data, int from) {
        return getInt(data, from, 4);
    }

    public String getHumanFirmwareVersion() {
        if (fwVersion == -1)
            return GBApplication.getContext().getString(R.string._unknown_);

        return String.format(Locale.US, "%d.%d.%d.%d",
                fwVersion >> 24 & 255,
                fwVersion >> 16 & 255,
                fwVersion >> 8 & 255,
                fwVersion & 255);
    }

    public int getFirmwareVersion() {
        return fwVersion;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", profileVersion=" + profileVersion +
                ", fwVersion=" + fwVersion +
                ", hwVersion=" + hwVersion +
                '}';
    }

    public boolean isMili1A() {
        return (this.feature & 255) == 5 && (this.appearance & 255) == 0 || (this.feature & 255) == 0 && (this.hwVersion & 255) == 208;
    }

}
