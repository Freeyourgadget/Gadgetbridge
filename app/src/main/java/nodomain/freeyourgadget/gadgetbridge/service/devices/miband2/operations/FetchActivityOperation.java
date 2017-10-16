/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer

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

import android.text.format.DateUtils;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.MiBand2Support;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchActivityOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchActivityOperation.class);

    private List<MiBandActivitySample> samples = new ArrayList<>(60*24); // 1day per default

    public FetchActivityOperation(MiBand2Support support) {
        super(support);
    }

    @Override
    protected void startFetching() throws IOException {
        samples.clear();
        super.startFetching();
    }

    @Override
    protected void startFetching(TransactionBuilder builder) {
        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        builder.write(characteristicFetch, BLETypeConversions.join(new byte[] { MiBand2Service.COMMAND_ACTIVITY_DATA_START_DATE, MiBand2Service.COMMAND_ACTIVITY_DATA_TYPE_ACTIVTY }, getSupport().getTimeBytes(sinceWhen, TimeUnit.MINUTES)));
        builder.add(new WaitAction(1000)); // TODO: actually wait for the success-reply
        builder.notify(characteristicActivityData, true);
        builder.write(characteristicFetch, new byte[] { MiBand2Service.COMMAND_FETCH_DATA});
    }

    protected void handleActivityFetchFinish() {
        LOG.info("Fetching activity data has finished round " + fetchCount);
        GregorianCalendar lastSyncTimestamp = saveSamples();
        if (lastSyncTimestamp != null && needsAnotherFetch(lastSyncTimestamp)) {
            try {
                startFetching();
                return;
            } catch (IOException ex) {
                LOG.error("Error starting another round of fetching activity data", ex);
            }
        }

        super.handleActivityFetchFinish();
    }

    private boolean needsAnotherFetch(GregorianCalendar lastSyncTimestamp) {
        if (fetchCount > 5) {
            LOG.warn("Already jave 5 fetch rounds, not doing another one.");
            return false;
        }

        if (DateUtils.isToday(lastSyncTimestamp.getTimeInMillis())) {
            LOG.info("Hopefully no further fetch needed, last synced timestamp is from today.");
            return false;
        }
        if (lastSyncTimestamp.getTimeInMillis() > System.currentTimeMillis()) {
            LOG.warn("Not doing another fetch since last synced timestamp is in the future: " + DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
            return false;
        }
        LOG.info("Doing another fetch since last sync timestamp is still too old: " + DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
        return true;
    }

    private GregorianCalendar saveSamples() {
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
                return timestamp;

            } catch (Exception ex) {
                GB.toast(getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR);
            } finally {
                samples.clear();
            }
        }
        return null;
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
    protected void handleActivityNotif(byte[] value) {
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
    protected void bufferActivityData(byte[] value) {
        int len = value.length;

        if (len % 4 != 1) {
            throw new AssertionError("Unexpected activity array size: " + len);
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

    @Override
    protected String getLastSyncTimeKey() {
        return getDevice().getAddress() + "_" + "lastSyncTimeMillis";
    }
}
