/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Sergey Trofimov

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class DeviceInfo extends AbstractInfo {
    public final String deviceId;
    public final int profileVersion;
    /**
     * Mi Band firmware version identifier
     */
    public final int fwVersion;
    public final int hwVersion;
    public final int feature;
    public final int appearance;
    /**
     * Heart rate firmware version identifier
     */
    public final int fw2Version;
    private boolean test1AHRMode;


    private boolean isChecksumCorrect(byte[] data) {
        int crc8 = CheckSums.getCRC8(new byte[]{data[0], data[1], data[2], data[3], data[4], data[5], data[6]});
        return (data[7] & 255) == (crc8 ^ data[3] & 255);
    }

    public DeviceInfo(byte[] data) {
        super(data);

        if ((data.length == 16 || data.length == 20) && isChecksumCorrect(data)) {
            deviceId = String.format("%02X%02X%02X%02X%02X%02X%02X%02X", data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
            profileVersion = getInt(data, 8);
            fwVersion = getInt(data, 12);
            hwVersion = data[6] & 255;
            appearance = data[5] & 255;
            feature = data[4] & 255;
            if (data.length == 20) {
                int s = 0;
                for (int i = 0; i < 4; ++i) {
                    s |= (data[16 + i] & 255) << i * 8;
                }
                fw2Version = s;
            } else {
                fw2Version = -1;
            }
        } else {
            deviceId = "crc error";
            profileVersion = -1;
            fwVersion = -1;
            hwVersion = -1;
            feature = -1;
            appearance = -1;
            fw2Version = -1;
        }
    }

    public static int getInt(byte[] data, int from, int len) {
        int ret = 0;
        for (int i = 0; i < len; ++i) {
            ret |= (data[from + i] & 255) << i * 8;
        }
        return ret;
    }

    private int getInt(byte[] data, int from) {
        return getInt(data, from, 4);
    }

    public int getFirmwareVersion() {
        return fwVersion;
    }

    public int getHeartrateFirmwareVersion() {
        if (test1AHRMode) {
            return fwVersion;
        }
        return fw2Version;
    }

    public int getProfileVersion() {
        return profileVersion;
    }

    public void setTest1AHRMode(boolean enableTestMode) {
        test1AHRMode = enableTestMode;
    }

    public boolean supportsHeartrate() {
        return isMili1S() || (test1AHRMode && isMili1A());
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", profileVersion=" + profileVersion +
                ", fwVersion=" + fwVersion +
                ", hwVersion=" + hwVersion +
                ", feature=" + feature +
                ", appearance=" + appearance +
                ", fw2Version (hr)=" + fw2Version +
                '}';
    }

    public boolean isMili1() {
        return hwVersion == 2 || (feature == 0 && appearance == 0 && hwVersion == 8 && fw2Version == -1);
    }

    public boolean isMili1A() {
        return feature == 5 && appearance == 0 || feature == 0 && hwVersion == 208;
    }

    public boolean isMili1S() {
        // TODO: this is probably not quite correct, but hopefully sufficient for early 1S support
        return (feature == 4 && appearance == 0) || hwVersion == 4;
    }

    public boolean isAmazFit() {
        return hwVersion == 6;
    }

    public String getHwVersion() {
        if (isMili1()) {
            return MiBandConst.MI_1;
        }
        if (isMili1A()) {
            return MiBandConst.MI_1A;
        }
        if (isMili1S()) {
            return MiBandConst.MI_1S;
        }
        if (isAmazFit()) {
            return MiBandConst.MI_AMAZFIT;
        }
        return "?";
    }
}
