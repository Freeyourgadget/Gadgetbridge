/*  Copyright (C) 2023 Johannes Krude

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Locale;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.TextStyle;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600.CasioGWB5600DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600.CasioGWB5600TimeZone;

public class InitOperation extends AbstractBTLEOperation<CasioGWB5600DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(InitOperation.class);

    private final TransactionBuilder builder;
    private final CasioGWB5600DeviceSupport support;
    private List<byte[]> responses = new LinkedList<byte[]>();

    public InitOperation(CasioGWB5600DeviceSupport support, TransactionBuilder builder) {
        super(support);
        this.support = support;
        this.builder = builder;
        builder.setCallback(this);
    }

    @Override
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        throw new UnsupportedOperationException("This IS the initialization class, you cannot call this method");
    }

    @Override
    protected void doPerform() {//throws IOException {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.notify(getCharacteristic(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID), true);
        for (int i = 1; i < 6; i++) {
            if (i%2 == 1)
                support.writeAllFeaturesRequest(builder, CasioGWB5600TimeZone.dstWatchStateRequest(i-1));

            support.writeAllFeaturesRequest(builder, CasioGWB5600TimeZone.dstSettingRequest(i));
            support.writeAllFeaturesRequest(builder, CasioGWB5600TimeZone.worldCityRequest(i));
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if (characteristicUUID.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID) && data.length > 0 &&
            (data[0] == CasioConstants.characteristicToByte.get("CASIO_DST_WATCH_STATE") ||
             data[0] == CasioConstants.characteristicToByte.get("CASIO_DST_SETTING") ||
             data[0] == CasioConstants.characteristicToByte.get("CASIO_WORLD_CITY"))) {
            responses.add(data);
            if (responses.size() == 13) {
                TransactionBuilder builder = createTransactionBuilder("setClocks");
                setClocks(builder);
                builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
                builder.setCallback(null);
                builder.queue(support.getQueue());
                operationFinished();
            }
            return true;
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    private void setClocks(TransactionBuilder builder) {
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
                support.writeAllFeatures(builder, CasioGWB5600TimeZone.dstWatchStateBytes(i, timezones[i], i+1, timezones[i+1]));
            support.writeAllFeatures(builder, timezones[i].dstSettingBytes(i));
            support.writeAllFeatures(builder, timezones[i].worldCityBytes(i));
        }
        support.writeCurrentTime(builder, ZonedDateTime.ofInstant(now, tz));
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
    }

}
