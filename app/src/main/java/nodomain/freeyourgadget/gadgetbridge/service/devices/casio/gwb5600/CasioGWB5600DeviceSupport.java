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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600;

import java.util.UUID;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;

import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;

import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.Casio2C2DSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600.CasioGWB5600TimeZone;

public class CasioGWB5600DeviceSupport extends Casio2C2DSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGWB5600DeviceSupport.class);

    public CasioGWB5600DeviceSupport() {
        super(LOG);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean connectFirstTime() {
    // remove this workaround in case Gadgetbridge fixes
    // https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/3216
        setAutoReconnect(true);
        return connect();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        // remove this workaround once Gadgetbridge does discovery on initial pairing
        if (getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID) == null ||
           getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID) == null) {
            LOG.info("Reconnecting to discover characteristics");
            disconnect();
            connect();
            return builder;
        }

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        // which button was pressed?
        requestFeature(builder, new FeatureRequest(FEATURE_BLE_FEATURES), data -> {
            if (data.length > 8 && data[8] == CasioConstants.CONNECT_FIND) {
                setInitialized();
            } else {
                requestWorldClocks();
            }
        });

        return builder;
    }

    private void requestWorldClocks() {
        TransactionBuilder builder = createTransactionBuilder("requestWorldClocks");
        HashSet<FeatureRequest> requests = new HashSet();

        for (byte i = 0; i < 6; i++) {
            requests.addAll(CasioGWB5600TimeZone.requests(i));
        }

        requestFeatures(builder, requests, responses -> {
                TransactionBuilder clockBuilder = createTransactionBuilder("setClocks");
                setClocks(clockBuilder, responses);
                clockBuilder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
                clockBuilder.queue(getQueue());
        });
        builder.queue(getQueue());
    }

    private void setClocks(TransactionBuilder builder, Map<FeatureRequest, byte[]> responses) {
        ZoneId tz = ZoneId.systemDefault();
        Instant now = Instant.now().plusSeconds(2);
        CasioGWB5600TimeZone[] timezones = {
            CasioGWB5600TimeZone.fromZoneId(tz, now, tz.getDisplayName(TextStyle.SHORT, Locale.getDefault())),
            CasioGWB5600TimeZone.fromWatchResponses(responses, 1),
            CasioGWB5600TimeZone.fromWatchResponses(responses, 2),
            CasioGWB5600TimeZone.fromWatchResponses(responses, 3),
            CasioGWB5600TimeZone.fromWatchResponses(responses, 4),
            CasioGWB5600TimeZone.fromWatchResponses(responses, 5),
        };
        for (int i = 5; i >= 0; i--) {
            if (i%2 == 0)
                writeAllFeatures(builder, CasioGWB5600TimeZone.dstWatchStateBytes(i, timezones[i], i+1, timezones[i+1]));
            writeAllFeatures(builder, timezones[i].dstSettingBytes(i));
            writeAllFeatures(builder, timezones[i].worldCityBytes(i));
        }
        writeCurrentTime(builder, ZonedDateTime.ofInstant(now, tz));
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            if(data[0] == FEATURE_ALERT_LEVEL) {
                GBDeviceEventFindPhone event = new GBDeviceEventFindPhone();
                if(data[1] == 0x02) {
                    event.event = GBDeviceEventFindPhone.Event.START_VIBRATE;
                } else {
                    event.event = GBDeviceEventFindPhone.Event.STOP;
                }
                evaluateGBDeviceEvent(event);
                return true;
            }
        }
        return super.onCharacteristicChanged(gatt, characteristic);
    }

}
