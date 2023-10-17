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

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiPlaintextSupport extends XiaomiSupport {

    private static final Logger LOG = LoggerFactory.getLogger(XiaomiPlaintextSupport.class);

    private static final UUID UUID_CHARACTERISTIC_MAIN_READ = UUID.fromString("16186f01-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_MAIN_WRITE = UUID.fromString("16186f02-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_ACTIVITY_DATA = UUID.fromString("16186f03-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_DATA_UPLOAD = UUID.fromString("16186f04-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_UNK5 = UUID.fromString("16186f05-0000-1000-8000-00807f9b34fb");

    public XiaomiPlaintextSupport() {
        super();
        addSupportedService(UUID.fromString("16186f00-0000-1000-8000-00807f9b34fb"));
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        final BluetoothGattCharacteristic btCharacteristicCommandRead = getCharacteristic(UUID_CHARACTERISTIC_MAIN_READ);
        final BluetoothGattCharacteristic btCharacteristicCommandWrite = getCharacteristic(UUID_CHARACTERISTIC_MAIN_WRITE);
        final BluetoothGattCharacteristic btCharacteristicActivityData = getCharacteristic(UUID_CHARACTERISTIC_ACTIVITY_DATA);
        final BluetoothGattCharacteristic btCharacteristicDataUpload = getCharacteristic(UUID_CHARACTERISTIC_DATA_UPLOAD);

        if (btCharacteristicCommandRead == null || btCharacteristicCommandWrite == null) {
            LOG.warn("Characteristics are null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
            return builder;
        }

        // TODO move this initialization to upstream class
        this.characteristicCommandRead = new XiaomiCharacteristic(this, btCharacteristicCommandRead, authService);
        this.characteristicCommandRead.setEncrypted(false);
        this.characteristicCommandRead.setHandler(this::handleCommandBytes);
        this.characteristicCommandWrite = new XiaomiCharacteristic(this, btCharacteristicCommandWrite, authService);
        this.characteristicCommandWrite.setEncrypted(false);
        this.characteristicActivityData = new XiaomiCharacteristic(this, btCharacteristicActivityData, authService);
        this.characteristicActivityData.setHandler(healthService.getActivityFetcher()::addChunk);
        this.characteristicActivityData.setEncrypted(false);
        this.characteristicDataUpload = new XiaomiCharacteristic(this, btCharacteristicDataUpload, authService);
        this.characteristicDataUpload.setEncrypted(false);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        enableNotifications(builder, true);
        builder.requestMtu(247);

        String userId = getUserId(gbDevice);
        authService.startClearTextHandshake(builder, userId);

        return builder;
    }

    private void enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_MAIN_WRITE), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_MAIN_READ), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_ACTIVITY_DATA), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_DATA_UPLOAD), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_UNK5), enable);
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
