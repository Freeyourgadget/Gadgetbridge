/*  Copyright (C) 2017-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.BipActivityType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchSportsSummaryOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSportsSummaryOperation.class);

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream(140);

    public FetchSportsSummaryOperation(MiBand2Support support) {
        super(support);
        setName("fetching sport summaries");
    }

    @Override
    protected void startFetching(TransactionBuilder builder) {
        LOG.info("start" + getName());
        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        builder.write(characteristicFetch, BLETypeConversions.join(new byte[] {
                MiBand2Service.COMMAND_ACTIVITY_DATA_START_DATE,
                AmazfitBipService.COMMAND_ACTIVITY_DATA_TYPE_SPORTS_SUMMARIES},
                getSupport().getTimeBytes(sinceWhen, TimeUnit.MINUTES)));
        builder.add(new WaitAction(1000)); // TODO: actually wait for the success-reply
        builder.notify(characteristicActivityData, true);
        builder.write(characteristicFetch, new byte[] { MiBand2Service.COMMAND_FETCH_DATA });
    }

    @Override
    protected void handleActivityFetchFinish(boolean success) {
        LOG.info(getName() + " has finished round " + fetchCount);

//        GregorianCalendar lastSyncTimestamp = saveSamples();
//        if (lastSyncTimestamp != null && needsAnotherFetch(lastSyncTimestamp)) {
//            try {
//                startFetching();
//                return;
//            } catch (IOException ex) {
//                LOG.error("Error starting another round of fetching activity data", ex);
//            }
//        }

        BaseActivitySummary summary = null;
        if (success) {
            summary = parseSummary(buffer);
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                DaoSession session = dbHandler.getDaoSession();
                Device device = DBHelper.getDevice(getDevice(), session);
                User user = DBHelper.getUser(session);
                summary.setDevice(device);
                summary.setUser(user);
                session.getBaseActivitySummaryDao().insertOrReplace(summary);
            } catch (Exception ex) {
                GB.toast(getContext(), "Error saving activity summary", Toast.LENGTH_LONG, GB.ERROR, ex);
            }
        }

        super.handleActivityFetchFinish(success);

        if (summary != null) {
            FetchSportsDetailsOperation nextOperation = new FetchSportsDetailsOperation(summary, getSupport(), getLastSyncTimeKey());
            try {
                nextOperation.perform();
            } catch (IOException ex) {
                GB.toast(getContext(), "Unable to fetch activity details: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
            }
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        LOG.warn("characteristic read: " + characteristic.getUuid() + ": " + Logging.formatBytes(characteristic.getValue()));
        return super.onCharacteristicRead(gatt, characteristic, status);
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
    @Override
    protected void handleActivityNotif(byte[] value) {
        LOG.warn("sports summary data: " + Logging.formatBytes(value));

        if (!isOperationRunning()) {
            LOG.error("ignoring activity data notification because operation is not running. Data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if (value.length < 2) {
            LOG.error("unexpected sports summary data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if ((byte) (lastPacketCounter + 1) == value[0] ) {
            lastPacketCounter++;
            bufferActivityData(value);
        } else {
            GB.toast("Error " + getName() + ", invalid package counter: " + value[0] + ", last was: " + lastPacketCounter, Toast.LENGTH_LONG, GB.ERROR);
            handleActivityFetchFinish(false);
            return;
        }
    }

    /**
     * Buffers the given activity summary data. If the total size is reached,
     * it is converted to an object and saved in the database.
     * @param value
     */
    @Override
    protected void bufferActivityData(byte[] value) {
        buffer.write(value, 1, value.length - 1); // skip the counter
    }

    private BaseActivitySummary parseSummary(ByteArrayOutputStream stream) {
        BaseActivitySummary summary = new BaseActivitySummary();
        ByteBuffer buffer = ByteBuffer.wrap(stream.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
//        summary.setVersion(BLETypeConversions.toUnsigned(buffer.getShort()));
        buffer.getShort(); // version
        int activityKind = ActivityKind.TYPE_UNKNOWN;
        try {
            int rawKind = BLETypeConversions.toUnsigned(buffer.getShort());
            BipActivityType activityType = BipActivityType.fromCode(rawKind);
            activityKind = activityType.toActivityKind();
        } catch (Exception ex) {
            LOG.error("Error mapping acivity kind: " + ex.getMessage(), ex);
        }
        summary.setActivityKind(activityKind);

        // FIXME: should honor timezone we were in at that time etc
        long timestamp_start = BLETypeConversions.toUnsigned(buffer.getInt()) * 1000;
        long timestamp_end = BLETypeConversions.toUnsigned(buffer.getInt()) * 1000;


        // FIXME: should be done like this but seems to return crap when in DST
        //summary.setStartTime(new Date(timestamp_start));
        //summary.setEndTime(new Date(timestamp_end));

        // FIXME ... so do it like this
        long duration = timestamp_end - timestamp_start;
        summary.setStartTime(new Date(getLastStartTimestamp().getTimeInMillis()));
        summary.setEndTime(new Date(getLastStartTimestamp().getTimeInMillis() + duration));

        int baseLongitude = buffer.getInt();
        int baseLatitude = buffer.getInt();
        int baseAltitude = buffer.getInt();
        summary.setBaseLongitude(baseLongitude);
        summary.setBaseLatitude(baseLatitude);
        summary.setBaseAltitude(baseAltitude);
//        summary.setBaseCoordinate(new GPSCoordinate(baseLatitude, baseLongitude, baseAltitude));

//        summary.setDistanceMeters(Float.intBitsToFloat(buffer.getInt()));
//        summary.setAscentMeters(Float.intBitsToFloat(buffer.getInt()));
//        summary.setDescentMeters(Float.intBitsToFloat(buffer.getInt()));
//
//        summary.setMinAltitude(Float.intBitsToFloat(buffer.getInt()));
//        summary.setMaxAltitude(Float.intBitsToFloat(buffer.getInt()));
//        summary.setMinLatitude(buffer.getInt());
//        summary.setMaxLatitude(buffer.getInt());
//        summary.setMinLongitude(buffer.getInt());
//        summary.setMaxLongitude(buffer.getInt());
//
//        summary.setSteps(BLETypeConversions.toUnsigned(buffer.getInt()));
//        summary.setActiveTimeSeconds(BLETypeConversions.toUnsigned(buffer.getInt()));
//
//        summary.setCaloriesBurnt(Float.intBitsToFloat(buffer.get()));
//        summary.setMaxSpeed(Float.intBitsToFloat(buffer.get()));
//        summary.setMinPace(Float.intBitsToFloat(buffer.get()));
//        summary.setMaxPace(Float.intBitsToFloat(buffer.get()));
//        summary.setTotalStride(Float.intBitsToFloat(buffer.get()));

        buffer.getInt(); //
        buffer.getInt(); //
        buffer.getInt(); //

//        summary.setTimeAscent(BLETypeConversions.toUnsigned(buffer.getInt()));
//        buffer.getInt(); //
//        summary.setTimeDescent(BLETypeConversions.toUnsigned(buffer.getInt()));
//        buffer.getInt(); //
//        summary.setTimeFlat(BLETypeConversions.toUnsigned(buffer.getInt()));
//
//        summary.setAverageHR(BLETypeConversions.toUnsigned(buffer.getShort()));
//
//        summary.setAveragePace(BLETypeConversions.toUnsigned(buffer.getShort()));
//        summary.setAverageStride(BLETypeConversions.toUnsigned(buffer.getShort()));

        buffer.getShort(); //

        return summary;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return getDevice().getAddress() + "_" + "lastSportsActivityTimeMillis";
    }
}
