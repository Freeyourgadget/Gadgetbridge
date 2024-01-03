/*  Copyright (C) 2023 José Rebelo, Yoran Vulker

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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiBleSupport extends XiaomiConnectionSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiBleSupport.class);

    private XiaomiCharacteristic characteristicCommandRead;
    private XiaomiCharacteristic characteristicCommandWrite;
    private XiaomiCharacteristic characteristicActivityData;
    private XiaomiCharacteristic characteristicDataUpload;

    private final XiaomiSupport mXiaomiSupport;

    final AbstractBTLEDeviceSupport commsSupport = new AbstractBTLEDeviceSupport(LOG) {
        @Override
        public boolean useAutoConnect() {
            return mXiaomiSupport.useAutoConnect();
        }

        @Override
        protected Set<UUID> getSupportedServices() {
            return XiaomiUuids.BLE_UUIDS.keySet();
        }

        @Override
        protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
            XiaomiUuids.XiaomiBleUuidSet uuidSet = null;
            BluetoothGattCharacteristic btCharacteristicCommandRead = null;
            BluetoothGattCharacteristic btCharacteristicCommandWrite = null;
            BluetoothGattCharacteristic btCharacteristicActivityData = null;
            BluetoothGattCharacteristic btCharacteristicDataUpload = null;

            // Attempt to find a known xiaomi service
            for (Map.Entry<UUID, XiaomiUuids.XiaomiBleUuidSet> xiaomiUuid : XiaomiUuids.BLE_UUIDS.entrySet()) {
                if (getSupportedServices().contains(xiaomiUuid.getKey())) {
                    LOG.debug("Found Xiaomi service: {}", xiaomiUuid.getKey());
                    uuidSet = xiaomiUuid.getValue();

                    btCharacteristicCommandRead = getCharacteristic(uuidSet.getCharacteristicCommandRead());
                    btCharacteristicCommandWrite = getCharacteristic(uuidSet.getCharacteristicCommandWrite());
                    btCharacteristicActivityData = getCharacteristic(uuidSet.getCharacteristicActivityData());
                    btCharacteristicDataUpload = getCharacteristic(uuidSet.getCharacteristicDataUpload());
                    if (btCharacteristicCommandRead == null) {
                        LOG.warn("btCharacteristicCommandRead characteristicc is null");
                        continue;
                    } else if (btCharacteristicCommandWrite == null) {
                        LOG.warn("btCharacteristicCommandWrite characteristicc is null");
                        continue;
                    } else if (btCharacteristicActivityData == null) {
                        LOG.warn("btCharacteristicActivityData characteristicc is null");
                        continue;
                    } else if (btCharacteristicDataUpload == null) {
                        LOG.warn("btCharacteristicDataUpload characteristicc is null");
                        continue;
                    }

                    break;
                }
            }

            if (uuidSet == null) {
                GB.toast(getContext(), "Failed to find known Xiaomi service", Toast.LENGTH_LONG, GB.ERROR);
                LOG.warn("Failed to find known Xiaomi service");
                builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.NOT_CONNECTED, getContext()));
                return builder;
            }

            // FIXME unsetDynamicState unsets the fw version, which causes problems..
            if (getDevice().getFirmwareVersion() == null && mXiaomiSupport.getCachedFirmwareVersion() != null) {
                getDevice().setFirmwareVersion(mXiaomiSupport.getCachedFirmwareVersion());
            }

            if (btCharacteristicCommandRead == null || btCharacteristicCommandWrite == null) {
                LOG.warn("Characteristics are null, will attempt to reconnect");
                builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
                return builder;
            }

            XiaomiBleSupport.this.characteristicCommandRead = new XiaomiCharacteristic(XiaomiBleSupport.this, btCharacteristicCommandRead, mXiaomiSupport.getAuthService());
            XiaomiBleSupport.this.characteristicCommandRead.setEncrypted(uuidSet.isEncrypted());
            XiaomiBleSupport.this.characteristicCommandRead.setChannelHandler(mXiaomiSupport::handleCommandBytes);
            XiaomiBleSupport.this.characteristicCommandWrite = new XiaomiCharacteristic(XiaomiBleSupport.this, btCharacteristicCommandWrite, mXiaomiSupport.getAuthService());
            XiaomiBleSupport.this.characteristicCommandWrite.setEncrypted(uuidSet.isEncrypted());
            XiaomiBleSupport.this.characteristicActivityData = new XiaomiCharacteristic(XiaomiBleSupport.this, btCharacteristicActivityData, mXiaomiSupport.getAuthService());
            XiaomiBleSupport.this.characteristicActivityData.setChannelHandler(mXiaomiSupport.getHealthService().getActivityFetcher()::addChunk);
            XiaomiBleSupport.this.characteristicActivityData.setEncrypted(uuidSet.isEncrypted());
            XiaomiBleSupport.this.characteristicDataUpload = new XiaomiCharacteristic(XiaomiBleSupport.this, btCharacteristicDataUpload, mXiaomiSupport.getAuthService());
            XiaomiBleSupport.this.characteristicDataUpload.setEncrypted(uuidSet.isEncrypted());
            XiaomiBleSupport.this.characteristicDataUpload.setIncrementNonce(false);

            builder.requestMtu(247);
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
            builder.notify(btCharacteristicCommandWrite, true);
            builder.notify(btCharacteristicCommandRead, true);
            builder.notify(btCharacteristicActivityData, true);
            builder.notify(btCharacteristicDataUpload, true);

            if (uuidSet.isEncrypted()) {
                mXiaomiSupport.getAuthService().startEncryptedHandshake(XiaomiBleSupport.this, builder);
            } else {
                mXiaomiSupport.getAuthService().startClearTextHandshake(XiaomiBleSupport.this, builder);
            }

            return builder;
        }

        @Override
        public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            if (super.onCharacteristicChanged(gatt, characteristic)) {
                return true;
            }

            final UUID characteristicUUID = characteristic.getUuid();
            final byte[] value = characteristic.getValue();

            if (characteristicCommandRead.getCharacteristicUUID().equals(characteristicUUID)) {
                characteristicCommandRead.onCharacteristicChanged(value);
                return true;
            } else if (characteristicCommandWrite.getCharacteristicUUID().equals(characteristicUUID)) {
                characteristicCommandWrite.onCharacteristicChanged(value);
                return true;
            } else if (characteristicActivityData.getCharacteristicUUID().equals(characteristicUUID)) {
                characteristicActivityData.onCharacteristicChanged(value);
                return true;
            } else if (characteristicDataUpload.getCharacteristicUUID().equals(characteristicUUID)) {
                characteristicDataUpload.onCharacteristicChanged(value);
                return true;
            }

            LOG.warn("Unhandled characteristic changed: {} {}", characteristicUUID, GB.hexdump(value));
            return false;
        }

        @Override
        public boolean getImplicitCallbackModify() {
            return mXiaomiSupport.getImplicitCallbackModify();
        }
    };

    public XiaomiBleSupport(final XiaomiSupport xiaomiSupport) {
        this.mXiaomiSupport = xiaomiSupport;
    }

    public void onAuthSuccess() {
        characteristicCommandRead.reset();
        characteristicCommandWrite.reset();
        characteristicActivityData.reset();
        characteristicDataUpload.reset();
    }

    @Override
    public void setContext(GBDevice device, BluetoothAdapter adapter, Context context) {
        this.commsSupport.setContext(device, adapter, context);
    }

    @Override
    public void disconnect() {
        this.commsSupport.disconnect();
    }

    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        if (this.characteristicCommandWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("characteristicCommandWrite is null!");
            return;
        }

        this.characteristicCommandWrite.write(taskName, command.toByteArray());
    }

    @Override
    public void sendDataChunk(String taskName, byte[] chunk, @Nullable XiaomiCharacteristic.SendCallback callback) {
        if (this.characteristicDataUpload == null) {
            LOG.warn("characteristicDataUpload is null!");
            return;
        }

        this.characteristicDataUpload.write(taskName, chunk, callback);
    }

    /**
     * Realistically, this function should only be used during auth, as we must schedule the command after
     * notifications were enabled on the characteristics, and for that we need the builder to guarantee the
     * order.
     */
    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        if (this.characteristicCommandWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("characteristicCommandWrite is null!");
            return;
        }

        this.characteristicCommandWrite.write(builder, command.toByteArray());
    }

    public TransactionBuilder createTransactionBuilder(String taskName) {
        return commsSupport.createTransactionBuilder(taskName);
    }

    public BtLEQueue getQueue() {
        return commsSupport.getQueue();
    }

    @Override
    public void onUploadProgress(int textRsrc, int progressPercent) {
        try {
            final TransactionBuilder builder = commsSupport.createTransactionBuilder("send data upload progress");
            builder.add(new SetProgressAction(
                    commsSupport.getContext().getString(textRsrc),
                    true,
                    progressPercent,
                    commsSupport.getContext()
            ));
            builder.queue(commsSupport.getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }

    @Override
    public boolean connect() {
        return commsSupport.connect();
    }

    @Override
    public void dispose() {
        commsSupport.dispose();
    }
}
