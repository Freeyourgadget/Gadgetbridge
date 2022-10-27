/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.AbstractGattListenerWriteAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public abstract class AbstractFetchOperation extends AbstractHuamiOperation {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFetchOperation.class);

    protected byte lastPacketCounter;
    int fetchCount;
    protected BluetoothGattCharacteristic characteristicActivityData;
    protected BluetoothGattCharacteristic characteristicFetch;
    Calendar startTimestamp;
    int expectedDataLength = 0;

    public AbstractFetchOperation(HuamiSupport support) {
        super(support);
    }

    @Override
    protected void enableNeededNotifications(TransactionBuilder builder, boolean enable) {
        if (!enable) {
            // dynamically enabled, but always disabled on finish
            builder.notify(characteristicFetch, enable);
            builder.notify(characteristicActivityData, enable);
        }
    }

    @Override
    protected void doPerform() throws IOException {
        startFetching();
    }

    protected void startFetching() throws IOException {
        expectedDataLength = 0;
        lastPacketCounter = -1;

        TransactionBuilder builder = performInitialized(getName());
        if (fetchCount == 0) {
            builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
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

    protected abstract void startFetching(TransactionBuilder builder);

    protected abstract String getLastSyncTimeKey();

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA.equals(characteristicUUID)) {
            handleActivityNotif(characteristic.getValue());
            return true;
        } else if (HuamiService.UUID_UNKNOWN_CHARACTERISTIC4.equals(characteristicUUID)) {
            handleActivityMetadata(characteristic.getValue());
            return true;
        } else {
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    /**
     * Handles the finishing of fetching the activity.
     * @param success whether fetching was successful
     * @return whether handling the activity fetch finish was successful
     */
    @CallSuper
    protected boolean handleActivityFetchFinish(boolean success) {
        GB.updateTransferNotification(null, "", false, 100, getContext());
        operationFinished();
        unsetBusy();
        return true;
    }

    /**
     * Validates that the received data has the expected checksum. Only
     * relevant for Huami2021Support devices.
     *
     * @param crc32 the expected checksum
     * @return whether the checksum was valid
     */
    protected abstract boolean validChecksum(int crc32);

    /**
     * Method to handle the incoming activity data.
     * There are two kind of messages we currently know:
     * - the first one is 11 bytes long and contains metadata (how many bytes to expect, when the data starts, etc.)
     * - the second one is 20 bytes long and contains the actual activity data
     * <p/>
     * The first message type is parsed by this method, for every other length of the value param, bufferActivityData is called.
     *
     * @param value
     */
    protected abstract void handleActivityNotif(byte[] value);

    protected abstract void bufferActivityData(byte[] value);

    protected void startFetching(TransactionBuilder builder, byte fetchType, GregorianCalendar sinceWhen) {
        final String taskName = StringUtils.ensureNotNull(builder.getTaskName());
        final HuamiSupport support = getSupport();
        final boolean isHuami2021 = support instanceof Huami2021Support;
        byte[] fetchBytes = BLETypeConversions.join(new byte[]{
                        HuamiService.COMMAND_ACTIVITY_DATA_START_DATE,
                        fetchType},
                support.getTimeBytes(sinceWhen, support.getFetchOperationsTimeUnit()));
        builder.add(new AbstractGattListenerWriteAction(getQueue(), characteristicFetch, fetchBytes) {
            @Override
            protected boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                UUID characteristicUUID = characteristic.getUuid();
                if (HuamiService.UUID_UNKNOWN_CHARACTERISTIC4.equals(characteristicUUID)) {
                    byte[] value = characteristic.getValue();

                    if (ArrayUtils.equals(value, HuamiService.RESPONSE_ACTIVITY_DATA_START_DATE_SUCCESS, 0)) {
                        handleActivityMetadata(value);
                        if (expectedDataLength == 0 && isHuami2021) {
                            // Nothing to receive, if we try to fetch data it will fail
                            sendAck2021(true);
                        } else {
                            TransactionBuilder newBuilder = createTransactionBuilder(taskName + " Step 2");
                            newBuilder.notify(characteristicActivityData, true);
                            newBuilder.write(characteristicFetch, new byte[]{HuamiService.COMMAND_FETCH_DATA});
                            try {
                                performImmediately(newBuilder);
                            } catch (IOException ex) {
                                GB.toast(getContext(), "Error fetching debug logs: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
                            }
                        }
                        return true;
                    } else {
                        handleActivityMetadata(value);
                    }
                }
                return false;
            }
        });
    }

    private void handleActivityMetadata(byte[] value) {
        if (value.length < 3) {
            LOG.warn("Activity metadata too short: {}", Logging.formatBytes(value));
            handleActivityFetchFinish(false);
            return;
        }

        if (value[0] != HuamiService.RESPONSE) {
            LOG.warn("Activity metadata not a response: {}", Logging.formatBytes(value));
            handleActivityFetchFinish(false);
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
                // ignore, this is just the reply to the COMMAND_ACK_ACTIVITY_DATA
                LOG.info("Got reply to COMMAND_ACK_ACTIVITY_DATA");
                return;
            default:
                LOG.warn("Unexpected activity metadata: {}", Logging.formatBytes(value));
                handleActivityFetchFinish(false);
        }
    }

    private void handleStartDateResponse(final byte[] value) {
        if (value[2] != HuamiService.SUCCESS) {
            LOG.warn("Start date unsuccessful response: {}", Logging.formatBytes(value));
            handleActivityFetchFinish(false);
            return;
        }

        // it's 16 on the MB7, with a 0 at the end
        if (value.length != 15 && (value.length != 16 && value[15] != 0x00)) {
            LOG.warn("Start date response length: {}", Logging.formatBytes(value));
            handleActivityFetchFinish(false);
            return;
        }

        // the third byte (0x01 on success) = ?
        // the 4th - 7th bytes represent the number of bytes/packets to expect, excluding the counter bytes
        expectedDataLength = BLETypeConversions.toUint32(Arrays.copyOfRange(value, 3, 7));

        // last 8 bytes are the start date
        Calendar startTimestamp = getSupport().fromTimeBytes(Arrays.copyOfRange(value, 7, value.length));

        if (expectedDataLength == 0) {
            LOG.info("No data to fetch since {}", startTimestamp.getTime());
            handleActivityFetchFinish(true);
            return;
        }

        setStartTimestamp(startTimestamp);
        LOG.info("Will transfer {} packets since {}", expectedDataLength, startTimestamp.getTime());

        GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data),
                getContext().getString(R.string.FetchActivityOperation_about_to_transfer_since,
                        DateFormat.getDateTimeInstance().format(startTimestamp.getTime())), true, 0, getContext());
    }

    private void handleFetchDataResponse(final byte[] value) {
        if (value[2] != HuamiService.SUCCESS) {
            LOG.warn("Fetch data unsuccessful response: {}", Logging.formatBytes(value));
            handleActivityFetchFinish(false);
            return;
        }

        if (value.length != 3 && value.length != 7) {
            LOG.warn("Fetch data unexpected metadata length: {}", Logging.formatBytes(value));
            handleActivityFetchFinish(false);
            return;
        }

        if (value.length == 7 && !validChecksum(BLETypeConversions.toUint32(value, 3))) {
            LOG.warn("Data checksum invalid");
            handleActivityFetchFinish(false);
            sendAck2021(true);
            return;
        }

        boolean handleFinishSuccess;
        try {
            handleFinishSuccess = handleActivityFetchFinish(true);
        } catch (final Exception e) {
            LOG.warn("Failed to handle activity fetch finish", e);
            handleFinishSuccess = false;
        }

        final boolean keepActivityDataOnDevice = HuamiCoordinator.getKeepActivityDataOnDevice(getDevice().getAddress());

        sendAck2021(keepActivityDataOnDevice || !handleFinishSuccess);
    }

    private void sendAck2021(final boolean keepDataOnDevice) {
        if (!(getSupport() instanceof Huami2021Support)) {
            return;
        }

        // 0x01 to ACK, mark as saved on phone (drop from band)
        // 0x09 to ACK, but keep it marked as not saved
        // If 0x01 is sent, detailed information seems to be discarded, and is not sent again anymore
        final byte ackByte = (byte) (keepDataOnDevice ? 0x09 : 0x01);

        try {
            final TransactionBuilder builder = performInitialized(getName() + " end");
            builder.write(characteristicFetch, new byte[]{HuamiService.COMMAND_ACK_ACTIVITY_DATA, ackByte});
            performImmediately(builder);
        } catch (final IOException e) {
            LOG.error("Ending failed", e);
        }
    }

    private void setStartTimestamp(Calendar startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    Calendar getLastStartTimestamp() {
        return startTimestamp;
    }

    void saveLastSyncTimestamp(@NonNull GregorianCalendar timestamp) {
        SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).edit();
        editor.putLong(getLastSyncTimeKey(), timestamp.getTimeInMillis());
        editor.apply();
    }

    protected GregorianCalendar getLastSuccessfulSyncTime() {
        long timeStampMillis = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getLong(getLastSyncTimeKey(), 0);
        if (timeStampMillis != 0) {
            GregorianCalendar calendar = BLETypeConversions.createCalendar();
            calendar.setTimeInMillis(timeStampMillis);
            return calendar;
        }
        GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -100);
        return calendar;
    }
}
