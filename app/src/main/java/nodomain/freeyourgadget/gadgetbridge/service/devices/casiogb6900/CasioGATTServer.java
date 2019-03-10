/*  Copyright (C) 2018-2019 Andreas Böhler, Daniele Gobbetti
    based on code from BlueWatcher, https://github.com/masterjc/bluewatcher

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casiogb6900;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.devices.casiogb6900.CasioGB6900Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

class CasioGATTServer extends BluetoothGattServerCallback {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGATTServer.class);

    private Context mContext;
    private BluetoothGattServer mBluetoothGattServer;
    private CasioGB6900DeviceSupport mDeviceSupport = null;
    private final GBDeviceEventMusicControl musicCmd = new GBDeviceEventMusicControl();

    CasioGATTServer(Context context, CasioGB6900DeviceSupport deviceSupport) {
        mContext = context;
        mDeviceSupport = deviceSupport;
    }

    public void setContext(Context ctx) {
        mContext = ctx;
    }

    boolean initialize() {
        if(mContext == null) {
            return false;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return false;
        }
        mBluetoothGattServer = bluetoothManager.openGattServer(mContext, this);
        if (mBluetoothGattServer == null) {
            return false;
        }

        BluetoothGattService casioGATTService = new BluetoothGattService(CasioGB6900Constants.WATCH_CTRL_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic bluetoothgGATTCharacteristic = new BluetoothGattCharacteristic(CasioGB6900Constants.KEY_CONTAINER_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        bluetoothgGATTCharacteristic.setValue(new byte[0]);

        BluetoothGattCharacteristic bluetoothgGATTCharacteristic2 = new BluetoothGattCharacteristic(CasioGB6900Constants.NAME_OF_APP_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        bluetoothgGATTCharacteristic2.setValue(CasioGB6900Constants.MUSIC_MESSAGE.getBytes());

        BluetoothGattDescriptor bluetoothGattDescriptor = new BluetoothGattDescriptor(CasioGB6900Constants.CCC_DESCRIPTOR_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        bluetoothgGATTCharacteristic2.addDescriptor(bluetoothGattDescriptor);

        casioGATTService.addCharacteristic(bluetoothgGATTCharacteristic);
        casioGATTService.addCharacteristic(bluetoothgGATTCharacteristic2);
        mBluetoothGattServer.addService(casioGATTService);

        return true;
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

        if (!characteristic.getUuid().equals(CasioGB6900Constants.NAME_OF_APP_CHARACTERISTIC_UUID)) {
            LOG.warn("unexpected read request");
            return;
        }

        LOG.info("will send response to read request from device: " + device.getAddress());

        if (!this.mBluetoothGattServer.sendResponse(device, requestId, 0, offset, CasioGB6900Constants.MUSIC_MESSAGE.getBytes())) {
            LOG.warn("error sending response");
        }
    }
    private GBDeviceEventMusicControl.Event parse3Button(int button) {
        GBDeviceEventMusicControl.Event event;
        switch(button) {
            case 3:
                event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            case 2:
                event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
            case 1:
                event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                break;
            default:
                LOG.warn("Unhandled button received: " + button);
                event =  GBDeviceEventMusicControl.Event.UNKNOWN;
        }
        return event;
    }

    private GBDeviceEventMusicControl.Event parse2Button(int button) {
        GBDeviceEventMusicControl.Event event;
        switch(button) {
            case 2:
                event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
                break;
            case 1:
                event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            default:
                LOG.warn("Unhandled button received: " + button);
                event =  GBDeviceEventMusicControl.Event.UNKNOWN;
        }
        return event;
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

        if (!characteristic.getUuid().equals(CasioGB6900Constants.KEY_CONTAINER_CHARACTERISTIC_UUID)) {
            LOG.warn("unexpected write request");
            return;
        }

        if(mDeviceSupport == null) {
            LOG.warn("mDeviceSupport is null, did initialization complete?");
            return;
        }

        if((value[0] & 0x03) == 0) {
            int button = value[1] & 0x0f;
            LOG.info("Button pressed: " + button);
            switch(mDeviceSupport.getModel())
            {
                case MODEL_CASIO_5600B:
                    musicCmd.event = parse2Button(button);
                    break;
                case MODEL_CASIO_6900B:
                    musicCmd.event = parse3Button(button);
                    break;
                case MODEL_CASIO_GENERIC:
                    musicCmd.event = parse3Button(button);
                    break;
                default:
                    LOG.warn("Unhandled device");
                    return;
            }
            mDeviceSupport.evaluateGBDeviceEvent(musicCmd);
            mDeviceSupport.evaluateGBDeviceEvent(musicCmd);
        }
        else {
            LOG.info("received from device: " + value.toString());
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {

        LOG.info("Connection state change for device: " + device.getAddress() + "  status = " + status + " newState = " + newState);
        if (newState == BluetoothGattServer.STATE_DISCONNECTED) {
            LOG.info("CASIO GATT server noticed disconnect.");
        }
        if (newState == BluetoothGattServer.STATE_CONNECTED) {
            GBDevice.State devState = mDeviceSupport.getDevice().getState();
            Intent deviceCommunicationServiceIntent = new Intent(mContext, DeviceCommunicationService.class);
            if (devState.equals(GBDevice.State.WAITING_FOR_RECONNECT) || devState.equals(GBDevice.State.NOT_CONNECTED)) {
                LOG.info("Forcing re-connect because GATT server has been reconnected.");
                deviceCommunicationServiceIntent.setAction(DeviceService.ACTION_CONNECT);
                deviceCommunicationServiceIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(deviceCommunicationServiceIntent);
                //PendingIntent reconnectPendingIntent = PendingIntent.getService(mContext, 2, deviceCommunicationServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                //builder.addAction(R.drawable.ic_notification, context.getString(R.string.controlcenter_connect), reconnectPendingIntent);
            }
        }
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

        LOG.info("onDescriptorWriteRequest() notifications enabled = " + (value[0] == 1));
        if (!this.mBluetoothGattServer.sendResponse(device, requestId, 0, offset, value)) {
            LOG.warn("onDescriptorWriteRequest() error sending response!");
        }
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        LOG.info("onServiceAdded() status = " + status + " service = " + service.getUuid());
    }

    @Override
    public void onNotificationSent(BluetoothDevice bluetoothDevice, int status) {
        LOG.info("onNotificationSent() status = " + status + " to device " + bluetoothDevice.getAddress());
    }

    void close() {
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
            mBluetoothGattServer = null;
        }
    }

}
