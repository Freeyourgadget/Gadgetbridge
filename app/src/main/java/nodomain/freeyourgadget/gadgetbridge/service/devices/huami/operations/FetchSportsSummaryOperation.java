/*  Copyright (C) 2017-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipService;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSportsActivityType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchSportsSummaryOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSportsSummaryOperation.class);

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream(140);

    public FetchSportsSummaryOperation(HuamiSupport support) {
        super(support);
        setName("fetching sport summaries");
    }

    @Override
    protected void startFetching(TransactionBuilder builder) {
        LOG.info("start" + getName());
        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        startFetching(builder, AmazfitBipService.COMMAND_ACTIVITY_DATA_TYPE_SPORTS_SUMMARIES, sinceWhen);
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

        if ((byte) (lastPacketCounter + 1) == value[0]) {
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
     *
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
        short version = buffer.getShort(); // version
        LOG.debug("Got sport summary version " + version + "total bytes=" + buffer.capacity());
        int activityKind = ActivityKind.TYPE_UNKNOWN;
        try {
            int rawKind = BLETypeConversions.toUnsigned(buffer.getShort());
            HuamiSportsActivityType activityType = HuamiSportsActivityType.fromCode(rawKind);
            activityKind = activityType.toActivityKind();
        } catch (Exception ex) {
            LOG.error("Error mapping activity kind: " + ex.getMessage(), ex);
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

        // unused data (for now)
        float distanceMeters = buffer.getFloat();
        float ascentMeters = buffer.getFloat();
        float descentMeters = buffer.getFloat();
        float maxAltitude = buffer.getFloat();
        float minAltitude = buffer.getFloat();
        int maxLatitude = buffer.getInt(); // format?
        int minLatitude = buffer.getInt(); // format?
        int maxLongitude = buffer.getInt(); // format?
        int minLongitude = buffer.getInt(); // format?
        int steps = buffer.getInt();
        int activeSeconds = buffer.getInt();
        float caloriesBurnt = buffer.getFloat();
        float maxSpeed = buffer.getFloat();
        float minPace = buffer.getFloat(); // format?
        float maxPace = buffer.getFloat(); // format?
        float totalStride = buffer.getFloat();

        buffer.getInt(); // unknown

        if (activityKind == ActivityKind.TYPE_SWIMMING) {
            // 28 bytes
            float averageStrokeDistance = buffer.getFloat();
            float averageStrokesPerSecond = buffer.getFloat();
            float averageLapPace = buffer.getFloat();
            short strokes = buffer.getShort();
            short swolfIndex = buffer.getShort();
            byte swimStyle = buffer.get();
            byte laps = buffer.get();
            buffer.getInt(); // unknown
            buffer.getInt(); // unknown
            buffer.getShort(); // unknown

            LOG.debug("unused swim data:" +
                    "\naverageStrokeDistance=" + averageStrokeDistance +
                    "\naverageStrokesPerSecond=" + averageStrokesPerSecond +
                    "\naverageLapPace" + averageLapPace +
                    "\nstrokes=" + strokes +
                    "\nswolfIndex=" + swolfIndex +
                    "\nswimStyle=" + swimStyle + // 1 = breast, 2 = freestyle
                    "\nlaps=" + laps +
                    ""
            );
        } else {
            // 28 bytes
            buffer.getInt(); // unknown
            buffer.getInt(); // unknown
            int ascentSeconds = buffer.getInt() / 1000; //ms?
            buffer.getInt(); // unknown;
            int descentSeconds = buffer.getInt() / 1000; //ms?
            buffer.getInt(); // unknown;
            int flatSeconds = buffer.getInt() / 1000; // ms?
            LOG.debug("unused non-swim data:" +
                    "\nascentSeconds=" + ascentSeconds +
                    "\ndescentSeconds=" + descentSeconds +
                    "\nflatSeconds=" + flatSeconds +
                    ""
            );
        }

        short averageHR = buffer.getShort();
        short averageKMPaceSeconds = buffer.getShort();
        short averageStride = buffer.getShort();

        LOG.debug("unused common:" +
                "\ndistanceMeters=" + distanceMeters +
                "\nascentMeters=" + ascentMeters +
                "\ndescentMeters=" + descentMeters +
                "\nmaxAltitude=" + maxAltitude +
                "\nminAltitude=" + minAltitude +
                //"\nmaxLatitude=" + maxLatitude + // not useful
                //"\nminLatitude=" + minLatitude + // not useful
                //"\nmaxLongitude=" + maxLongitude + // not useful
                //"\nminLongitude=" + minLongitude + // not useful
                "\nsteps=" + steps +
                "\nactiveSeconds=" + activeSeconds +
                "\ncaloriesBurnt=" + caloriesBurnt +
                "\nmaxSpeed=" + maxSpeed +
                "\nminPace=" + minPace +
                "\nmaxPace=" + maxPace +
                "\ntotalStride=" + totalStride +
                "\naverageHR=" + averageHR +
                "\naverageKMPaceSeconds=" + averageKMPaceSeconds +
                "\naverageStride=" + averageStride +
                ""
        );

//        summary.setBaseCoordinate(new GPSCoordinate(baseLatitude, baseLongitude, baseAltitude));
//        summary.setDistanceMeters(distanceMeters);
//        summary.setAscentMeters(ascentMeters);
//        summary.setDescentMeters(descentMeters);
//        summary.setMinAltitude(maxAltitude);
//        summary.setMaxAltitude(maxAltitude);
//        summary.setMinLatitude(minLatitude);
//        summary.setMaxLatitude(maxLatitude);
//        summary.setMinLongitude(minLatitude);
//        summary.setMaxLongitude(maxLatitude);
//        summary.setSteps(steps);
//        summary.setActiveTimeSeconds(secondsActive);
//        summary.setCaloriesBurnt(caloriesBurnt);
//        summary.setMaxSpeed(maxSpeed);
//        summary.setMinPace(minPace);
//        summary.setMaxPace(maxPace);
//        summary.setTotalStride(totalStride);
//        summary.setTimeAscent(BLETypeConversions.toUnsigned(ascentSeconds);
//        summary.setTimeDescent(BLETypeConversions.toUnsigned(descentSeconds);
//        summary.setTimeFlat(BLETypeConversions.toUnsigned(flatSeconds);
//        summary.setAverageHR(BLETypeConversions.toUnsigned(averageHR);
//        summary.setAveragePace(BLETypeConversions.toUnsigned(averagePace);
//        summary.setAverageStride(BLETypeConversions.toUnsigned(averageStride);

        return summary;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSportsActivityTimeMillis";
    }
}
