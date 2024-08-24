/*  Copyright (C) 2018-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Petr Vaněk

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data.
 */
public abstract class AbstractFetchOperation extends AbstractHuamiOperation {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFetchOperation.class);

    protected BluetoothGattCharacteristic characteristicActivityData;
    protected BluetoothGattCharacteristic characteristicFetch;

    protected Calendar startTimestamp;

    protected int fetchCount;
    protected byte lastPacketCounter;
    protected int expectedDataLength = 0;
    protected final ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);

    protected boolean operationValid = true; // to mark operation failed midway (eg. out of sync)

    public AbstractFetchOperation(final HuamiSupport support) {
        super(support);
    }

    @Override
    protected void enableNeededNotifications(final TransactionBuilder builder, final boolean enable) {
        if (!enable) {
            // dynamically enabled, but always disabled on finish
            builder.notify(characteristicFetch, false);
            builder.notify(characteristicActivityData, false);
        }
    }

    @Override
    protected void doPerform() throws IOException {
        startFetching();
    }

    protected void startFetching() throws IOException {
        expectedDataLength = 0;
        lastPacketCounter = -1;

        final TransactionBuilder builder = performInitialized(getName());
        if (fetchCount == 0) {
            builder.add(new SetDeviceBusyAction(getDevice(), taskDescription(), getContext()));
        }
        fetchCount++;

        // TODO: this probably returns null when device is not connected/initialized yet!
        characteristicActivityData = getCharacteristic(HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA);
        builder.notify(characteristicActivityData, false);

        characteristicFetch = getCharacteristic(HuamiService.UUID_UNKNOWN_CHARACTERISTIC4);
        builder.notify(characteristicFetch, true);

        startFetching(builder);
        builder.queue(getQueue());
    }

    /**
     * A task description, to display in notifications and device card.
     */
    protected abstract String taskDescription();

    protected abstract void startFetching(TransactionBuilder builder);

    protected abstract String getLastSyncTimeKey();

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA.equals(characteristicUUID)) {
            handleActivityData(characteristic.getValue());
            return true;
        } else if (HuamiService.UUID_UNKNOWN_CHARACTERISTIC4.equals(characteristicUUID)) {
            handleActivityMetadata(characteristic.getValue());
            return true;
        } else {
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    /**
     * Handles the finishing of fetching the activity. This signals the actual end of this operation.
     */
    protected final void onOperationFinished() {
        final AbstractFetchOperation nextFetchOperation = getSupport().getNextFetchOperation();
        if (nextFetchOperation != null) {
            LOG.debug("Performing next operation {}", nextFetchOperation.getName());
            try {
                nextFetchOperation.perform();
                return;
            } catch (final IOException e) {
                GB.toast(
                        getContext(),
                        "Failed to run next fetch operation",
                        Toast.LENGTH_SHORT,
                        GB.ERROR, e
                );
                return;
            }
        }

        LOG.debug("All operations finished");

        GB.updateTransferNotification(null, "", false, 100, getContext());
        GB.signalActivityDataFinish(getDevice());
        operationFinished();
        unsetBusy();
    }

    /**
     * Validates that the received data has the expected checksum. Only
     * relevant for ZeppOsSupport devices.
     *
     * @param crc32 the expected checksum
     * @return whether the checksum was valid
     */
    protected boolean validChecksum(int crc32) {
        return crc32 == CheckSums.getCRC32(buffer.toByteArray());
    }

    protected abstract boolean processBufferedData();

    protected void handleActivityData(final byte[] value) {
        LOG.debug("{} data: {}", getName(), Logging.formatBytes(value));

        if (!isOperationRunning()) {
            LOG.error("ignoring {} notification because operation is not running. Data length: {}", getName(), value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if ((byte) (lastPacketCounter + 1) == value[0]) {
            // TODO we should handle skipped or repeated bytes more gracefully
            lastPacketCounter++;
            bufferActivityData(value);
        } else {
            GB.toast("Error " + getName() + ", invalid package counter: " + value[0] + ", last was: " + lastPacketCounter, Toast.LENGTH_LONG, GB.ERROR);
            operationValid = false;
        }
    }

    protected void bufferActivityData(byte[] value) {
        buffer.write(value, 1, value.length - 1); // skip the counter
    }

    protected void startFetching(final TransactionBuilder builder, final byte fetchType, final GregorianCalendar sinceWhen) {
        final HuamiSupport support = getSupport();
        byte[] fetchBytes = BLETypeConversions.join(new byte[]{
                        HuamiService.COMMAND_ACTIVITY_DATA_START_DATE,
                        fetchType},
                support.getTimeBytes(sinceWhen, support.getFetchOperationsTimeUnit()));
        builder.write(characteristicFetch, fetchBytes);
    }

    private void handleActivityMetadata(byte[] value) {
        if (value.length < 3) {
            LOG.warn("Activity metadata too short: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        if (value[0] != HuamiService.RESPONSE) {
            LOG.warn("Activity metadata not a response: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        switch (value[1]) {
            case HuamiService.COMMAND_ACTIVITY_DATA_START_DATE:
                handleStartDateResponse(value);
                return;
            case HuamiService.COMMAND_FETCH_DATA:
                handleFetchDataResponse(value);
                return;
            case HuamiService.COMMAND_ACK_ACTIVITY_DATA:
                LOG.info("Got reply to COMMAND_ACK_ACTIVITY_DATA");
                onOperationFinished();
                return;
            default:
                LOG.warn("Unexpected activity metadata: {}", Logging.formatBytes(value));
                onOperationFinished();
        }
    }

    private void handleStartDateResponse(final byte[] value) {
        if (value[2] != HuamiService.SUCCESS) {
            LOG.warn("Start date unsuccessful response: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        // it's 16 on the MB7, with a 0 at the end
        if (value.length != 15 && (value.length != 16 && value[15] != 0x00)) {
            LOG.warn("Start date response length: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        // the third byte (0x01 on success) = ?
        // the 4th - 7th bytes represent the number of bytes/packets to expect, excluding the counter bytes
        expectedDataLength = BLETypeConversions.toUint32(Arrays.copyOfRange(value, 3, 7));

        // last 8 bytes are the start date
        Calendar startTimestamp = getSupport().fromTimeBytes(Arrays.copyOfRange(value, 7, value.length));

        if (expectedDataLength == 0) {
            LOG.info("No data to fetch since {}", startTimestamp.getTime());
            sendAck(true);
            // do not finish the operation - do it in the ack response
            return;
        }

        setStartTimestamp(startTimestamp);
        LOG.info("Will transfer {} packets since {}", expectedDataLength, startTimestamp.getTime());

        GB.updateTransferNotification(taskDescription(),
                getContext().getString(R.string.FetchActivityOperation_about_to_transfer_since,
                        DateFormat.getDateTimeInstance().format(startTimestamp.getTime())), true, 0, getContext());

        // Trigger the actual data fetch
        final TransactionBuilder step2builder = createTransactionBuilder(getName() + " Step 2");
        step2builder.notify(characteristicActivityData, true);
        step2builder.write(characteristicFetch, new byte[]{HuamiService.COMMAND_FETCH_DATA});
        try {
            performImmediately(step2builder);
        } catch (final IOException e) {
            GB.toast(getContext(), "Error starting fetch step 2: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            onOperationFinished();
        }
    }

    private void handleFetchDataResponse(final byte[] value) {
        if (value[2] != HuamiService.SUCCESS) {
            LOG.warn("Fetch data unsuccessful response: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        if (value.length != 3 && value.length != 7) {
            LOG.warn("Fetch data unexpected metadata length: {}", Logging.formatBytes(value));
            onOperationFinished();
            return;
        }

        if (value.length == 7 && !validChecksum(BLETypeConversions.toUint32(value, 3))) {
            LOG.warn("Data checksum invalid");
            // If we're on Zepp OS, ack but keep data on device
            if (isZeppOs()) {
                sendAck(true);
                // do not finish the operation - do it in the ack response
                return;
            }
            onOperationFinished();
            return;
        }

        final boolean success = operationValid && processBufferedData();

        final boolean keepActivityDataOnDevice = !success || HuamiCoordinator.getKeepActivityDataOnDevice(getDevice().getAddress());
        if (isZeppOs() || !keepActivityDataOnDevice) {
            sendAck(keepActivityDataOnDevice);
            // do not finish the operation - do it in the ack response
            return;
        }

        onOperationFinished();
    }

    protected void sendAck(final boolean keepDataOnDevice) {
        final byte[] ackBytes;

        if (isZeppOs()) {
            LOG.debug("Sending ack, keepDataOnDevice = {}", keepDataOnDevice);

            // 0x01 to ACK, mark as saved on phone (drop from band)
            // 0x09 to ACK, but keep it marked as not saved
            // If 0x01 is sent, detailed information seems to be discarded, and is not sent again anymore
            final byte ackByte = (byte) (keepDataOnDevice ? 0x09 : 0x01);
            ackBytes = new byte[]{HuamiService.COMMAND_ACK_ACTIVITY_DATA, ackByte};
        } else {
            LOG.debug("Sending ack, simple");
            ackBytes = new byte[]{HuamiService.COMMAND_ACK_ACTIVITY_DATA};
        }

        try {
            final TransactionBuilder builder = createTransactionBuilder(getName() + " end");
            builder.write(characteristicFetch, ackBytes);
            performImmediately(builder);
        } catch (final IOException e) {
            LOG.error("Failed to send ack", e);
        }
    }

    private void setStartTimestamp(final Calendar startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    protected Calendar getLastStartTimestamp() {
        return startTimestamp;
    }

    protected void saveLastSyncTimestamp(@NonNull final GregorianCalendar timestamp) {
        final SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).edit();
        editor.putLong(getLastSyncTimeKey(), timestamp.getTimeInMillis());
        editor.apply();
    }

    protected GregorianCalendar getLastSuccessfulSyncTime() {
        final long timeStampMillis = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getLong(getLastSyncTimeKey(), 0);
        if (timeStampMillis != 0) {
            GregorianCalendar calendar = BLETypeConversions.createCalendar();
            calendar.setTimeInMillis(timeStampMillis);
            return calendar;
        }
        final GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -100);
        return calendar;
    }

    protected boolean isZeppOs() {
        return getSupport() instanceof ZeppOsSupport;
    }
}
