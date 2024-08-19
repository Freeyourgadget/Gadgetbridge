/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.moyoung;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSetting;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class QuerySettingsOperation extends AbstractBTLEOperation<MoyoungDeviceSupport> {

    private static final Logger LOG = LoggerFactory.getLogger(QuerySettingsOperation.class);

    private final MoyoungSetting[] settingsToQuery;
    private boolean[] received;

    private MoyoungPacketIn packetIn = new MoyoungPacketIn();

    public QuerySettingsOperation(MoyoungDeviceSupport support, MoyoungSetting[] settingsToQuery) {
        super(support);
        this.settingsToQuery = settingsToQuery;
    }

    public QuerySettingsOperation(MoyoungDeviceSupport support) {
        super(support);
        MoyoungDeviceCoordinator coordinator = (MoyoungDeviceCoordinator) DeviceHelper.getInstance().getCoordinator(getDevice());
        this.settingsToQuery = coordinator.getSupportedSettings();
    }

    @Override
    protected void prePerform() {
        getDevice().setBusyTask("Querying settings"); // mark as busy quickly to avoid interruptions from the outside
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        received = new boolean[settingsToQuery.length];
        TransactionBuilder builder = performInitialized("QuerySettingsOperation");
        for (MoyoungSetting setting : settingsToQuery)
        {
            if (setting.cmdQuery == -1)
                continue;

            getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(setting.cmdQuery, new byte[0]));
        }
        builder.queue(getQueue());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isOperationRunning())
        {
            LOG.error("onCharacteristicChanged but operation is not running!");
        }
        else
        {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(MoyoungConstants.UUID_CHARACTERISTIC_DATA_IN))
            {
                if (packetIn.putFragment(characteristic.getValue())) {
                    Pair<Byte, byte[]> packet = MoyoungPacketIn.parsePacket(packetIn.getPacket());
                    packetIn = new MoyoungPacketIn();
                    if (packet != null) {
                        byte packetType = packet.first;
                        byte[] payload = packet.second;

                        if (handlePacket(packetType, payload))
                            return true;
                    }
                }
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private boolean handlePacket(byte packetType, byte[] payload) {
        boolean handled = false;
        boolean receivedEverything = true;
        for(int i = 0; i < settingsToQuery.length; i++)
        {
            MoyoungSetting setting = settingsToQuery[i];
            if (setting.cmdQuery == -1)
                continue;
            if (setting.cmdQuery == packetType)
            {
                Object value = setting.decode(payload);
                Log.i("SETTING QUERY", setting.name + " = " + value.toString());
                received[i] = true;
                handled = true;
            }
            else if (!received[i])
                receivedEverything = false;
        }
        if (receivedEverything)
            operationFinished();

        return handled;
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null && getDevice().isConnected()) {
            unsetBusy();
        }
    }
}
