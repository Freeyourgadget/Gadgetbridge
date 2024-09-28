package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ALTITUDE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ALTITUDE_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ALTITUDE_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CADENCE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.INTERNAL_HAS_GPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SPEED_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SPEED_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SPEED_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STRIDE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STRIDE_MAX;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.STRIDE_MIN;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_BPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS_PER_SECOND;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_STEPS;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class BangleJSWorkoutParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(BangleJSWorkoutParser.class);

    private final Context mContext;

    public BangleJSWorkoutParser(final Context context) {
        this.mContext = context;
    }

    @Override
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary, final boolean forDetails) {
        if (!forDetails) {
            // Re-parsing the csv is too slow for summary
            return summary;
        }

        if (summary.getRawDetailsPath() == null) {
            return summary;
        }

        final File inputFile = FileUtils.tryFixPath(new File(summary.getRawDetailsPath()));
        if (inputFile == null) {
            return summary;
        }

        final List<BangleJSActivityPoint> points = BangleJSActivityPoint.fromCsv(inputFile);
        if (points == null) {
            return summary;
        }

        summary.setSummaryData(dataFromPoints(points).toString());
        return summary;
    }

    public static ActivitySummaryData dataFromPoints(final List<BangleJSActivityPoint> points) {
        final Accumulator accHeartRate = new Accumulator();
        final Accumulator accSpeed = new Accumulator();
        final Accumulator accAltitude = new Accumulator();
        final Accumulator accStride = new Accumulator();
        double totalDistance = 0;
        int totalSteps = 0;
        long totalTime = 0;
        long totalActiveTime = 0;
        boolean hasGps = false;

        final ActivityUser activityUser = new ActivityUser();

        BangleJSActivityPoint previousPoint = null;
        for (final BangleJSActivityPoint p : points) {
            if (p.getHeartRate() > 0) {
                accHeartRate.add(p.getHeartRate());
            }
            final long timeDiff = previousPoint != null ? p.getTime() - previousPoint.getTime() : 0;
            double distanceDiff;
            // FIXME: GPS data can be missing for some entries which is handled here.
            //  Should use more complex logic to be more accurate. Use interpolation.
            //  Should distances be done via the GPX file we generate instead?
            if (previousPoint != null && previousPoint.getLocation() != null && p.getLocation() != null) {
                distanceDiff = p.getLocation().getDistance(previousPoint.getLocation());
                hasGps = true;
            } else {
                distanceDiff = p.getSteps() * activityUser.getStepLengthCm() * 0.01d;
            }
            if (p.getSteps() > 0) {
                accStride.add(distanceDiff / p.getSteps());
            }
            totalTime += timeDiff;
            totalDistance += distanceDiff;
            if (distanceDiff != 0) {
                totalActiveTime += timeDiff;
            }
            if (timeDiff > 0) {
                accSpeed.add(distanceDiff / (timeDiff / 1000d));
            }

            totalSteps += p.getSteps();

            previousPoint = p;
        }

        final ActivitySummaryData summaryData = new ActivitySummaryData();
        if (totalDistance != 0) {
            summaryData.add(DISTANCE_METERS, (float) totalDistance, UNIT_METERS);
        }
        if (totalActiveTime > 0) {
            summaryData.add(ACTIVE_SECONDS, Math.round(totalActiveTime / 1000d), UNIT_SECONDS);
        }

        if (totalSteps != 0) {
            summaryData.add(STEPS, totalSteps, UNIT_STEPS);
        }
        if (accHeartRate.getCount() > 0) {
            summaryData.add(HR_AVG, accHeartRate.getAverage(), UNIT_BPM);
            summaryData.add(HR_MAX, (int) accHeartRate.getMax(), UNIT_BPM);
            summaryData.add(HR_MIN, (int) accHeartRate.getMin(), UNIT_BPM);
        }
        if (accStride.getCount() > 0) {
            summaryData.add(STRIDE_AVG, accStride.getAverage(), UNIT_METERS);
            summaryData.add(STRIDE_MAX, accStride.getMax(), UNIT_METERS);
            summaryData.add(STRIDE_MIN, accStride.getMin(), UNIT_METERS);
        }
        if (accSpeed.getCount() > 0) {
            summaryData.add(SPEED_AVG, accSpeed.getAverage(), UNIT_METERS_PER_SECOND);
            summaryData.add(SPEED_MAX, accSpeed.getMax(), UNIT_METERS_PER_SECOND);
            summaryData.add(SPEED_MIN, accSpeed.getMin(), UNIT_METERS_PER_SECOND);
        }
        if (accAltitude.getCount() != 0) {
            summaryData.add(ALTITUDE_MAX, accAltitude.getMax(), UNIT_METERS);
            summaryData.add(ALTITUDE_MIN, accAltitude.getMin(), UNIT_METERS);
            summaryData.add(ALTITUDE_AVG, accAltitude.getAverage(), UNIT_METERS);
        }

        if (totalTime > 0) {
            // FIXME: Should cadence be steps/min or half that? https://www.polar.com/blog/what-is-running-cadence/
            //  The Bangle.js App Loader has Cadence = (steps/min)/2,  https://github.com/espruino/BangleApps/blob/master/apps/recorder/interface.html#L103,
            //  as discussed here: https://github.com/espruino/BangleApps/pull/3068#issuecomment-1790293879 .
            summaryData.add(CADENCE_AVG, 0.5 * 60 * totalSteps / (totalTime / 1000d), UNIT_SPM);
        }

        // TODO: Implement hrZones by doing calculations on Gadgetbridge side or make Bangle.js report
        //  this (Karvonen method implemented to a degree in watch app "Run+")?

        // TODO: Does Bangle.js report laps in recorder logs?

        summaryData.add(INTERNAL_HAS_GPS, String.valueOf(hasGps));

        return summaryData;
    }
}
