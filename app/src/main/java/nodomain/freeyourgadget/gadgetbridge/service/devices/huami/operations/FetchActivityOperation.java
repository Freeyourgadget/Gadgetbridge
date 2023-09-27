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

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchActivityOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchActivityOperation.class);

    private final int sampleSize;

    public FetchActivityOperation(final HuamiSupport support) {
        super(support, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_ACTIVTY, "activity data");
        this.sampleSize = support.getActivitySampleSize();
    }

    @Override
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_activity_data);
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        if (bytes.length % sampleSize != 0) {
            GB.toast(getContext(), "Unexpected " + getName() + " array size: " + bytes.length, Toast.LENGTH_LONG, GB.ERROR);
            return false;
        }

        final List<MiBandActivitySample> samples = new ArrayList<>(60 * 24); // 1day per default

        for (int i = 0; i < bytes.length; i += sampleSize) {
            final MiBandActivitySample sample;

            switch (sampleSize) {
                case 4:
                    sample = createSample(bytes, i);
                    break;
                case 8:
                    sample = createExtendedSample(bytes, i);
                    break;
                default:
                    throw new IllegalStateException("Unsupported sample size " + sampleSize);
            }

            samples.add(sample);
        }

        if (samples.isEmpty()) {
            LOG.info("No samples to save");
            return true;
        }

        LOG.info("Saving {} samples", samples.size());

        // save all the samples that we got
        try (DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            DeviceCoordinator coordinator = getDevice().getDeviceCoordinator();
            SampleProvider sampleProvider = coordinator.getSampleProvider(getDevice(), session);
            Device device = DBHelper.getDevice(getDevice(), session);
            User user = DBHelper.getUser(session);

            for (MiBandActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setTimestamp((int) (timestamp.getTimeInMillis() / 1000));
                sample.setProvider(sampleProvider);

                //LOG.debug(sampleToString(sample));

                timestamp.add(Calendar.MINUTE, 1);
            }
            sampleProvider.addGBActivitySamples(samples.toArray(new MiBandActivitySample[0]));

            timestamp.add(Calendar.MINUTE, -1);

            LOG.info("Huami activity data: last sample timestamp: {}", DateTimeUtils.formatDateTime(timestamp.getTime()));
            return true;
        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving activity samples", ex);
            return false;
        }
    }

    @Override
    protected void postActivityFetchFinish(final boolean success) {
        GB.signalActivityDataFinish();
    }

    @Override
    protected boolean validChecksum(final int crc32) {
        // TODO actually check it
        LOG.warn("Checksum not implemented for activity data, assuming it's valid");
        return true;
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
