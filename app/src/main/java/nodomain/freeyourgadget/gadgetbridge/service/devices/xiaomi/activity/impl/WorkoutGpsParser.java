/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WorkoutGpsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutGpsParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        final int sampleSize;
        switch (version) {
            case 1:
            case 2:
                headerSize = 1;
                sampleSize = 18;
                break;
            default:
                LOG.warn("Unable to parse workout gps version {}", fileId.getVersion());
                return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Workout gps Header: {}", GB.hexdump(header));

        if ((buf.limit() - buf.position()) % sampleSize != 0) {
            LOG.warn("Remaining data in the buffer is not a multiple of {}", sampleSize);
            return false;
        }

        final ActivityTrack activityTrack = new ActivityTrack();

        while (buf.position() < buf.limit()) {
            final int ts = buf.getInt();
            final float longitude = buf.getFloat();
            final float latitude = buf.getFloat();
            final int unk1 = buf.getInt(); // 0
            final float speed = (buf.getShort() >> 2) / 10.0f;

            final ActivityPoint ap = new ActivityPoint(new Date(ts * 1000L));
            ap.setLocation(new GPSCoordinate(longitude, latitude, 0));
            activityTrack.addTrackPoint(ap);

            LOG.trace("ActivityPoint: ts={} lon={} lat={} unk1={} speed={}", ts, longitude, latitude, unk1, speed);
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(support.getDevice(), session);
            final User user = DBHelper.getUser(session);

            // Find the matching summary
            final BaseActivitySummary summary = findOrCreateBaseActivitySummary(session, device, user, fileId);

            // Set the info on the activity track
            activityTrack.setUser(user);
            activityTrack.setDevice(device);
            activityTrack.setName(ActivityKind.asString(summary.getActivityKind(), support.getContext()));

            // Save the raw bytes
            final String rawBytesPath = saveRawBytes(fileId, bytes);

            // Save the gpx file
            final GPXExporter exporter = new GPXExporter();
            exporter.setCreator(GBApplication.app().getNameAndVersion());

            final String gpxFileName = FileUtils.makeValidFileName("gadgetbridge-" + DateTimeUtils.formatIso8601(fileId.getTimestamp()) + ".gpx");
            final File gpxTargetFile = new File(FileUtils.getExternalFilesDir(), gpxFileName);

            boolean exportGpxSuccess = true;
            try {
                exporter.performExport(activityTrack, gpxTargetFile);
            } catch (final ActivityTrackExporter.GPXTrackEmptyException ex) {
                exportGpxSuccess = false;
                GB.toast(support.getContext(), "This activity does not contain GPX tracks.", Toast.LENGTH_LONG, GB.ERROR, ex);
            }

            if (exportGpxSuccess) {
                summary.setGpxTrack(gpxTargetFile.getAbsolutePath());
            }
            if (rawBytesPath != null) {
                summary.setRawDetailsPath(rawBytesPath);
            }
            session.getBaseActivitySummaryDao().insertOrReplace(summary);
        } catch (final Exception e) {
            GB.toast(support.getContext(), "Error saving workout gps", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        return true;
    }

    private String saveRawBytes(final XiaomiActivityFileId fileId, final byte[] bytes) {
        try {
            final File targetFolder = new File(FileUtils.getExternalFilesDir(), "rawDetails");
            targetFolder.mkdirs();
            final File targetFile = new File(targetFolder, fileId.getFilename());
            FileOutputStream outputStream = new FileOutputStream(targetFile);
            outputStream.write(fileId.toBytes());
            outputStream.write(bytes);
            outputStream.close();
            return targetFile.getAbsolutePath();
        } catch (final IOException e) {
            LOG.error("Failed to save raw bytes", e);
        }

        return null;
    }
}
