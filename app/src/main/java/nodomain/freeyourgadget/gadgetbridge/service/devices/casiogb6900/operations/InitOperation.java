/*  Copyright (C) 2016-2018 Andreas Böhler

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casiogb6900.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.casiogb6900.CasioGB6900Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casiogb6900.CasioGB6900DeviceSupport;

public class InitOperation extends AbstractBTLEOperation<CasioGB6900DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(InitOperation.class);

    private final TransactionBuilder builder;
    private byte[] mBleSettings = null;


    public InitOperation(CasioGB6900DeviceSupport support, TransactionBuilder builder) {
        super(support);
        this.builder = builder;
        builder.setGattCallback(this);
    }

    @Override
    protected void doPerform() throws IOException {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        TransactionBuilder builder = getSupport().createTransactionBuilder("readBleSettings");
        builder.setGattCallback(this);
        builder.read(getCharacteristic(CasioGB6900Constants.CASIO_SETTING_FOR_BLE_CHARACTERISTIC_UUID));
        getSupport().performImmediately(builder);
    }

    @Override
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        throw new UnsupportedOperationException("This IS the initialization class, you cannot call this method");
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private void configureBleSettings() {
        // These values seem to improve connection stability _on my phone_
        // Maybe they should be configurable?
        int slaveLatency = 2;
        int connInterval = 300;

        mBleSettings[5] = (byte)(connInterval & 0xff);
        mBleSettings[6] = (byte)((connInterval >> 8) & 0xff);
        mBleSettings[7] = (byte)(slaveLatency & 0xff);
        mBleSettings[8] = (byte)((slaveLatency >> 8) & 0xff);

        mBleSettings[9] = 0; // Setting for Disconnect!?
    }

    private void writeBleSettings() {
        try {
            TransactionBuilder builder = getSupport().createTransactionBuilder("writeBleInit");
            builder.setGattCallback(this);
            builder.write(getCharacteristic(CasioGB6900Constants.CASIO_SETTING_FOR_BLE_CHARACTERISTIC_UUID), mBleSettings);
            getSupport().performImmediately(builder);
        } catch(IOException e) {
            LOG.error("Error writing BLE settings: " + e.getMessage());
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if(data.length == 0)
            return true;

        if(characteristicUUID.equals(CasioGB6900Constants.CASIO_SETTING_FOR_BLE_CHARACTERISTIC_UUID)) {
            mBleSettings = data;
            String str = "Read Casio Setting for BLE: ";
            for(int i=0; i<data.length; i++) {
                str += String.format("0x%1x ", data[i]);
            }
            /* Definition of parameters - for future reference */

            // data[0]; // BLE alert for call, mail and other
            // data[1]; // BLE alert for Calendar
            // data[2]; // BLE alert for SNS
            // data[3]; // BLE alert for vibration and alarm
            // data[4]; // BLE alert for animation
            // data[5]; // Connection Interval
            // data[6]; // Connection Interval
            // data[7]; // Slave Latency
            // data[8]; // Slave Latency

            // Alert definitions:
            // 0 = Off
            // 1 = Sound
            // 2 = Vibration
            // 3 = Sound and Vibration
            //int callAlert = (data[0] >> 6) & 0x03;
            //LOG.info("Call Alert: " + callAlert);
            //int mailAlert = (data[0] >> 2) & 0x03;
            //LOG.info("Mail Alert: " + mailAlert);
            //int snsAlert = (data[2] >> 4) & 0x03;
            //LOG.info("SNS Alert: " + snsAlert);
            //int calAlert = (data[1] >> 6) & 0x03;
            //LOG.info("Calendart Alert: " + calAlert);
            //int otherAlert = (data[0] & 0x03);
            //LOG.info("Other Alert: " + otherAlert);
            //int vibrationValue = (data[3] & 0x0f);
            //LOG.info("Vibration Value: " + vibrationValue);
            //int alarmValue = (data[3] >> 4) & 0x0f;
            // Vibration pattern; A = 0, B = 1, C = 2
            //LOG.info("Alarm Value: " + alarmValue);
            //int animationValue = data[4] & 0x40;
            // Length of Alarm, only 2, 5 and 10 possible
            //LOG.info("Animation Value: " + animationValue);
            // 0 = on
            // 64 = off
            //int useDisableMtuReqBit = data[4] & 0x08;
            // 8 = on
            // 0 = off!?
            //LOG.info("useDisableMtuReqBit: " + useDisableMtuReqBit);

            //int slaveLatency = ((data[7] & 0xff) | ((data[8] & 0xff) << 8));
            //int connInterval = ((data[5] & 0xff) | ((data[6] & 0xff) << 8));
            //LOG.info("Slave Latency: " + slaveLatency);
            //LOG.info("Connection Interval: " + connInterval);
            //LOG.info(str);

            configureBleSettings();
            writeBleSettings();
        }
        else {
            return super.onCharacteristicRead(gatt, characteristic, status);
        }

        return true;
    }
}
