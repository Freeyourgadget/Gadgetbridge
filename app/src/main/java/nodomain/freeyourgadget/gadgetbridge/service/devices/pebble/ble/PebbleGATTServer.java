package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble;

import android.bluetooth.BluetoothDevice;
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

import nodomain.freeyourgadget.gadgetbridge.util.GB;

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

        BluetoothGattService pebbleGATTService = new BluetoothGattService(SERVER_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        pebbleGATTService.addCharacteristic(new BluetoothGattCharacteristic(READ_CHARACTERISTICS, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));

        writeCharacteristics = new BluetoothGattCharacteristic(WRITE_CHARACTERISTICS, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE);

        writeCharacteristics.addDescriptor(new BluetoothGattDescriptor(CHARACTERISTICS_CONFIGURATION_DESCRIPTOR, BluetoothGattDescriptor.PERMISSION_WRITE));
        pebbleGATTService.addCharacteristic(writeCharacteristics);
        mBluetoothGattServer.addService(pebbleGATTService);


        final BluetoothGattService badbadService = new BluetoothGattService(SERVER_SERVICE_BADBAD, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        badbadService.addCharacteristic(new BluetoothGattCharacteristic(SERVER_SERVICE_BADBAD, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
        mBluetoothGattServer.addService(badbadService);
        return true;
    }

    synchronized void sendDataToPebble(byte[] data) {
        LOG.info("send data to pebble " + GB.hexdump(data, 0, -1));
        writeCharacteristics.setValue(data.clone());


        mBluetoothGattServer.notifyCharacteristicChanged(mBtDevice, writeCharacteristics, false);

        try {
            Thread.sleep(100); // FIXME: bad bad, I mean BAAAD
        } catch (InterruptedException ignore) {
        }
    }

    synchronized private void sendAckToPebble(int serial) {
        LOG.info("send ack to pebble for serial " + serial);

        writeCharacteristics.setValue(new byte[]{(byte) (((serial << 3) | 1) & 0xff)});

        mBluetoothGattServer.notifyCharacteristicChanged(mBtDevice, writeCharacteristics, false);

        try {
            Thread.sleep(100); // FIXME: bad bad, I mean BAAAD
        } catch (InterruptedException ignore) {
        }
    }

    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
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
        if (!characteristic.getUuid().equals(WRITE_CHARACTERISTICS)) {
            LOG.warn("unexpected write request");
            return;
        }
        LOG.info("write request: offset = " + offset + " value = " + GB.hexdump(value, 0, -1));
        int header = value[0] & 0xff;
        int command = header & 7;
        int serial = header >> 3;
        if (command == 0x01) {
            LOG.info("got ACK for serial = " + serial);
        }
        if (command == 0x02) { // some request?
            LOG.info("got command 0x02");
            if (value.length > 1) {
                sendDataToPebble(new byte[]{0x03, 0x19, 0x19}); // no we dont know what that means
                mPebbleLESupport.createPipedInputReader(); // FIXME: maybe not here
            } else {
                sendDataToPebble(new byte[]{0x03}); // no we dont know what that means
            }
        } else if (command == 0) { // normal package
            LOG.info("got PPoGATT package serial = " + serial + " sending ACK");

            sendAckToPebble(serial);

            mPebbleLESupport.writeToPipedOutputStream(value, 1, value.length - 1);
        }
    }

    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        LOG.info("Connection state change for device: " + device.getAddress() + "  status = " + status + " newState = " + newState);
    }

    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

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
    }

    public void onNotificationSent(BluetoothDevice bluetoothDevice, int status) {
        //LOG.info("onNotificationSent() status = " + status + " to device " + mmBtDevice.getAddress());
    }

    void close() {
        mBluetoothGattServer.cancelConnection(mBtDevice);
        mBluetoothGattServer.clearServices();
        mBluetoothGattServer.close();
    }
}
