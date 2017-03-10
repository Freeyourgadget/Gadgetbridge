/*  Copyright (C) 2016-2017 Andreas Shimokawa, Daniele Gobbetti, Uwe Hermann

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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

class PebbleGATTServer extends BluetoothGattServerCallback {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleGATTServer.class);
    private static final UUID WRITE_CHARACTERISTICS = UUID.fromString("10000001-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID READ_CHARACTERISTICS = UUID.fromString("10000002-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID CHARACTERISTICS_CONFIGURATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVER_SERVICE = UUID.fromString("10000000-328E-0FBB-C642-1AA6699BDADA");
    private static final UUID SERVER_SERVICE_BADBAD = UUID.fromString("BADBADBA-DBAD-BADB-ADBA-BADBADBADBAD");
    private final BluetoothDevice mBtDevice;
    private final PebbleLESupport mPebbleLESupport;
    private Context mContext;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattCharacteristic writeCharacteristics;

    PebbleGATTServer(PebbleLESupport pebbleLESupport, Context context, BluetoothDevice btDevice) {
        mContext = context;
        mBtDevice = btDevice;
        mPebbleLESupport = pebbleLESupport;
    }

    boolean initialize() {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothGattServer = bluetoothManager.openGattServer(mContext, this);
        if (mBluetoothGattServer == null) {
            return false;
        }

        BluetoothGattService pebbleGATTService = new BluetoothGattService(SERVER_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        pebbleGATTService.addCharacteristic(new BluetoothGattCharacteristic(READ_CHARACTERISTICS, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));

        writeCharacteristics = new BluetoothGattCharacteristic(WRITE_CHARACTERISTICS, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE);

        writeCharacteristics.addDescriptor(new BluetoothGattDescriptor(CHARACTERISTICS_CONFIGURATION_DESCRIPTOR, BluetoothGattDescriptor.PERMISSION_WRITE));
        pebbleGATTService.addCharacteristic(writeCharacteristics);
        mBluetoothGattServer.addService(pebbleGATTService);

        return true;
    }

    synchronized void sendDataToPebble(byte[] data) {
        //LOG.info("send data to pebble " + GB.hexdump(data, 0, -1));
        writeCharacteristics.setValue(data.clone());

        mBluetoothGattServer.notifyCharacteristicChanged(mBtDevice, writeCharacteristics, false);
    }

    synchronized private void sendAckToPebble(int serial) {
        writeCharacteristics.setValue(new byte[]{(byte) (((serial << 3) | 1) & 0xff)});

        mBluetoothGattServer.notifyCharacteristicChanged(mBtDevice, writeCharacteristics, false);
    }

    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
        if (!mPebbleLESupport.isExpectedDevice(device)) {
            return;
        }

        if (!characteristic.getUuid().equals(READ_CHARACTERISTICS)) {
            LOG.warn("unexpected read request");
            return;
        }

        LOG.info("will send response to read request from device: " + device.getAddress());
        if (!this.mBluetoothGattServer.sendResponse(device, requestId, 0, offset, new byte[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})) {
            LOG.warn("error sending response");
        }
    }


    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        if (!mPebbleLESupport.isExpectedDevice(device)) {
            return;
        }

        if (!characteristic.getUuid().equals(WRITE_CHARACTERISTICS)) {
            LOG.warn("unexpected write request");
            return;
        }
        if (!mPebbleLESupport.mIsConnected) {
            mPebbleLESupport.mIsConnected = true;
            synchronized (mPebbleLESupport) {
                mPebbleLESupport.notify();
            }
        }
        //LOG.info("write request: offset = " + offset + " value = " + GB.hexdump(value, 0, -1));
        int header = value[0] & 0xff;
        int command = header & 7;
        int serial = header >> 3;
        if (command == 0x01) {
            LOG.info("got ACK for serial = " + serial);
            if (mPebbleLESupport.mPPAck != null) {
                mPebbleLESupport.mPPAck.countDown();
            } else {
                LOG.warn("mPPAck countdownlatch is not present but it probably should");
            }
        }
        if (command == 0x02) { // some request?
            LOG.info("got command 0x02");
            if (value.length > 1) {
                sendDataToPebble(new byte[]{0x03, 0x19, 0x19}); // no we don't know what that means
                mPebbleLESupport.createPipedInputReader(); // FIXME: maybe not here
            } else {
                sendDataToPebble(new byte[]{0x03}); // no we don't know what that means
            }
        } else if (command == 0) { // normal package
            LOG.info("got PPoGATT package serial = " + serial + " sending ACK");

            sendAckToPebble(serial);

            mPebbleLESupport.writeToPipedOutputStream(value, 1, value.length - 1);
        }
    }

    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        if (!mPebbleLESupport.isExpectedDevice(device)) {
            return;
        }

        LOG.info("Connection state change for device: " + device.getAddress() + "  status = " + status + " newState = " + newState);
        if (newState == BluetoothGattServer.STATE_DISCONNECTED) {
            mPebbleLESupport.close();
        }
    }

    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

        if (!mPebbleLESupport.isExpectedDevice(device)) {
            return;
        }

        if (!descriptor.getCharacteristic().getUuid().equals(WRITE_CHARACTERISTICS)) {
            LOG.warn("unexpected write request");
            return;
        }

        LOG.info("onDescriptorWriteRequest() notifications enabled = " + (value[0] == 1));
        if (!this.mBluetoothGattServer.sendResponse(device, requestId, 0, offset, value)) {
            LOG.warn("onDescriptorWriteRequest() error sending response!");
        }
    }

    public void onServiceAdded(int status, BluetoothGattService service) {
        LOG.info("onServiceAdded() status = " + status + " service = " + service.getUuid());
        if (status == BluetoothGatt.GATT_SUCCESS && service.getUuid().equals(SERVER_SERVICE)) {
            final BluetoothGattService badbadService = new BluetoothGattService(SERVER_SERVICE_BADBAD, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            badbadService.addCharacteristic(new BluetoothGattCharacteristic(SERVER_SERVICE_BADBAD, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
            mBluetoothGattServer.addService(badbadService);
        }
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        if (!mPebbleLESupport.isExpectedDevice(device)) {
            return;
        }

        LOG.info("Pebble requested mtu for server: " + mtu);
        mPebbleLESupport.setMTU(mtu);
    }

    public void onNotificationSent(BluetoothDevice bluetoothDevice, int status) {
        //LOG.info("onNotificationSent() status = " + status + " to device " + mmBtDevice.getAddress());
    }

    void close() {
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.cancelConnection(mBtDevice);
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
        }
    }
}
