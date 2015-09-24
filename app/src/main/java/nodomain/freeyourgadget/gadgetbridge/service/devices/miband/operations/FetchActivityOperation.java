package nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandDateConverter;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchActivityOperation extends AbstractBTLEOperation<MiBandSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(FetchActivityOperation.class);
    private static final byte[] fetch = new byte[]{MiBandService.COMMAND_FETCH_DATA};

    //temporary buffer, size is a multiple of 60 because we want to store complete minutes (1 minute = 3 bytes)
    private static final int activityDataHolderSize = 3 * 60 * 4; // 4h

    private static class ActivityStruct {
        private byte[] activityDataHolder = new byte[activityDataHolderSize];
        //index of the buffer above
        private int activityDataHolderProgress = 0;
        //number of bytes we will get in a single data transfer, used as counter
        private int activityDataRemainingBytes = 0;
        //same as above, but remains untouched for the ack message
        private int activityDataUntilNextHeader = 0;
        //timestamp of the single data transfer, incremented to store each minute's data
        private GregorianCalendar activityDataTimestampProgress = null;
        //same as above, but remains untouched for the ack message
        private GregorianCalendar activityDataTimestampToAck = null;

        public boolean hasRoomFor(byte[] value) {
            return activityDataRemainingBytes >= value.length;
        }

        public boolean isValidData(byte[] value) {
            //I don't like this clause, but until we figure out why we get different data sometimes this should work
            return value.length == 20 || value.length == activityDataRemainingBytes;
        }

        public boolean isBufferFull() {
            return activityDataHolderSize == activityDataHolderProgress;
        }

        public void buffer(byte[] value) {
            System.arraycopy(value, 0, activityDataHolder, activityDataHolderProgress, value.length);
            activityDataHolderProgress += value.length;
            activityDataRemainingBytes -= value.length;

            validate();
        }

        private void validate() {
            GB.assertThat(activityDataRemainingBytes >= 0, "Illegal state, remaining bytes is negative");
        }

        public boolean isFirstChunk() {
            return activityDataTimestampProgress == null;
        }

        public void startNewBlock(GregorianCalendar timestamp, int dataUntilNextHeader) {
            GB.assertThat(timestamp != null, "Timestamp must not be null");

            if (isFirstChunk()) {
                activityDataTimestampProgress = timestamp;
            } else {
                if (timestamp.getTimeInMillis() >= activityDataTimestampProgress.getTimeInMillis()) {
                    activityDataTimestampProgress = timestamp;
                } else {
                    // something is fishy here... better not trust the given timestamp and simply
                    // (re)use the current one
                    // we do accept the timestamp to ack though, so that the bogus data is properly cleared on the band
                }
            }
            activityDataTimestampToAck = (GregorianCalendar) timestamp.clone();
            activityDataRemainingBytes = activityDataUntilNextHeader = dataUntilNextHeader;
            validate();
        }

        public boolean isBlockFinished() {
            return activityDataRemainingBytes == 0;
        }

        public void bufferFlushed(int minutes) {
            activityDataTimestampProgress.add(Calendar.MINUTE, minutes);
            activityDataHolderProgress = 0;
        }
    }

    private ActivityStruct activityStruct = new ActivityStruct();

    public FetchActivityOperation(MiBandSupport support) {
        super(support);
    }

    @Override
    public void perform() throws IOException {
        TransactionBuilder builder = performInitialized("fetch activity data");
//            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_LE_PARAMS), getLowLatency());
        builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
        builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), fetch);
        builder.queue(getQueue());
    }


    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (MiBandService.UUID_CHARACTERISTIC_ACTIVITY_DATA.equals(characteristicUUID)) {
            handleActivityNotif(characteristic.getValue());
        } else {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    private void handleActivityFetchFinish() {
        LOG.info("Fetching activity data has finished.");
        activityStruct = null;
        unsetBusy();
    }

    /**
     * Method to handle the incoming activity data.
     * There are two kind of messages we currently know:
     * - the first one is 11 bytes long and contains metadata (how many bytes to expect, when the data starts, etc.)
     * - the second one is 20 bytes long and contains the actual activity data
     *
     * The first message type is parsed by this method, for every other length of the value param, bufferActivityData is called.
     * @see #bufferActivityData(byte[])
     *
     *
     * @param value
     */
    private void handleActivityNotif(byte[] value) {
        if (value.length == 11) {
            // byte 0 is the data type: 1 means that each minute is represented by a triplet of bytes
            int dataType = value[0];
            // byte 1 to 6 represent a timestamp
            GregorianCalendar timestamp = MiBandDateConverter.rawBytesToCalendar(value, 1);

            // counter of all data held by the band
            int totalDataToRead = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
            totalDataToRead *= (dataType == MiBandService.MODE_REGULAR_DATA_LEN_MINUTE) ? 3 : 1;


            // counter of this data block
            int dataUntilNextHeader = (value[9] & 0xff) | ((value[10] & 0xff) << 8);
            dataUntilNextHeader *= (dataType == MiBandService.MODE_REGULAR_DATA_LEN_MINUTE) ? 3 : 1;

            // there is a total of totalDataToRead that will come in chunks (3 bytes per minute if dataType == 1 (MiBandService.MODE_REGULAR_DATA_LEN_MINUTE)),
            // these chunks are usually 20 bytes long and grouped in blocks
            // after dataUntilNextHeader bytes we will get a new packet of 11 bytes that should be parsed
            // as we just did

            if (activityStruct.isFirstChunk() && dataUntilNextHeader != 0) {
                GB.toast(getContext().getString(R.string.user_feedback_miband_activity_data_transfer,
                        DateTimeUtils.formatDurationHoursMinutes((totalDataToRead / 3), TimeUnit.MINUTES),
                        DateFormat.getDateTimeInstance().format(timestamp.getTime())), Toast.LENGTH_LONG, GB.INFO);
            }
            LOG.info("total data to read: " + totalDataToRead + " len: " + (totalDataToRead / 3) + " minute(s)");
            LOG.info("data to read until next header: " + dataUntilNextHeader + " len: " + (dataUntilNextHeader / 3) + " minute(s)");
            LOG.info("TIMESTAMP: " + DateFormat.getDateTimeInstance().format(timestamp.getTime()).toString() + " magic byte: " + dataUntilNextHeader);

            activityStruct.startNewBlock(timestamp, dataUntilNextHeader);

        } else {
            bufferActivityData(value);
        }
        LOG.debug("activity data: length: " + value.length + ", remaining bytes: " + activityStruct.activityDataRemainingBytes);

        GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data), true, (int)(((float) (activityStruct.activityDataUntilNextHeader - activityStruct.activityDataRemainingBytes)) / activityStruct.activityDataUntilNextHeader * 100), getContext());

        if (activityStruct.isBlockFinished()) {
            sendAckDataTransfer(activityStruct.activityDataTimestampToAck, activityStruct.activityDataUntilNextHeader);
            GB.updateTransferNotification("", false, 100, getContext());
        }
    }

    /**
     * Method to store temporarily the activity data values got from the Mi Band.
     *
     * Since we expect chunks of 20 bytes each, we do not store the received bytes it the length is different.
     *
     * @param value
     */
    private void bufferActivityData(byte[] value) {
        if (activityStruct.hasRoomFor(value)) {
            if (activityStruct.isValidData(value)) {
                activityStruct.buffer(value);

                if (activityStruct.isBufferFull()) {
                    flushActivityDataHolder();
                }
            } else {
                // the length of the chunk is not what we expect. We need to make sense of this data
                LOG.warn("GOT UNEXPECTED ACTIVITY DATA WITH LENGTH: " + value.length + ", EXPECTED LENGTH: " + activityStruct.activityDataRemainingBytes);
                getSupport().logMessageContent(value);
            }
        } else {
            LOG.error("error buffering activity data: remaining bytes: " + activityStruct.activityDataRemainingBytes + ", received: " + value.length);
        }
    }

    /**
     * empty the local buffer for activity data, arrange the values received in groups of three and
     * store them in the DB
     */
    private void flushActivityDataHolder() {
        if (activityStruct == null) {
            LOG.debug("nothing to flush, struct is already null");
            return;
        }
        LOG.debug("flushing activity data samples: " + activityStruct.activityDataHolderProgress / 3);
        byte category, intensity, steps;

        DBHandler dbHandler = null;
        try {
            dbHandler = GBApplication.acquireDB();
            int minutes = 0;
            try (SQLiteDatabase db = dbHandler.getWritableDatabase()) { // explicitly keep the db open while looping over the samples
                int timestampInSeconds = (int) (activityStruct.activityDataTimestampProgress.getTimeInMillis() / 1000);
                for (int i = 0; i < activityStruct.activityDataHolderProgress; i += 3) { //TODO: check if multiple of 3, if not something is wrong
                    category = activityStruct.activityDataHolder[i];
                    intensity = activityStruct.activityDataHolder[i + 1];
                    steps = activityStruct.activityDataHolder[i + 2];

                    dbHandler.addGBActivitySample(
                            timestampInSeconds,
                            SampleProvider.PROVIDER_MIBAND,
                            (short) (intensity & 0xff),
                            (short) (steps & 0xff),
                            category);
                    // next minute
                    minutes++;
                    timestampInSeconds += 60;
                }
            } finally {
                activityStruct.bufferFlushed(minutes);
            }
        } catch (Exception ex) {
            GB.toast(getContext(), ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR);
        } finally {
            if (dbHandler != null) {
                dbHandler.release();
            }
        }
    }

    /**
     * Acknowledge the transfer of activity data to the Mi Band.
     *
     * After receiving data from the band, it has to be acknowledged. This way the Mi Band will delete
     * the data it has on record.
     *
     * @param time
     * @param bytesTransferred
     */
    private void sendAckDataTransfer(Calendar time, int bytesTransferred) {
        byte[] ackTime = MiBandDateConverter.calendarToRawBytes(time);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());

        byte[] ackChecksum = new byte[]{
                (byte) (bytesTransferred & 0xff),
                (byte) (0xff & (bytesTransferred >> 8))
        };
        if (prefs.getBoolean(MiBandConst.PREF_MIBAND_DONT_ACK_TRANSFER, false)) {
            ackChecksum = new byte[]{
                    (byte) (~bytesTransferred & 0xff),
                    (byte) (0xff & (~bytesTransferred >> 8))
            };
        }
        byte[] ack = new byte[]{
                MiBandService.COMMAND_CONFIRM_ACTIVITY_DATA_TRANSFER_COMPLETE,
                ackTime[0],
                ackTime[1],
                ackTime[2],
                ackTime[3],
                ackTime[4],
                ackTime[5],
                ackChecksum[0],
                ackChecksum[1]
        };
        try {
            TransactionBuilder builder = performInitialized("send acknowledge");
            builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), ack);
            builder.queue(getQueue());

            // flush to the DB after queueing the ACK
            flushActivityDataHolder();

            //The last data chunk sent by the miband has always length 0.
            //When we ack this chunk, the transfer is done.
            if (getDevice().isBusy() && bytesTransferred == 0) {
                //if we are not clearing miband's data, we have to stop the sync
                if (prefs.getBoolean(MiBandConst.PREF_MIBAND_DONT_ACK_TRANSFER, false)) {
                    builder = performInitialized("send acknowledge");
                    builder.write(getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT), new byte[]{MiBandService.COMMAND_STOP_SYNC_DATA});
                    builder.queue(getQueue());
                }
                handleActivityFetchFinish();
            }
        } catch (IOException ex) {
            LOG.error("Unable to send ack to MI", ex);
        }
    }
}
