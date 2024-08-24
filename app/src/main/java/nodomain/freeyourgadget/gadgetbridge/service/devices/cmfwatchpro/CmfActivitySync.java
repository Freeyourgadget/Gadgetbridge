/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfSleepSessionSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfWorkoutGpsSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.workout.CmfWorkoutSummaryParser;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSleepSessionSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfWorkoutGpsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CmfActivitySync {
    private static final Logger LOG = LoggerFactory.getLogger(CmfActivitySync.class);

    private final CmfWatchProSupport mSupport;

    private final List<BaseActivitySummary> activitiesWithGps = new ArrayList<>();

    protected CmfActivitySync(final CmfWatchProSupport support) {
        this.mSupport = support;
    }

    protected boolean onCommand(final CmfCommand cmd, final byte[] payload) {
        switch (cmd) {
            case ACTIVITY_FETCH_ACK_1:
                handleActivityFetchAck1(payload);
                return true;
            case ACTIVITY_FETCH_ACK_2:
                handleActivityFetchAck2(payload);
                return true;
            case ACTIVITY_DATA:
                handleActivityData(payload);
                return true;
            case HEART_RATE_MANUAL_AUTO:
            case HEART_RATE_WORKOUT:
                handleHeartRate(payload);
                return true;
            case HEART_RATE_RESTING:
                handleHeartRateResting(payload);
                return true;
            case SLEEP_DATA:
                handleSleepData(payload);
                return true;
            case STRESS:
                handleStress(payload);
                return true;
            case SPO2:
                handleSpo2(payload);
                return true;
            case WORKOUT_SUMMARY:
                handleWorkoutSummary(payload);
                return true;
            case WORKOUT_GPS:
                handleWorkoutGps(payload);
                return true;
        }

        return false;
    }

    private void handleActivityFetchAck1(final byte[] payload) {
        switch (payload[0]) {
            case 0x01:
                LOG.debug("Got activity fetch ack 1, starting step 2");
                GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data), "", true, 0, getContext());
                getDevice().setBusyTask(getContext().getString(R.string.busy_task_fetch_activity_data));
                mSupport.sendCommand("fetch recorded data step 2", CmfCommand.ACTIVITY_FETCH_2, CmfWatchProSupport.A5);
                break;
            case 0x02:
                LOG.debug("Got activity fetch finish");
                // Process activities with GPS before unsetting device as busy
                processActivitiesWithGps();
                break;
            default:
                LOG.warn("Unknown activity fetch ack code {}", payload[0]);
                return;
        }

        getDevice().sendDeviceUpdateIntent(getContext());
    }

    private static void handleActivityFetchAck2(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final int activityTs = buf.getInt();
        final byte[] activityFlags = new byte[4]; // TODO what do they mean?
        buf.order(ByteOrder.BIG_ENDIAN).get(activityFlags);
        LOG.debug("Getting activity since {}, flags={}", activityTs, GB.hexdump(activityFlags));
    }

    private void handleActivityData(final byte[] payload) {
        if (payload.length % 32 != 0) {
            LOG.error("Activity data payload size {} not divisible by 32", payload.length);
            return;
        }

        LOG.debug("Got {} activity samples", payload.length / 32);

        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final List<CmfActivitySample> samples = new ArrayList<>();

        while (buf.remaining() > 0) {
            final CmfActivitySample sample = new CmfActivitySample();
            sample.setTimestamp(buf.getInt());
            sample.setSteps(buf.getInt());
            sample.setDistance(buf.getInt());
            sample.setCalories(buf.getInt());

            final byte[] unk = new byte[16];
            buf.get(unk);

            samples.add(sample);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final CmfActivitySampleProvider sampleProvider = new CmfActivitySampleProvider(getDevice(), session);

            for (final CmfActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(sampleProvider);
            }

            LOG.debug("Will persist {} activity samples", samples.size());
            sampleProvider.addGBActivitySamples(samples.toArray(new CmfActivitySample[0]));
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void handleHeartRate(final byte[] payload) {
        if (payload.length % 8 != 0) {
            LOG.error("Heart rate payload size {} not divisible by 8", payload.length);
            return;
        }

        LOG.debug("Got {} heart rate samples", payload.length / 8);

        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final List<CmfHeartRateSample> samples = new ArrayList<>();

        while (buf.remaining() > 0) {
            final CmfHeartRateSample sample = new CmfHeartRateSample();
            sample.setTimestamp(buf.getInt() * 1000L);
            sample.setHeartRate(buf.getInt());

            samples.add(sample);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final CmfHeartRateSampleProvider sampleProvider = new CmfHeartRateSampleProvider(getDevice(), session);

            for (final CmfHeartRateSample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} heart rate samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving heart rate samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private static void handleHeartRateResting(final byte[] payload) {
        // TODO persist resting HR samples;
        LOG.warn("Persisting resting HR samples is not implemented");
    }

    private void handleSleepData(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        LOG.debug("Got sleep data samples");

        final int sessionTimestamp = buf.getInt();
        final int wakeupTime = buf.getInt();
        final byte[] metadata = new byte[10];
        buf.get(metadata);

        final CmfSleepSessionSample sessionSample = new CmfSleepSessionSample();
        sessionSample.setTimestamp(sessionTimestamp * 1000L);
        sessionSample.setWakeupTime(wakeupTime * 1000L);
        sessionSample.setMetadata(metadata);

        final List<CmfSleepStageSample> stageSamples = new ArrayList<>();

        while (buf.remaining() > 0) {
            final CmfSleepStageSample sample = new CmfSleepStageSample();
            sample.setTimestamp(buf.getInt() * 1000L);
            sample.setDuration(buf.getShort());
            sample.setStage(buf.getShort());
            stageSamples.add(sample);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final CmfSleepSessionSampleProvider sampleProvider = new CmfSleepSessionSampleProvider(getDevice(), session);

            sessionSample.setDevice(device);
            sessionSample.setUser(user);

            LOG.debug("Will persist 1 sleep session sample from {} to {}", sessionSample.getTimestamp(), sessionSample.getWakeupTime());
            sampleProvider.addSample(sessionSample);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving sleep session sample", Toast.LENGTH_LONG, GB.ERROR, e);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final CmfSleepStageSampleProvider sampleProvider = new CmfSleepStageSampleProvider(getDevice(), session);

            for (final CmfSleepStageSample sample : stageSamples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} sleep stage samples", stageSamples.size());
            sampleProvider.addSamples(stageSamples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving sleep samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void handleStress(final byte[] payload) {
        if (payload.length % 8 != 0) {
            LOG.error("Stress payload size {} not divisible by 8", payload.length);
            return;
        }

        LOG.debug("Got {} stress samples", payload.length / 8);

        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final List<CmfStressSample> samples = new ArrayList<>();

        while (buf.remaining() > 0) {
            final CmfStressSample sample = new CmfStressSample();
            sample.setTimestamp(buf.getInt() * 1000L);
            sample.setStress(buf.getInt());

            samples.add(sample);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final CmfStressSampleProvider sampleProvider = new CmfStressSampleProvider(getDevice(), session);

            for (final CmfStressSample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} stress samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving stress samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void handleSpo2(final byte[] payload) {
        if (payload.length % 8 != 0) {
            LOG.error("Spo2 payload size {} not divisible by 8", payload.length);
            return;
        }

        LOG.debug("Got {} spo2 samples", payload.length / 8);

        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final List<CmfSpo2Sample> samples = new ArrayList<>();

        while (buf.remaining() > 0) {
            final CmfSpo2Sample sample = new CmfSpo2Sample();
            sample.setTimestamp(buf.getInt() * 1000L);
            sample.setSpo2(buf.getInt());

            samples.add(sample);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final CmfSpo2SampleProvider sampleProvider = new CmfSpo2SampleProvider(getDevice(), session);

            for (final CmfSpo2Sample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} spo2 samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving spo2 samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void handleWorkoutSummary(final byte[] payload) {
        final int bytesPerWorkout;

        if (payload.length % 32 == 0) {
            bytesPerWorkout = 32;
        } else if (payload.length % 54 == 0) {
            bytesPerWorkout = 54;
        } else {
            LOG.error("Workout summary payload size {} not divisible by 32 or 54", payload.length);
            return;
        }

        LOG.debug("Got {} workout summary samples", payload.length / bytesPerWorkout);

        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final CmfWorkoutSummaryParser summaryParser = new CmfWorkoutSummaryParser(getDevice());

        while (buf.remaining() > 0) {
            final byte[] summaryBytes = new byte[bytesPerWorkout];
            buf.get(summaryBytes);

            BaseActivitySummary summary = new BaseActivitySummary();
            summary.setRawSummaryData(summaryBytes);
            summary.setActivityKind(ActivityKind.UNKNOWN.getCode());

            try {
                summary = summaryParser.parseBinaryData(summary);
            } catch (final Exception e) {
                LOG.error("Failed to parse workout summary", e);
                GB.toast(getContext(), "Failed to parse workout summary", Toast.LENGTH_LONG, GB.ERROR, e);
                return;
            }

            if (summary == null) {
                LOG.error("Workout summary is null");
                return;
            }

            summary.setSummaryData(null); // remove json before saving to database

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                final DaoSession session = dbHandler.getDaoSession();
                final Device device = DBHelper.getDevice(getDevice(), session);
                final User user = DBHelper.getUser(session);

                summary.setDevice(device);
                summary.setUser(user);

                LOG.debug("Persisting workout summary for {}", summary.getStartTime());

                session.getBaseActivitySummaryDao().insertOrReplace(summary);
            } catch (final Exception e) {
                GB.toast(getContext(), "Error saving activity summary", Toast.LENGTH_LONG, GB.ERROR, e);
                return;
            }

            // FIXME: This should be set by CmfWorkoutSummaryParser
            if (summaryBytes[30] == 1) {
                activitiesWithGps.add(summary);
            }
        }
    }

    private void handleWorkoutGps(final byte[] payload) {
        if (payload.length % 12 != 0) {
            LOG.error("Workout gps payload size {} not divisible by 12", payload.length);
            return;
        }

        LOG.debug("Got {} workout gps samples", payload.length / 12);

        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final List<CmfWorkoutGpsSample> samples = new ArrayList<>();

        while (buf.remaining() > 0) {
            final CmfWorkoutGpsSample sample = new CmfWorkoutGpsSample();
            sample.setTimestamp(buf.getInt() * 1000L);
            sample.setLongitude(buf.getInt());
            sample.setLatitude(buf.getInt());

            samples.add(sample);
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final Device device = DBHelper.getDevice(getDevice(), session);
            final User user = DBHelper.getUser(session);

            final CmfWorkoutGpsSampleProvider sampleProvider = new CmfWorkoutGpsSampleProvider(getDevice(), session);

            for (final CmfWorkoutGpsSample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
            }

            LOG.debug("Will persist {} workout gps samples", samples.size());
            sampleProvider.addSamples(samples);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving workout gps samples", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void processActivitiesWithGps() {
        LOG.debug("There are {} activities with gps to process", activitiesWithGps.size());

        for (final BaseActivitySummary summary : activitiesWithGps) {
            processGps(summary);
        }

        activitiesWithGps.clear();

        getDevice().unsetBusyTask();
        GB.signalActivityDataFinish(getDevice());
        GB.updateTransferNotification(null, "", false, 100, getContext());
    }

    private void processGps(final BaseActivitySummary summary) {
        final ActivityTrack activityTrack = buildActivityTrack(summary);
        if (activityTrack == null) {
            return;
        }

        // Save the gpx file
        final File gpxFile = exportGpx(summary, activityTrack);
        if (gpxFile == null) {
            return;
        }

        // Update the summary in the db with the gpx path
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(mSupport.getDevice(), session);
            final User user = DBHelper.getUser(session);

            final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
            final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
            qb.where(BaseActivitySummaryDao.Properties.StartTime.eq(summary.getStartTime()));
            qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(device.getId()));
            qb.where(BaseActivitySummaryDao.Properties.UserId.eq(user.getId()));
            final List<BaseActivitySummary> summaries = qb.build().list();

            if (summaries.isEmpty()) {
                LOG.warn("Failed to find existing summary in db - this should never happen");
                return;
            }
            if (summaries.size() > 1) {
                LOG.warn("Found multiple summaries in db - this should never happen");
            }

            final BaseActivitySummary summaryToUpdate = summaries.get(0);
            summaryToUpdate.setGpxTrack(gpxFile.getAbsolutePath());
            session.getBaseActivitySummaryDao().insertOrReplace(summaryToUpdate);
        } catch (final Exception e) {
            LOG.error("Failed to update summary with gpx path", e);
        }
    }

    @Nullable
    private File exportGpx(final BaseActivitySummary summary, final ActivityTrack activityTrack) {
        final GPXExporter exporter = new GPXExporter();
        exporter.setCreator(GBApplication.app().getNameAndVersion());

        final String gpxFileName = FileUtils.makeValidFileName("gadgetbridge-" + DateTimeUtils.formatIso8601(summary.getStartTime()) + ".gpx");
        final File gpxTargetFile;
        try {
            gpxTargetFile = new File(FileUtils.getExternalFilesDir(), gpxFileName);
        } catch (final IOException e) {
            LOG.error("Failed to get external files dir", e);
            return null;
        }

        try {
            exporter.performExport(activityTrack, gpxTargetFile);
        } catch (final ActivityTrackExporter.GPXTrackEmptyException e) {
            LOG.warn("Gpx is empty");
            return null;
        } catch (IOException e) {
            LOG.error("Failed to write gpx", e);
            return null;
        }

        return gpxTargetFile;
    }

    @Nullable
    private ActivityTrack buildActivityTrack(final BaseActivitySummary summary) {
        final ActivityTrack track = new ActivityTrack();
        track.setUser(summary.getUser());
        track.setDevice(summary.getDevice());
        track.setName(createActivityName(summary));

        final List<CmfWorkoutGpsSample> gpsSamples;
        final List<CmfHeartRateSample> hrSamples;
        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();

            final CmfWorkoutGpsSampleProvider gpsSampleProvider = new CmfWorkoutGpsSampleProvider(getDevice(), session);
            gpsSamples = gpsSampleProvider.getAllSamples(summary.getStartTime().getTime(), summary.getEndTime().getTime());

            final CmfHeartRateSampleProvider hrSampleProvider = new CmfHeartRateSampleProvider(getDevice(), session);
            hrSamples = new ArrayList<>(hrSampleProvider.getAllSamples(summary.getStartTime().getTime(), summary.getEndTime().getTime()));
        } catch (final Exception e) {
            LOG.error("Error while building activity track", e);
            return null;
        }

        Collections.sort(hrSamples, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

        for (final CmfWorkoutGpsSample gpsSample : gpsSamples) {
            final ActivityPoint ap = new ActivityPoint(new Date(gpsSample.getTimestamp()));
            final GPSCoordinate coordinate = new GPSCoordinate(
                    gpsSample.getLongitude() / 10000000d,
                    gpsSample.getLatitude() / 10000000d
            );
            ap.setLocation(coordinate);

            final CmfHeartRateSample hrSample = findNearestSample(hrSamples, gpsSample.getTimestamp());
            if (hrSample != null) {
                ap.setHeartRate(hrSample.getHeartRate());
            }

            track.addTrackPoint(ap);
        }

        return track;
    }

    @Nullable
    private CmfHeartRateSample findNearestSample(final List<CmfHeartRateSample> samples, final long timestamp) {
        if (samples.isEmpty()) {
            return null;
        }

        if (timestamp < samples.get(0).getTimestamp()) {
            return samples.get(0);
        }

        if (timestamp > samples.get(samples.size() - 1).getTimestamp()) {
            return samples.get(samples.size() - 1);
        }

        int start = 0;
        int end = samples.size() - 1;

        while (start <= end) {
            final int mid = (start + end) / 2;

            if (timestamp < samples.get(mid).getTimestamp()) {
                end = mid - 1;
            } else if (timestamp > samples.get(mid).getTimestamp()) {
                start = mid + 1;
            } else {
                return samples.get(mid);
            }
        }

        // FIXME return null if too far?

        if (samples.get(start).getTimestamp() - timestamp < timestamp - samples.get(end).getTimestamp()) {
            return samples.get(start);
        }

        return samples.get(end);
    }

    protected static String createActivityName(final BaseActivitySummary summary) {
        String name = summary.getName();
        String nameText = "";
        Long id = summary.getId();
        if (name != null) {
            nameText = name + " - ";
        }
        return nameText + id;
    }

    private Context getContext() {
        return mSupport.getContext();
    }

    private GBDevice getDevice() {
        return mSupport.getDevice();
    }
}
