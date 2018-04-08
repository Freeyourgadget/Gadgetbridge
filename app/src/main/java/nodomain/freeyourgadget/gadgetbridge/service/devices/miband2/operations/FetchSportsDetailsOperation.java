/*  Copyright (C) 2017 Andreas Shimokawa, Carsten Pfeiffer

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

import android.support.annotation.NonNull;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbip.AmazfitBipService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2Service;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip.ActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchSportsDetailsOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSportsDetailsOperation.class);
    private final BaseActivitySummary summary;
    private final String lastSyncTimeKey;

    private ByteArrayOutputStream buffer;

    public FetchSportsDetailsOperation(@NonNull BaseActivitySummary summary, @NonNull MiBand2Support support, @NonNull String lastSyncTimeKey) {
        super(support);
        setName("fetching sport details");
        this.summary = summary;
        this.lastSyncTimeKey = lastSyncTimeKey;
    }

    @Override
    protected void startFetching(TransactionBuilder builder) {
        LOG.info("start " + getName());
        buffer = new ByteArrayOutputStream(1024);
        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();

        builder.write(characteristicFetch, BLETypeConversions.join(new byte[] {
                MiBand2Service.COMMAND_ACTIVITY_DATA_START_DATE,
                AmazfitBipService.COMMAND_ACTIVITY_DATA_TYPE_SPORTS_DETAILS},
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


        if (success) {
            ActivityDetailsParser parser = new ActivityDetailsParser(summary);
            parser.setSkipCounterByte(false); // is already stripped
            try {
                ActivityTrack track = parser.parse(buffer.toByteArray());
                ActivityTrackExporter exporter = createExporter();
                String fileName = FileUtils.makeValidFileName("gadgetbridge-track-" + DateTimeUtils.formatIso8601(summary.getStartTime())+".gpx");
                File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);

                try {
                    exporter.performExport(track, targetFile);

                    try (DBHandler dbHandler = GBApplication.acquireDB()) {
                        summary.setGpxTrack(targetFile.getAbsolutePath());
                        dbHandler.getDaoSession().getBaseActivitySummaryDao().update(summary);
                    }
                } catch (ActivityTrackExporter.GPXTrackEmptyException ex) {
                    GB.toast(getContext(), "This activity does not contain GPX tracks.", Toast.LENGTH_LONG, GB.ERROR, ex);
                }

                GregorianCalendar endTime = BLETypeConversions.createCalendar();
                endTime.setTime(summary.getEndTime());
                saveLastSyncTimestamp(endTime);
            } catch (Exception ex) {
                GB.toast(getContext(), "Error getting activity details: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
            }
        }

        super.handleActivityFetchFinish(success);
    }

    protected ActivityTrackExporter createExporter() {
        GPXExporter exporter = new GPXExporter();
        exporter.setCreator(GBApplication.app().getNameAndVersion());
        return exporter;
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
        LOG.warn("sports details: " + Logging.formatBytes(value));

        if (!isOperationRunning()) {
            LOG.error("ignoring sports details notification because operation is not running. Data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if (value.length < 2) {
            LOG.error("unexpected sports details data length: " + value.length);
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

    @Override
    protected String getLastSyncTimeKey() {
        return lastSyncTimeKey;
    }

    protected GregorianCalendar getLastSuccessfulSyncTime() {
        GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.setTime(summary.getStartTime());
        return calendar;
    }
}
