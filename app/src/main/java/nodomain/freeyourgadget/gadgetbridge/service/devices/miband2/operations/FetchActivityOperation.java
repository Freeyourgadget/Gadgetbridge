/*  Copyright (C) 2016-2017 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.AbstractMiBand2Operation;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchActivityOperation extends AbstractMiBand2Operation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchActivityOperation.class);

    private List<MiBandActivitySample> samples = new ArrayList<>(60*24); // 1day per default

    private byte lastPacketCounter = -1;
    private Calendar startTimestamp;

    public FetchActivityOperation(MiBand2Support support) {
        super(support);
    }

    @Override
    protected void enableNeededNotifications(TransactionBuilder builder, boolean enable) {
        if (!enable) {
            // dynamically enabled, but always disabled on finish
            builder.notify(getCharacteristic(MiBand2Service.UUID_CHARACTERISTIC_5_ACTIVITY_DATA), enable);
        }
    }

    @Override
    protected void doPerform() throws IOException {
        TransactionBuilder builder = performInitialized("fetching activity data");
        getSupport().setLowLatency(builder);
        builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
        BluetoothGattCharacteristic characteristicFetch = getCharacteristic(MiBand2Service.UUID_UNKNOWN_CHARACTERISTIC4);
        builder.notify(characteristicFetch, true);
        BluetoothGattCharacteristic characteristicActivityData = getCharacteristic(MiBand2Service.UUID_CHARACTERISTIC_5_ACTIVITY_DATA);

        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        builder.write(characteristicFetch, BLETypeConversions.join(new byte[] { MiBand2Service.COMMAND_ACTIVITY_DATA_START_DATE, 0x01 }, getSupport().getTimeBytes(sinceWhen, TimeUnit.MINUTES)));
        builder.add(new WaitAction(1000)); // TODO: actually wait for the success-reply
        builder.notify(characteristicActivityData, true);
        builder.write(characteristicFetch, new byte[] { MiBand2Service.COMMAND_FETCH_ACTIVITY_DATA });
        builder.queue(getQueue());
    }

    private GregorianCalendar getLastSuccessfulSyncTime() {
        long timeStampMillis = GBApplication.getPrefs().getLong(getLastSyncTimeKey(), 0);
        if (timeStampMillis != 0) {
            GregorianCalendar calendar = BLETypeConversions.createCalendar();
            calendar.setTimeInMillis(timeStampMillis);
            return calendar;
        }
        GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -10);
        return calendar;
    }

    private void saveLastSyncTimestamp(@NonNull GregorianCalendar timestamp) {
        SharedPreferences.Editor editor = GBApplication.getPrefs().getPreferences().edit();
        editor.putLong(getLastSyncTimeKey(), timestamp.getTimeInMillis());
        editor.apply();
    }

    private String getLastSyncTimeKey() {
        return getDevice().getAddress() + "_" + "lastSyncTimeMillis";
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (MiBand2Service.UUID_CHARACTERISTIC_5_ACTIVITY_DATA.equals(characteristicUUID)) {
            handleActivityNotif(characteristic.getValue());
            return true;
        } else if (MiBand2Service.UUID_UNKNOWN_CHARACTERISTIC4.equals(characteristicUUID)) {
            handleActivityMetadata(characteristic.getValue());
            return true;
        } else {
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    private void handleActivityFetchFinish() {
        LOG.info("Fetching activity data has finished.");
        saveSamples();
        operationFinished();
        unsetBusy();
    }

    private void saveSamples() {
        if (samples.size() > 0) {
            // save all the samples that we got
            try (DBHandler handler = GBApplication.acquireDB()) {
                DaoSession session = handler.getDaoSession();
                SampleProvider<MiBandActivitySample> sampleProvider = new MiBandSampleProvider(getDevice(), session);
                Device device = DBHelper.getDevice(getDevice(), session);
                User user = DBHelper.getUser(session);

                GregorianCalendar timestamp = (GregorianCalendar) startTimestamp.clone();
                for (MiBandActivitySample sample : samples) {
                    sample.setDevice(device);
                    sample.setUser(user);
                    sample.setTimestamp((int) (timestamp.getTimeInMillis() / 1000));
                    sample.setProvider(sampleProvider);

                    if (LOG.isDebugEnabled()) {
//                        LOG.debug("sample: " + sample);
                    }

                    timestamp.add(Calendar.MINUTE, 1);
                }
                sampleProvider.addGBActivitySamples(samples.toArray(new MiBandActivitySample[0]));

                saveLastSyncTimestamp(timestamp);
                LOG.info("Mi2 activity data: last sample timestamp: " + DateTimeUtils.formatDateTime(timestamp.getTime()));

            } catch (Exception ex) {
                GB.toast(getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR);
            } finally {
                samples.clear();
            }
        }
    }

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
    private void handleActivityNotif(byte[] value) {
        if (!isOperationRunning()) {
            LOG.error("ignoring activity data notification because operation is not running. Data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if ((value.length % 4) == 1) {
            if ((byte) (lastPacketCounter + 1) == value[0] ) {
                lastPacketCounter++;
                bufferActivityData(value);
            } else {
                GB.toast("Error fetching activity data, invalid package counter: " + value[0], Toast.LENGTH_LONG, GB.ERROR);
                handleActivityFetchFinish();
                return;
            }
        } else {
            GB.toast("Error fetching activity data, unexpected package length: " + value.length, Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    /**
     * Creates samples from the given 17-length array
     * @param value
     */
    private void bufferActivityData(byte[] value) {
        int len = value.length;

        if (len % 4 != 1) {
            throw new AssertionError("Unexpected activity array size: " + value);
        }

        for (int i = 1; i < len; i+=4) {
            MiBandActivitySample sample = createSample(value[i], value[i + 1], value[i + 2], value[i + 3]);
            samples.add(sample);
        }
    }

    private MiBandActivitySample createSample(byte category, byte intensity, byte steps, byte heartrate) {
        MiBandActivitySample sample = new MiBandActivitySample();
        sample.setRawKind(category & 0xff);
        sample.setRawIntensity(intensity & 0xff);
        sample.setSteps(steps & 0xff);
        sample.setHeartRate(heartrate & 0xff);

        return sample;
    }

    private void handleActivityMetadata(byte[] value) {
        if (value.length == 15) {
            // first two bytes are whether our request was accepted
            if (ArrayUtils.equals(value, MiBand2Service.RESPONSE_ACTIVITY_DATA_START_DATE_SUCCESS, 0)) {
                // the third byte (0x01 on success) = ?
                // the 4th - 7th bytes probably somehow represent the number of bytes/packets to expect

                // last 8 bytes are the start date
                Calendar startTimestamp = getSupport().fromTimeBytes(org.apache.commons.lang3.ArrayUtils.subarray(value, 7, value.length));
                setStartTimestamp(startTimestamp);

                GB.toast(getContext().getString(R.string.FetchActivityOperation_about_to_transfer_since,
                        DateFormat.getDateTimeInstance().format(startTimestamp.getTime())), Toast.LENGTH_LONG, GB.INFO);
            } else {
                LOG.warn("Unexpected activity metadata: " + Logging.formatBytes(value));
                handleActivityFetchFinish();
            }
        } else if (value.length == 3) {
            if (Arrays.equals(MiBand2Service.RESPONSE_FINISH_SUCCESS, value)) {
                handleActivityFetchFinish();
            } else {
                LOG.warn("Unexpected activity metadata: " + Logging.formatBytes(value));
                handleActivityFetchFinish();
            }
        }
    }

    private void setStartTimestamp(Calendar startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
}
