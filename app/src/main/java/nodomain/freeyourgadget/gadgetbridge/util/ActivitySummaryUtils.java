package nodomain.freeyourgadget.gadgetbridge.util;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;

public final class ActivitySummaryUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryUtils.class);

    private ActivitySummaryUtils() {
        // utility class
    }

    @Nullable
    public static File getTrackFile(final BaseActivitySummary summary) {
        final String gpxTrack = summary.getGpxTrack();
        if (gpxTrack != null) {
            return FileUtils.tryFixPath(new File(gpxTrack));
        }
        final String rawDetails = summary.getRawDetailsPath();
        if (rawDetails != null && rawDetails.endsWith(".fit")) {
            return FileUtils.tryFixPath(new File(rawDetails));
        }
        return null;
    }

    @Nullable
    public static File getGpxFile(final BaseActivitySummary summary) {
        final File trackFile = getTrackFile(summary);
        if (trackFile == null) {
            return null;
        }

        try {
            if (trackFile.getName().endsWith(".gpx")) {
                return trackFile;
            } else if (trackFile.getName().endsWith(".fit")) {
                return convertFitToGpx(summary, trackFile);
            } else {
                LOG.error("Unknown track format for {}", trackFile.getName());
            }
        } catch (final Exception e) {
            LOG.error("Failed to get gpx track", e);
        }

        return null;
    }

    private static File convertFitToGpx(final BaseActivitySummary summary, final File file) throws IOException, ActivityTrackExporter.GPXTrackEmptyException {
        final FitFile fitFile = FitFile.parseIncoming(file);
        final List<ActivityPoint> activityPoints = fitFile.getRecords().stream()
                .filter(r -> r instanceof FitRecord)
                .map(r -> ((FitRecord) r).toActivityPoint())
                .filter(ap -> ap.getLocation() != null)
                .collect(Collectors.toList());

        final ActivityTrack activityTrack = new ActivityTrack();
        activityTrack.setName(summary.getName());
        activityTrack.addTrackPoints(activityPoints);

        final File cacheDir = GBApplication.getContext().getCacheDir();
        final File rawCacheDir = new File(cacheDir, "gpx");
        //noinspection ResultOfMethodCallIgnored
        rawCacheDir.mkdir();
        final File gpxFile = new File(rawCacheDir, file.getName().replace(".fit", ".gpx"));

        final GPXExporter gpxExporter = new GPXExporter();
        gpxExporter.performExport(activityTrack, gpxFile);

        return gpxFile;
    }
}
