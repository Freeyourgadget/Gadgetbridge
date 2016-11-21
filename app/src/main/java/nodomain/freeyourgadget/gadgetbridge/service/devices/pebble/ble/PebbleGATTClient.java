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

    private final String mBtDeviceAddress;
    private final BluetoothDevice mBtDevice;
    private final Context mContext;
    private final PebbleLESupport mPebbleLESupport;

    private boolean oldPebble = false;
    private boolean doPairing = true;
    private boolean removeBond = false;
    private BluetoothGatt mBluetoothGatt;

    PebbleGATTClient(PebbleLESupport pebbleLESupport, Context context, BluetoothDevice btDevice) {
        mContext = context;
        mBtDevice = btDevice;
        mPebbleLESupport = pebbleLESupport;
        mBtDeviceAddress = btDevice.getAddress();
    }

    boolean initialize() {
        connectToPebble(mBtDevice);
        return true;
    }

    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!gatt.getDevice().getAddress().equals(mBtDeviceAddress)) {
            LOG.info("onCharacteristicChanged() unexpected device: " + gatt.getDevice().getAddress() + " , expected: " + mBtDeviceAddress);
            return;
        }
        if (characteristic.getUuid().equals(MTU_CHARACTERISTIC)) {
            int newMTU = characteristic.getIntValue(FORMAT_UINT16, 0);
            LOG.info("Pebble requested MTU = " + newMTU);
        } else {
            LOG.info("onCharacteristicChanged()" + characteristic.getUuid().toString() + " " + GB.hexdump(characteristic.getValue(), 0, -1));
        }
    }

    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!gatt.getDevice().getAddress().equals(mBtDeviceAddress)) {
            LOG.info("onCharacteristicRead() unexpected device: " + gatt.getDevice().getAddress() + " , expected: " + mBtDeviceAddress);
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
        if (!gatt.getDevice().getAddress().equals(mBtDeviceAddress)) {
            LOG.info("onConnectionStateChange() unexpected device: " + gatt.getDevice().getAddress() + " , expected: " + mBtDeviceAddress);
            return;
        }
        LOG.info("onConnectionStateChange() status = " + status + " newState = " + newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            LOG.info("calling discoverServices()");
            gatt.discoverServices();
        }
        else if (newState == BluetoothGatt.STATE_DISCONNECTED){
            mPebbleLESupport.close();
        }
    }

    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!gatt.getDevice().getAddress().equals(mBtDeviceAddress)) {
            LOG.info("onCharacteristcsWrite unexpected device: " + gatt.getDevice().getAddress() + " , expected: " + mBtDeviceAddress);
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
        }
    }

    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor bluetoothGattDescriptor, int status) {
        if (!gatt.getDevice().getAddress().equals(mBtDeviceAddress)) {
            LOG.info("onDescriptorWrite() unexpected device: " + gatt.getDevice().getAddress() + " , expected: " + mBtDeviceAddress);
            return;
        }
        LOG.info("onDescriptorWrite() status=" + status);

        UUID CHARACTERISTICUUID = bluetoothGattDescriptor.getCharacteristic().getUuid();

        if (CHARACTERISTICUUID.equals(CONNECTION_PARAMETERS_CHARACTERISTIC)) {
            subscribeToConnectivity(gatt);
        } else if (CHARACTERISTICUUID.equals(CONNECTIVITY_CHARACTERISTIC)) {
            subscribeToMTU(gatt);
        } else if (CHARACTERISTICUUID.equals(MTU_CHARACTERISTIC)) {
            setMTU(gatt);
        }
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (!gatt.getDevice().getAddress().equals(mBtDeviceAddress)) {
            LOG.info("onServicesDiscovered() unexpected device: " + gatt.getDevice().getAddress() + " , expected: " + mBtDeviceAddress);
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
                    characteristic.setValue(new byte[]{1});
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
        //mBtDevice.createBond();
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

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
}
