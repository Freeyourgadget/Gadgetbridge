/*  Copyright (C) 2023 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import android.content.SharedPreferences;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class XiaomiPlaintextSupport extends XiaomiSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiPlaintextSupport.class);

    public static final UUID UUID_SERVICE = UUID.fromString("16186f00-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_MAIN_READ = UUID.fromString("16186f01-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_MAIN_WRITE = UUID.fromString("16186f02-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_ACTIVITY_DATA = UUID.fromString("16186f03-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_DATA_UPLOAD = UUID.fromString("16186f04-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_UNK5 = UUID.fromString("16186f05-0000-1000-8000-00807f9b34fb");

    public XiaomiPlaintextSupport() {
        super();
        addSupportedService(UUID_SERVICE);
    }

    @Override
    protected boolean isEncrypted() {
        return false;
    }

    @Override
    protected UUID getCharacteristicCommandRead() {
        return UUID_CHARACTERISTIC_MAIN_READ;
    }

    @Override
    protected UUID getCharacteristicCommandWrite() {
        return UUID_CHARACTERISTIC_MAIN_WRITE;
    }

    @Override
    protected UUID getCharacteristicActivityData() {
        return UUID_CHARACTERISTIC_ACTIVITY_DATA;
    }

    @Override
    protected UUID getCharacteristicDataUpload() {
        return UUID_CHARACTERISTIC_DATA_UPLOAD;
    }

    @Override
    protected void startAuthentication(final TransactionBuilder builder) {
        final String userId = getUserId(gbDevice);
        authService.startClearTextHandshake(builder, userId);
    }

    protected static String getUserId(final GBDevice device) {
        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        final String authKey = sharedPrefs.getString("authkey", null);
        if (StringUtils.isNotBlank(authKey)) {
            return authKey;
        }

        return "0000000000";
    }
}
