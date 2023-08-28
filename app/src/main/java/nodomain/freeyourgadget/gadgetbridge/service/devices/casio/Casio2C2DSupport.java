/*  Copyright (C) 2023-2024 Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;


// this class is for those Casio watches which request reads on the 2C characteristic and write on the 2D characteristic

public abstract class Casio2C2DSupport extends CasioSupport {

    public static final byte FEATURE_CURRENT_TIME = 0x09;
    public static final byte FEATURE_ALERT_LEVEL = 0x0a;
    public static final byte FEATURE_BLE_FEATURES = 0x10;
    public static final byte FEATURE_SETTING_FOR_BLE = 0x11;
    public static final byte FEATURE_SETTING_FOR_BASIC = 0x13;
    public static final byte FEATURE_SETTING_FOR_ALM = 0x15;
    public static final byte FEATURE_SETTING_FOR_ALM2 = 0x16;
    public static final byte FEATURE_VERSION_INFORMATION = 0x20;
    public static final byte FEATURE_APP_INFORMATION = 0x22;
    public static final byte FEATURE_WATCH_NAME = 0x23;
    public static final byte FEATURE_MODULE_ID = 0x26;
    public static final byte FEATURE_WATCH_CONDITION = 0x28;
    public static final byte FEATURE_DST_WATCH_STATE = 0x1d;
    public static final byte FEATURE_DST_SETTING = 0x1e;
    public static final byte FEATURE_WORLD_CITY = 0x1f;
    public static final byte FEATURE_CURRENT_TIME_MANAGER = 0x39;
    public static final byte FEATURE_CONNECTION_PARAMETER_MANAGER = 0x3a;
    public static final byte FEATURE_ADVERTISE_PARAMETER_MANAGER = 0x3b;
    public static final byte FEATURE_SETTING_FOR_TARGET_VALUE = 0x43;
    public static final byte FEATURE_SETTING_FOR_USER_PROFILE = 0x45;
    public static final byte FEATURE_SERVICE_DISCOVERY_MANAGER = 0x47;


    public Casio2C2DSupport(Logger logger) {
        super(logger);
    }

    public void writeAllFeatures(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public void writeAllFeaturesRequest(TransactionBuilder builder, byte[] arr) {
        builder.write(getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID), arr);
    }

    public void writeCurrentTime(TransactionBuilder builder, ZonedDateTime time) {
        byte[] arr = new byte[11];
        arr[0] = FEATURE_CURRENT_TIME;
        byte[] tmp = prepareCurrentTime(time);
        System.arraycopy(tmp, 0, arr, 1, 10);

        writeAllFeatures(builder, arr);
    }

}
