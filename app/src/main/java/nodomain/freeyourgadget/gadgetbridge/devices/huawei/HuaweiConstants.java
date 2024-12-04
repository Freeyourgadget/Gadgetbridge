/*  Copyright (C) 2024 Damien Gaignon, Vitalii Tomin

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.util.UUID;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport.BASE_UUID;

public final class HuaweiConstants {

    public static final UUID UUID_SERVICE_HUAWEI_SERVICE = UUID.fromString(String.format(BASE_UUID, "FE86"));
    public static final UUID UUID_CHARACTERISTIC_HUAWEI_WRITE = UUID.fromString(String.format(BASE_UUID, "FE01"));
    public static final UUID UUID_CHARACTERISTIC_HUAWEI_READ = UUID.fromString(String.format(BASE_UUID, "FE02"));
    public static final UUID UUID_SERVICE_HUAWEI_SDP = UUID.fromString("82FF3820-8411-400C-B85A-55BDB32CF060");

    public static final String GROUP_ID = "7B0BC0CBCE474F6C238D9661C63400B797B166EA7849B3A370FC73A9A236E989";
    public static final byte[] KEY_TYPE = new byte[]{0x00, 0x07};

    public static final byte HUAWEI_MAGIC = 0x5A;

    public static final byte PROTOCOL_VERSION = 0x02;

    public static final int TAG_RESULT = 127;
    public static final byte[] RESULT_SUCCESS = new byte[]{0x00, 0x01, (byte)0x86, (byte)0xA0};
    public static final int RESULT_SUCCESS_INT = 0x186a0;

    public static class CryptoTags {
        public static final int encryption = 124;
        public static final int initVector = 125;
        public static final int cipherText = 126;
    }

    public static final String HO_BAND3_NAME = "honor band 3-";
    public static final String HO_BAND4_NAME = "honor band 4-";
    public static final String HO_BAND5_NAME = "honor band 5-";
    public static final String HO_BAND6_NAME = "honor band 6-";
    public static final String HO_BAND7_NAME = "honor band 7-";
    public static final String HO_MAGICWATCH2_NAME = "honor magicwatch 2-";
    public static final String HO_WATCHGS3_NAME = "honor watch gs 3-";
    public static final String HO_WATCHGSPRO_NAME = "honor watch gs pro-";
    public static final String HU_BAND3E_NAME = "huawei band 3e-";
    public static final String HU_BAND4E_NAME = "huawei band 4e-";
    public static final String HU_BAND6_NAME = "huawei band 6-";
    public static final String HU_WATCHGT_NAME = "huawei watch gt-";
    public static final String HU_BAND2_NAME = "huawei band 2-";
    public static final String HU_BAND3_NAME = "huawei band 3-";
    public static final String HU_BAND4_NAME = "huawei band 4-";
    public static final String HU_BAND2PRO_NAME = "huawei band 2 pro-";
    public static final String HU_BAND3PRO_NAME = "huawei band 3 pro-";
    public static final String HU_BAND4PRO_NAME = "huawei band 4 pro-";
    public static final String HU_WATCHGT2_NAME = "huawei watch gt 2-";
    public static final String HU_WATCHGT2E_NAME = "huawei watch gt 2e-";
    public static final String HU_WATCHGT2PRO_NAME = "huawei watch gt 2 pro-";
    public static final String HU_TALKBANDB6_NAME = "huawei b6-";
    public static final String HU_BAND7_NAME = "huawei band 7-";
    public static final String HU_BAND8_NAME = "huawei band 8-";
    public static final String HU_BAND9_NAME = "huawei band 9-";
    public static final String HU_WATCHD2_NAME = "huawei watch d2-";
    public static final String HU_WATCHGT3_NAME = "huawei watch gt 3-";
    public static final String HU_WATCHGT3SE_NAME = "huawei watch gt 3 se-";
    public static final String HU_WATCHGT3PRO_NAME = "huawei watch gt 3 pro-";
    public static final String HU_WATCHGTRUNNER_NAME = "huawei watch gt runner-";
    public static final String HU_WATCHGTCYBER_NAME = "huawei watch gt cyber-";
    public static final String HU_WATCH3_NAME = "huawei watch 3-";
    public static final String HU_WATCH3PRO_NAME = "huawei watch 3 pro-";
    public static final String HU_WATCHGT4_NAME = "huawei watch gt 4-";
    public static final String HU_WATCHGT5_NAME = "huawei watch gt 5-";
    public static final String HU_WATCHGT5PRO_NAME = "huawei watch gt 5 pro-";
    public static final String HU_WATCHFIT_NAME = "huawei watch fit-";
    public static final String HU_WATCHFIT2_NAME = "huawei watch fit 2-";
    public static final String HU_WATCHFIT3_NAME = "huawei watch fit 3-";
    public static final String HU_WATCHULTIMATE_NAME = "huawei watch ultimate-";
    public static final String HU_WATCH4_NAME = "huawei watch 4-";
    public static final String HU_WATCH4PRO_NAME = "huawei watch 4 pro-";

    public static final String PREF_HUAWEI_ADDRESS = "huawei_address";
    public static final String PREF_HUAWEI_WORKMODE = "workmode";
    public static final String PREF_HUAWEI_TRUSLEEP = "trusleep";
    public static final String PREF_HUAWEI_ACCOUNT = "huawei_account";
    public static final String PREF_HUAWEI_DND_LIFT_WRIST_TYPE = "dnd_lift_wrist_type"; // SharedPref for 0x01 0x1D
    public static final String PREF_HUAWEI_DEBUG_REQUEST = "debug_huawei_request";
    public static final String PREF_HUAWEI_CONTINUOUS_SKIN_TEMPERATURE_MEASUREMENT = "continuous_skin_temperature_measurement";

    public static final String PKG_NAME = "com.huawei.devicegroupmanage";
}
