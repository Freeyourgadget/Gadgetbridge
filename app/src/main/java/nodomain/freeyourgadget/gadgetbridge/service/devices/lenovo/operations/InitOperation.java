/*  Copyright (C) 2018-2019 maxirnilian

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lenovo.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus.WatchXPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.watch9.Watch9Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lenovo.watchxplus.WatchXPlusDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InitOperation extends AbstractBTLEOperation<WatchXPlusDeviceSupport>{

    private static final Logger LOG = LoggerFactory.getLogger(InitOperation.class);

    private final TransactionBuilder builder;
    private final boolean needsAuth;
    private final BluetoothGattCharacteristic cmdCharacteristic = getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_WRITE);
    private final BluetoothGattCharacteristic dbCharacteristic = getCharacteristic(WatchXPlusConstants.UUID_CHARACTERISTIC_DATABASE_READ);

    public InitOperation(boolean needsAuth, WatchXPlusDeviceSupport support, TransactionBuilder builder) {
        super(support);
        this.needsAuth = needsAuth;
        this.builder = builder;
        builder.setGattCallback(this);
    }

    @Override
    protected void doPerform() throws IOException {
        builder.notify(cmdCharacteristic, true).notify(dbCharacteristic, true);
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.AUTHENTICATING, getContext()));
            getSupport().authorizationRequest(builder, needsAuth);
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
            getSupport().initialize(builder);
            getSupport().performImmediately(builder);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (Watch9Constants.UUID_CHARACTERISTIC_WRITE.equals(characteristicUUID) && needsAuth) {
            try {
                byte[] value = characteristic.getValue();
                getSupport().logMessageContent(value);
                if (ArrayUtils.equals(value, Watch9Constants.RESP_AUTHORIZATION_TASK, 5) && value[8] == 0x01) {
                    TransactionBuilder builder = getSupport().createTransactionBuilder("authInit");
                    builder.setGattCallback(this);
                    builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
                    getSupport().initialize(builder).performImmediately(builder);
                } else {
                    return super.onCharacteristicChanged(gatt, characteristic);
                }
            } catch (Exception e) {
                GB.toast(getContext(), "Error authenticating Watch X Plus", Toast.LENGTH_LONG, GB.ERROR, e);
            }
            return true;
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }


}
