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

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.MoyoungConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FetchDataOperation extends AbstractBTLEOperation<MoyoungDeviceSupport> {

    private static final Logger LOG = LoggerFactory.getLogger(FetchDataOperation.class);

    private boolean[] receivedSteps = new boolean[3];
    private boolean[] receivedSleep = new boolean[3];
    private boolean receivedTrainingData = false;

    private MoyoungPacketIn packetIn = new MoyoungPacketIn();

    public FetchDataOperation(MoyoungDeviceSupport support) {
        super(support);
    }

    @Override
    protected void prePerform() {
        getDevice().setBusyTask(getContext().getString(R.string.busy_task_fetch_activity_data));
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        TransactionBuilder builder = performInitialized("FetchDataOperation");
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[] { MoyoungConstants.ARG_SYNC_YESTERDAY_SLEEP }));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[] { MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_SLEEP }));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(MoyoungConstants.CMD_SYNC_SLEEP, new byte[0]));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[] { MoyoungConstants.ARG_SYNC_YESTERDAY_STEPS }));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP, new byte[] { MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_STEPS }));
        builder.read(getCharacteristic(MoyoungConstants.UUID_CHARACTERISTIC_STEPS));
        getSupport().sendPacket(builder, MoyoungPacketOut.buildPacket(MoyoungConstants.CMD_QUERY_MOVEMENT_HEART_RATE, new byte[] { }));
        builder.queue(getQueue());

        updateProgressAndCheckFinish();
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!isOperationRunning())
        {
            LOG.error("onCharacteristicRead but operation is not running!");
        }
        else
        {
            UUID charUuid = characteristic.getUuid();
            if (charUuid.equals(MoyoungConstants.UUID_CHARACTERISTIC_STEPS)) {
                byte[] data = characteristic.getValue();
                Log.i("TODAY STEPS", "data: " + Logging.formatBytes(data));
                decodeSteps(0, data);
                return true;
            }
        }

        return super.onCharacteristicRead(gatt, characteristic, status);
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
        if (packetType == MoyoungConstants.CMD_SYNC_SLEEP) {
            Log.i("TODAY SLEEP", "data: " + Logging.formatBytes(payload));
            decodeSleep(0, payload);
            return true;
        }
        if (packetType == MoyoungConstants.CMD_SYNC_PAST_SLEEP_AND_STEP) {
            byte dataType = payload[0];
            byte[] data = new byte[payload.length - 1];
            System.arraycopy(payload, 1, data, 0, data.length);

            // NOTE: Does this seem swapped to you? That's because IT IS! I took the constant names
            //       from the official app, but as it turns out, the official app has a bug.
            //       (and yes, you can see that data from yesterday appears as two days ago
            //       in the app itself and all past data is getting messed up because of it)

            if (dataType == MoyoungConstants.ARG_SYNC_YESTERDAY_STEPS) {
                Log.i("2 DAYS AGO STEPS", "data: " + Logging.formatBytes(data));
                decodeSteps(2, data);
                return true;
            }
            else if (dataType == MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_STEPS) {
                Log.i("YESTERDAY STEPS", "data: " + Logging.formatBytes(data));
                decodeSteps(1, data);
                return true;
            }
            else if (dataType == MoyoungConstants.ARG_SYNC_YESTERDAY_SLEEP) {
                Log.i("2 DAYS AGO SLEEP", "data: " + Logging.formatBytes(data));
                decodeSleep(2, data);
                return true;
            }
            else if (dataType == MoyoungConstants.ARG_SYNC_DAY_BEFORE_YESTERDAY_SLEEP) {
                Log.i("YESTERDAY SLEEP", "data: " + Logging.formatBytes(data));
                decodeSleep(1, data);
                return true;
            }
        }
        if (packetType == MoyoungConstants.CMD_QUERY_MOVEMENT_HEART_RATE) {
            decodeTrainingData(payload);
            return true;
        }
        return false;
    }

    private void decodeSteps(int daysAgo, byte[] data)
    {
        getSupport().handleStepsHistory(daysAgo, data, false);
        receivedSteps[daysAgo] = true;
        updateProgressAndCheckFinish();
    }

    private void decodeSleep(int daysAgo, byte[] data)
    {
        getSupport().handleSleepHistory(daysAgo, data);
        receivedSleep[daysAgo] = true;
        updateProgressAndCheckFinish();
    }

    private void decodeTrainingData(byte[] data)
    {
        getSupport().handleTrainingData(data);
        receivedTrainingData = true;
        updateProgressAndCheckFinish();
    }

    private void updateProgressAndCheckFinish()
    {
        int count = 0;
        int total = receivedSteps.length + receivedSleep.length + 1;
        for(int i = 0; i < receivedSteps.length; i++)
            if (receivedSteps[i])
                ++count;
        for(int i = 0; i < receivedSleep.length; i++)
            if (receivedSleep[i])
                ++count;
        if (receivedTrainingData)
            ++count;
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 100 * count / total, getContext());
        if (count == total)
            operationFinished();
    }

    @Override
    protected void operationFinished() {
        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null && getDevice().isConnected()) {
            unsetBusy();
            GB.signalActivityDataFinish();
        }
    }
}
