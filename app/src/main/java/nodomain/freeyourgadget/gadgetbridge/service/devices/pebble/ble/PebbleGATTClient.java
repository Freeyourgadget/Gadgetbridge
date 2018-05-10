/*  Copyright (C) 2016-2018 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;


class PebbleGATTClient extends BluetoothGattCallback {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleGATTClient.class);

    private static final UUID SERVICE_UUID = UUID.fromString("0000fed9-0000-1000-8000-00805f9b34fb");
    private static final UUID CONNECTIVITY_CHARACTERISTIC = UUID.fromString("00000001-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID PAIRING_TRIGGER_CHARACTERISTIC = UUID.fromString("00000002-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID MTU_CHARACTERISTIC = UUID.fromString("00000003-328e-0fbb-c642-1aa6699bdada");
    private static final UUID CONNECTION_PARAMETERS_CHARACTERISTIC = UUID.fromString("00000005-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID CHARACTERISTIC_CONFIGURATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //PPoGATT service (Pebble side)
    private static final UUID PPOGATT_SERVICE_UUID = UUID.fromString("30000003-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID PPOGATT_CHARACTERISTIC_READ = UUID.fromString("30000004-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID PPOGATT_CHARACTERISTIC_WRITE = UUID.fromString("30000006-328E-0FBB-C642-1AA6699BDADA");

    private BluetoothGattCharacteristic writeCharacteristics;

    private final Context mContext;
    private final PebbleLESupport mPebbleLESupport;

    private boolean oldPebble = false;
    private boolean doPairing = true;
    private boolean removeBond = false;
    private BluetoothGatt mBluetoothGatt;

    PebbleGATTClient(PebbleLESupport pebbleLESupport, Context context, BluetoothDevice btDevice) {
        mContext = context;
        mPebbleLESupport = pebbleLESupport;
        connectToPebble(btDevice);
    }

    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!mPebbleLESupport.isExpectedDevice(gatt.getDevice())) {
            return;
        }

        if (characteristic.getUuid().equals(MTU_CHARACTERISTIC)) {
            int newMTU = characteristic.getIntValue(FORMAT_UINT16, 0);
            LOG.info("Pebble requested MTU: " + newMTU);
            mPebbleLESupport.setMTU(newMTU);
        } else if (characteristic.getUuid().equals(PPOGATT_CHARACTERISTIC_READ)) {
            mPebbleLESupport.handlePPoGATTPacket(characteristic.getValue().clone());
        } else {
            LOG.info("onCharacteristicChanged()" + characteristic.getUuid().toString() + " " + GB.hexdump(characteristic.getValue(), 0, -1));
        }
    }

    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!mPebbleLESupport.isExpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onCharacteristicRead() status = " + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            LOG.info("onCharacteristicRead()" + characteristic.getUuid().toString() + " " + GB.hexdump(characteristic.getValue(), 0, -1));

            if (oldPebble) {
                subscribeToConnectivity(gatt);
            } else {
                subscribeToConnectionParams(gatt);
            }
        }
    }

    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (!mPebbleLESupport.isExpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onConnectionStateChange() status = " + status + " newState = " + newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            LOG.info("calling discoverServices()");
            gatt.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            mPebbleLESupport.close();
        }
    }

    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!mPebbleLESupport.isExpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onCharacteristicWrite() " + characteristic.getUuid());
        if (characteristic.getUuid().equals(PAIRING_TRIGGER_CHARACTERISTIC) || characteristic.getUuid().equals(CONNECTIVITY_CHARACTERISTIC)) {
            //mBtDevice.createBond(); // did not work when last tried

            if (oldPebble) {
                subscribeToConnectivity(gatt);
            } else {
                subscribeToConnectionParams(gatt);
            }
        } else if (characteristic.getUuid().equals(MTU_CHARACTERISTIC)) {
            if (GBApplication.isRunningLollipopOrLater()) {
                gatt.requestMtu(339);
            }
        }
    }

    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor bluetoothGattDescriptor, int status) {
        if (!mPebbleLESupport.isExpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onDescriptorWrite() status=" + status);

        UUID CHARACTERISTICUUID = bluetoothGattDescriptor.getCharacteristic().getUuid();

        if (CHARACTERISTICUUID.equals(CONNECTION_PARAMETERS_CHARACTERISTIC)) {
            subscribeToConnectivity(gatt);
        } else if (CHARACTERISTICUUID.equals(CONNECTIVITY_CHARACTERISTIC)) {
            subscribeToMTU(gatt);
        } else if (CHARACTERISTICUUID.equals(MTU_CHARACTERISTIC)) {
            if (mPebbleLESupport.clientOnly) {
                subscribeToPPoGATT(gatt);
            } else {
                setMTU(gatt);
            }
        } else if (CHARACTERISTICUUID.equals(PPOGATT_CHARACTERISTIC_READ)) {
            setMTU(gatt);
        }
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (!mPebbleLESupport.isExpectedDevice(gatt.getDevice())) {
            return;
        }

        LOG.info("onServicesDiscovered() status = " + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BluetoothGattCharacteristic connectionPararmharacteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTION_PARAMETERS_CHARACTERISTIC);
            oldPebble = connectionPararmharacteristic == null;

            if (oldPebble) {
                LOG.info("This seems to be an older le enabled pebble");
            }

            if (doPairing) {
                BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(PAIRING_TRIGGER_CHARACTERISTIC);
                if ((characteristic.getProperties() & PROPERTY_WRITE) != 0) {
                    LOG.info("This seems to be a >=4.0 FW Pebble, writing to pairing trigger");
                    // flags:
                    // 0 - always 1
                    // 1 - unknown
                    // 2 - always 0
                    // 3 - unknown, set on kitkat (seems to help to get a "better" pairing)
                    // 4 - unknown, set on some phones
                    if (mPebbleLESupport.clientOnly) {
                        characteristic.setValue(new byte[]{0x11}); // needed in clientOnly mode (TODO: try 0x19)
                    } else {
                        characteristic.setValue(new byte[]{0x09}); // I just keep this, because it worked
                    }
                    gatt.writeCharacteristic(characteristic);
                } else {
                    LOG.info("This seems to be some <4.0 FW Pebble, reading pairing trigger");
                    gatt.readCharacteristic(characteristic);
                }
            } else {
                if (oldPebble) {
                    subscribeToConnectivity(gatt);
                } else {
                    subscribeToConnectionParams(gatt);
                }
            }
        }
    }

    private void connectToPebble(BluetoothDevice btDevice) {
        if (removeBond) {
            try {
                Method m = btDevice.getClass()
                        .getMethod("removeBond", (Class[]) null);
                m.invoke(btDevice, (Object[]) null);
            } catch (Exception e) {
                LOG.warn(e.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        if (mBluetoothGatt != null) {
            this.close();
        }
        mBluetoothGatt = btDevice.connectGatt(mContext, false, this);
    }

    private void subscribeToConnectivity(BluetoothGatt gatt) {
        LOG.info("subscribing to connectivity characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTIVITY_CHARACTERISTIC).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        gatt.setCharacteristicNotification(gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTIVITY_CHARACTERISTIC), true);
    }

    private void subscribeToMTU(BluetoothGatt gatt) {
        LOG.info("subscribing to mtu characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        gatt.setCharacteristicNotification(gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC), true);
    }

    private void subscribeToConnectionParams(BluetoothGatt gatt) {
        LOG.info("subscribing to connection parameters characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTION_PARAMETERS_CHARACTERISTIC).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        gatt.setCharacteristicNotification(gatt.getService(SERVICE_UUID).getCharacteristic(CONNECTION_PARAMETERS_CHARACTERISTIC), true);
    }

    private void setMTU(BluetoothGatt gatt) {
        LOG.info("setting MTU");
        BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(MTU_CHARACTERISTIC);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        descriptor.setValue(new byte[]{0x0b, 0x01}); // unknown
        gatt.writeCharacteristic(characteristic);
    }

    private void subscribeToPPoGATT(BluetoothGatt gatt) {
        LOG.info("subscribing to PPoGATT read characteristic");
        BluetoothGattDescriptor descriptor = gatt.getService(PPOGATT_SERVICE_UUID).getCharacteristic(PPOGATT_CHARACTERISTIC_READ).getDescriptor(CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
        descriptor.setValue(new byte[]{1, 0});
        gatt.writeDescriptor(descriptor);
        gatt.setCharacteristicNotification(gatt.getService(PPOGATT_SERVICE_UUID).getCharacteristic(PPOGATT_CHARACTERISTIC_READ), true);
        writeCharacteristics = gatt.getService(PPOGATT_SERVICE_UUID).getCharacteristic(PPOGATT_CHARACTERISTIC_WRITE);
    }

    synchronized void sendDataToPebble(byte[] data) {
        writeCharacteristics.setValue(data.clone());
        mBluetoothGatt.writeCharacteristic(writeCharacteristics);
    }

    synchronized void sendAckToPebble(int serial) {
        writeCharacteristics.setValue(new byte[]{(byte) (((serial << 3) | 1) & 0xff)});
        mBluetoothGatt.writeCharacteristic(writeCharacteristics);
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}
