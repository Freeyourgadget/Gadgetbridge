/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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

import android.text.format.DateUtils;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchActivityOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchActivityOperation.class);

    private final int sampleSize;
    private List<MiBandActivitySample> samples = new ArrayList<>(60 * 24); // 1day per default

    public FetchActivityOperation(HuamiSupport support) {
        super(support);
        setName("fetching activity data");
        sampleSize = getSupport().getActivitySampleSize();
    }

    @Override
    protected void startFetching() throws IOException {
        samples.clear();
        super.startFetching();
    }

    @Override
    protected void startFetching(TransactionBuilder builder) {
        GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        startFetching(builder, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_ACTIVTY, sinceWhen);
    }

    @Override
    protected boolean handleActivityFetchFinish(boolean success) {
        LOG.info("{} has finished round {}", getName(), fetchCount);
        GregorianCalendar lastSyncTimestamp = saveSamples();

        if (lastSyncTimestamp != null && needsAnotherFetch(lastSyncTimestamp)) {
            try {
                startFetching();
                return true;
            } catch (IOException ex) {
                LOG.error("Error starting another round of {}", getName(), ex);
                return false;
            }
        }

        final boolean superSuccess = super.handleActivityFetchFinish(success);
        GB.signalActivityDataFinish();
        return superSuccess;
    }

    @Override
    protected boolean validChecksum(int crc32) {
        // TODO actually check it
        LOG.warn("Checksum not implemented for activity data, assuming it's valid");
        return true;
    }

    private boolean needsAnotherFetch(GregorianCalendar lastSyncTimestamp) {
        if (fetchCount > 5) {
            LOG.warn("Already have 5 fetch rounds, not doing another one.");
            return false;
        }

        if (DateUtils.isToday(lastSyncTimestamp.getTimeInMillis())) {
            LOG.info("Hopefully no further fetch needed, last synced timestamp is from today.");
            return false;
        }
        if (lastSyncTimestamp.getTimeInMillis() > System.currentTimeMillis()) {
            LOG.warn("Not doing another fetch since last synced timestamp is in the future: {}", DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
            return false;
        }
        LOG.info("Doing another fetch since last sync timestamp is still too old: {}", DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
        return true;
    }

    private GregorianCalendar saveSamples() {
        if (samples.isEmpty()) {
            LOG.info("No samples to save");
            return null;
        }

        LOG.info("Saving {} samples", samples.size());

        // save all the samples that we got
        try (DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(getDevice());
            SampleProvider sampleProvider = coordinator.getSampleProvider(getDevice(), session);
            Device device = DBHelper.getDevice(getDevice(), session);
            User user = DBHelper.getUser(session);

            GregorianCalendar timestamp = (GregorianCalendar) startTimestamp.clone();
            for (MiBandActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setTimestamp((int) (timestamp.getTimeInMillis() / 1000));
                sample.setProvider(sampleProvider);

                //LOG.debug(sampleToString(sample));

                timestamp.add(Calendar.MINUTE, 1);
            }
            sampleProvider.addGBActivitySamples(samples.toArray(new MiBandActivitySample[0]));

            saveLastSyncTimestamp(timestamp);
            LOG.info("Huami activity data: last sample timestamp: {}", DateTimeUtils.formatDateTime(timestamp.getTime()));
            return timestamp;
        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving activity samples", ex);
            return null;
        } finally {
            samples.clear();
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
    protected void handleActivityNotif(byte[] value) {
        if (!isOperationRunning()) {
            LOG.error("ignoring activity data notification because operation is not running. Data length: " + value.length);
            getSupport().logMessageContent(value);
            return;
        }

        if ((value.length % sampleSize) == 1) {
            if ((byte) (lastPacketCounter + 1) == value[0]) {
                lastPacketCounter++;
                bufferActivityData(value);
            } else {
                GB.toast("Error " + getName() + ", invalid package counter: " + value[0], Toast.LENGTH_LONG, GB.ERROR);
                handleActivityFetchFinish(false);
                return;
            }
        } else {
            GB.toast("Error " + getName() + ", unexpected package length: " + value.length, Toast.LENGTH_LONG, GB.ERROR);
            handleActivityFetchFinish(false);
        }
    }

    /**
     * Creates samples from the given 17-length array
     * @param value
     */
    protected void bufferActivityData(byte[] value) {
        int len = value.length;

        if (len % sampleSize != 1) {
            throw new AssertionError("Unexpected activity array size: " + len);
        }

        for (int i = 1; i < len; i += sampleSize) {
            final MiBandActivitySample sample;

            switch (sampleSize) {
                case 4:
                    sample = createSample(value, i);
                    break;
                case 8:
                    sample = createExtendedSample(value, i);
                    break;
                default:
                    throw new IllegalStateException("Unsupported sample size " + sampleSize);
            }

            samples.add(sample);
        }
    }

    private MiBandActivitySample createSample(byte[] value, int i) {
        MiBandActivitySample sample = new MiBandActivitySample();
        sample.setRawKind(value[i] & 0xff);
        sample.setRawIntensity(value[i + 1] & 0xff);
        sample.setSteps(value[i + 2] & 0xff);
        sample.setHeartRate(value[i + 3] & 0xff);

        return sample;
    }

    private MiBandActivitySample createExtendedSample(byte[] value, int i) {
        final HuamiExtendedActivitySample huamiExtendedActivitySample = new HuamiExtendedActivitySample();
        huamiExtendedActivitySample.setRawKind(value[i] & 0xff);
        huamiExtendedActivitySample.setRawIntensity(value[i + 1] & 0xff);
        huamiExtendedActivitySample.setSteps(value[i + 2] & 0xff);
        huamiExtendedActivitySample.setHeartRate(value[i + 3] & 0xff);
        huamiExtendedActivitySample.setUnknown1(value[i + 4] & 0xff);
        huamiExtendedActivitySample.setSleep(value[i + 5] & 0xff);
        huamiExtendedActivitySample.setDeepSleep(value[i + 6] & 0xff);
        huamiExtendedActivitySample.setRemSleep(value[i + 7] & 0xff);

        return huamiExtendedActivitySample;
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);

    public static String sampleToString(final MiBandActivitySample sample) {
        final StringBuilder builder = new StringBuilder(160);

        builder.append(sample.getClass().getSimpleName());
        builder.append("{");
        builder.append(sdf.format(new Date(sample.getTimestamp() * 1000L)));
        builder.append(",rawKind=").append(sample.getRawKind());
        builder.append(",rawIntensity=").append(sample.getRawIntensity());
        builder.append(",steps=").append(sample.getSteps());
        builder.append(",heartRate=").append(sample.getHeartRate());

        if (sample instanceof HuamiExtendedActivitySample) {
            final HuamiExtendedActivitySample huamiExtendedSample = (HuamiExtendedActivitySample) sample;

            builder.append(",unknown1=").append(huamiExtendedSample.getUnknown1());
            builder.append(",sleep=").append(huamiExtendedSample.getSleep());
            builder.append(",deepSleep=").append(huamiExtendedSample.getDeepSleep());
            builder.append(",remSleep=").append(huamiExtendedSample.getRemSleep());
        }

        builder.append("}");

        return builder.toString();
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastSyncTimeMillis";
    }
}
