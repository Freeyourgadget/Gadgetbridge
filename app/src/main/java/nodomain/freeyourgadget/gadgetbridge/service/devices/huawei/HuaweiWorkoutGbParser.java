/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.widget.Toast;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * This class parses the Huawei workouts into the table GB uses to show the workouts
 * It also re-parses the unknown data from the workout tables
 * It is a separate class so it can easily be used to re-parse the data without database migrations
 */
public class HuaweiWorkoutGbParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiWorkoutGbParser.class);

    // TODO: Might be nicer to propagate the exceptions, so they can be handled upstream

    public static void parseAllWorkouts() {
        parseUnknownWorkoutData();

        try (DBHandler db = GBApplication.acquireDB()) {
            QueryBuilder<HuaweiWorkoutSummarySample> qb = db.getDaoSession().getHuaweiWorkoutSummarySampleDao().queryBuilder();
            for (HuaweiWorkoutSummarySample summary : qb.listLazy()) {
                parseWorkout(summary.getWorkoutId());
            }
        } catch (Exception e) {
            GB.toast("Exception parsing workouts", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Exception parsing workouts", e);
        }
    }

    /**
     * Parses the unknown data from the workout data table
     */
    private static void parseUnknownWorkoutData() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            QueryBuilder<HuaweiWorkoutDataSample> qb = dbHandler.getDaoSession().getHuaweiWorkoutDataSampleDao().queryBuilder().where(
                    HuaweiWorkoutDataSampleDao.Properties.DataErrorHex.notEq("")
            );
            for (HuaweiWorkoutDataSample sample : qb.build().listLazy()) {
                byte[] data = GB.hexStringToByteArray(new String(sample.getDataErrorHex()));
                Workout.WorkoutData.Response response = new Workout.WorkoutData.Response(data);

                for (Workout.WorkoutData.Response.Data responseData : response.dataList) {
                    byte[] dataErrorHex;
                    if (responseData.unknownData == null)
                        dataErrorHex = null;
                    else
                        dataErrorHex = StringUtils.bytesToHex(responseData.unknownData).getBytes(StandardCharsets.UTF_8);

                    HuaweiWorkoutDataSample dataSample = new HuaweiWorkoutDataSample(
                            sample.getWorkoutId(),
                            responseData.timestamp,
                            responseData.heartRate,
                            responseData.speed,
                            responseData.stepRate,
                            responseData.cadence,
                            responseData.stepLength,
                            responseData.groundContactTime,
                            responseData.impact,
                            responseData.swingAngle,
                            responseData.foreFootLanding,
                            responseData.midFootLanding,
                            responseData.backFootLanding,
                            responseData.eversionAngle,
                            responseData.swolf,
                            responseData.strokeRate,
                            dataErrorHex
                    );

                    dbHandler.getDaoSession().getHuaweiWorkoutDataSampleDao().insertOrReplace(dataSample);
                }
            }
        } catch (Exception e) {
            GB.toast("Exception parsing unknown workout data", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Exception parsing unknown workout data", e);
        }
    }

    public static int huaweiTypeToGbType(byte huaweiType) {
        int type = huaweiType & 0xFF;
        switch (type) {
            case 1:
                return ActivityKind.TYPE_RUNNING;
            case 2:
            case 13:
                return ActivityKind.TYPE_WALKING;
            case 6:
                return ActivityKind.TYPE_SWIMMING;
            case 3:
                return ActivityKind.TYPE_CYCLING;
            case 7:
                return ActivityKind.TYPE_INDOOR_CYCLING;
            case 129:
                return ActivityKind.TYPE_BADMINTON;
            case 130:
                return ActivityKind.TYPE_EXERCISE; // TODO: Tennis
            case 132:
                return ActivityKind.TYPE_BASKETBALL;
            case 133:
                return ActivityKind.TYPE_EXERCISE; // TODO: Volleyball
            case 134:
                return ActivityKind.TYPE_ELLIPTICAL_TRAINER;
            case 135:
                return ActivityKind.TYPE_ROWING_MACHINE;
            case 173:
                return ActivityKind.TYPE_EXERCISE; // TODO: Laser tag
            case 177:
                return ActivityKind.TYPE_EXERCISE; // TODO: stair climbing
            case 196:
                return ActivityKind.TYPE_EXERCISE; // TODO: fishing
            case 216:
                return ActivityKind.TYPE_EXERCISE; // TODO: motor racing
            default:
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    public static void parseWorkout(Long workoutId) {
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            QueryBuilder<HuaweiWorkoutSummarySample> qbSummary = db.getDaoSession().getHuaweiWorkoutSummarySampleDao().queryBuilder().where(
                    HuaweiWorkoutSummarySampleDao.Properties.WorkoutId.eq(workoutId)
            );
            List<HuaweiWorkoutSummarySample> summarySamples = qbSummary.build().list();
            if (summarySamples.size() != 1)
                return;
            HuaweiWorkoutSummarySample summary = summarySamples.get(0);

            QueryBuilder<HuaweiWorkoutDataSample> qbData = db.getDaoSession().getHuaweiWorkoutDataSampleDao().queryBuilder().where(
                    HuaweiWorkoutDataSampleDao.Properties.WorkoutId.eq(workoutId)
            );
            List<HuaweiWorkoutDataSample> dataSamples = qbData.build().list();

            QueryBuilder<HuaweiWorkoutPaceSample> qbPace = db.getDaoSession().getHuaweiWorkoutPaceSampleDao().queryBuilder().where(
                    HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.eq(workoutId)
            );

            long userId = summary.getUserId();
            long deviceId = summary.getDeviceId();
            Date start = new Date(summary.getStartTimestamp() * 1000L);
            Date end = new Date(summary.getEndTimestamp() * 1000L);

            // Avoid duplicates
            QueryBuilder<BaseActivitySummary> qb = db.getDaoSession().getBaseActivitySummaryDao().queryBuilder().where(
                    BaseActivitySummaryDao.Properties.UserId.eq(userId),
                    BaseActivitySummaryDao.Properties.DeviceId.eq(deviceId),
                    BaseActivitySummaryDao.Properties.StartTime.eq(start),
                    BaseActivitySummaryDao.Properties.EndTime.eq(end)
            );
            List<BaseActivitySummary> duplicates = qb.build().list();
            BaseActivitySummary previous = null;
            if (!duplicates.isEmpty())
                previous = duplicates.get(0);

            int type = huaweiTypeToGbType(summary.getType());

            JSONObject jsonObject = new JSONObject();

            // The first few should get auto translated in ActivitySummariesAdapter:fill_dashboard
            JSONObject calories = new JSONObject();
            calories.put("value", summary.getCalories());
            calories.put("unit", "calories_unit");
            jsonObject.put("caloriesBurnt", calories);

            JSONObject distance = new JSONObject();
            distance.put("value", summary.getDistance());
            distance.put("unit", "meters");
            jsonObject.put("distanceMeters", distance);

            JSONObject steps = new JSONObject();
            steps.put("value", summary.getStepCount());
            steps.put("unit", "steps_unit");
            jsonObject.put("steps", steps);

            JSONObject time = new JSONObject();
            time.put("value", summary.getDuration());
            time.put("unit", "seconds");
            jsonObject.put("activeSeconds", time);

            JSONObject status = new JSONObject();
            status.put("value", summary.getStatus() & 0xFF);
            status.put("unit", "");
            jsonObject.put(GBApplication.getContext().getString(R.string.status), status);

            JSONObject typeJson = new JSONObject();
            typeJson.put("value", summary.getType() & 0xFF);
            typeJson.put("unit", "");
            jsonObject.put(GBApplication.getContext().getString(R.string.watchface_dialog_widget_type), typeJson);

            if (summary.getStrokes() != -1) {
                JSONObject strokesJson = new JSONObject();
                strokesJson.put("value", summary.getStrokes());
                strokesJson.put("unit", GBApplication.getContext().getString(R.string.strokes_unit));
                jsonObject.put(GBApplication.getContext().getString(R.string.Strokes), strokesJson);
            }

            if (summary.getAvgStrokeRate() != -1) {
                JSONObject avgStrokeRateJson = new JSONObject();
                avgStrokeRateJson.put("value", summary.getAvgStrokeRate());
                avgStrokeRateJson.put("unit", ""); // TODO: find out unit
                jsonObject.put(GBApplication.getContext().getString(R.string.avgStrokeRate), avgStrokeRateJson);
            }

            if (summary.getPoolLength() != -1) {
                JSONObject poolLengthJson = new JSONObject();
                poolLengthJson.put("value", summary.getPoolLength());
                poolLengthJson.put("unit", GBApplication.getContext().getString(R.string.cm));
                jsonObject.put(GBApplication.getContext().getString(R.string.laneLength), poolLengthJson);
            }

            if (summary.getLaps() != -1) {
                JSONObject lapsJson = new JSONObject();
                lapsJson.put("value", summary.getLaps());
                lapsJson.put("unit", GBApplication.getContext().getString(R.string.laps_unit));
                jsonObject.put(GBApplication.getContext().getString(R.string.laps), lapsJson);
            }

            if (summary.getAvgSwolf() != -1) {
                JSONObject avgSwolfJson = new JSONObject();
                avgSwolfJson.put("value", summary.getAvgSwolf());
                avgSwolfJson.put("unit", "");
                jsonObject.put(GBApplication.getContext().getString(R.string.swolfAvg), avgSwolfJson);
            }

            boolean unknownData = false;
            if (dataSamples.size() != 0) {
                int speed = 0;
                int speedCount = 0;
                boolean stepRatePresent = false;
                int stepRate = 0;
                int avgStepRate = 0;
                int cadence = 0;
                int cadenceCount = 0;
                int stepLength = 0;
                int stepLengthCount = 0;
                int groundContactTime = 0;
                int groundContactTimeCount = 0;
                int impact = 0;
                int impactCount = 0;
                int maxImpact = 0;
                int swingAngle = 0;
                int swingAngleCount = 0;
                boolean footLandingPresent = false;
                int foreFootLanding = 0;
                int midFootLanding = 0;
                int backFootLanding = 0;
                int eversionAngle = 0;
                int eversionAngleCount = 0;
                int maxEversionAngle = 0;
                int swolf = 0;
                int swolfCount = 0;
                int maxSwolf = 0;
                int minSwolf = 0;
                int strokeRate = 0;
                int strokeRateCount = 0;
                int maxStrokeRate = 0;
                int heartRate = 0;
                int heartRateCount = 0;
                int maxHeartRate = 0;
                int minHeartRate = Integer.MAX_VALUE;
                for (HuaweiWorkoutDataSample dataSample : dataSamples) {
                    if (dataSample.getSpeed() != -1) {
                        speed += dataSample.getSpeed();
                        speedCount += 1;
                    }
                    if (dataSample.getStepRate() != -1) {
                        stepRate += dataSample.getStepRate();
                        stepRatePresent = true;
                    }
                    if (dataSample.getCadence() != -1) {
                        cadence += dataSample.getCadence();
                        cadenceCount += 1;
                    }
                    if (dataSample.getStepLength() != -1) {
                        stepLength += dataSample.getStepLength();
                        stepLengthCount += 1;
                    }
                    if (dataSample.getGroundContactTime() != -1) {
                        groundContactTime += dataSample.getGroundContactTime();
                        groundContactTimeCount += 1;
                    }
                    if (dataSample.getImpact() != -1) {
                        impact += dataSample.getImpact();
                        impactCount += 1;
                        if (dataSample.getImpact() > maxImpact)
                            maxImpact = dataSample.getImpact();
                    }
                    if (dataSample.getSwingAngle() != -1) {
                        swingAngle += dataSample.getSwingAngle();
                        swingAngleCount += 1;
                    }
                    if (dataSample.getForeFootLanding() != -1) {
                        foreFootLanding += dataSample.getForeFootLanding();
                        footLandingPresent = true;
                    }
                    if (dataSample.getMidFootLanding() != -1) {
                        midFootLanding += dataSample.getMidFootLanding();
                        footLandingPresent = true;
                    }
                    if (dataSample.getBackFootLanding() != -1) {
                        backFootLanding += dataSample.getBackFootLanding();
                        footLandingPresent = true;
                    }
                    if (dataSample.getEversionAngle() != -1) {
                        eversionAngle += dataSample.getEversionAngle();
                        eversionAngleCount += 1;
                        if (dataSample.getEversionAngle() > maxEversionAngle)
                            maxEversionAngle = dataSample.getEversionAngle();
                    }
                    if (dataSample.getSwolf() != -1) {
                        swolf += dataSample.getSwolf();
                        swolfCount += 1;
                        if (dataSample.getSwolf() > maxSwolf)
                            maxSwolf = dataSample.getSwolf();
                        if (dataSample.getSwolf() < minSwolf)
                            minSwolf = dataSample.getSwolf();
                    }
                    if (dataSample.getStrokeRate() != -1) {
                        strokeRate += dataSample.getStrokeRate();
                        strokeRateCount += 1;
                        if (dataSample.getStrokeRate() > maxStrokeRate)
                            maxStrokeRate = dataSample.getStrokeRate();
                    }
                    if (dataSample.getHeartRate() != -1 && dataSample.getHeartRate() != 0) {
                        int hr = dataSample.getHeartRate() & 0xff;
                        heartRate += hr;
                        heartRateCount += 1;
                        if (hr > maxHeartRate)
                            maxHeartRate = hr;
                        if (hr < minHeartRate)
                            minHeartRate = hr;
                    }
                    if (dataSample.getDataErrorHex() != null)
                        unknownData = true;
                }

                // Average the things that should be averaged
                if (speedCount > 0)
                    speed = speed / speedCount;
                if (cadenceCount > 0)
                    cadence = cadence / cadenceCount;
                if (summary.getDuration() > 60)
                    avgStepRate = stepRate / (summary.getDuration() / 60); // steps per minute
                if (stepLengthCount > 0)
                    stepLength = stepLength / stepLengthCount;
                if (groundContactTimeCount > 0)
                    groundContactTime = groundContactTime / groundContactTimeCount;
                if (impactCount > 0)
                    impact = impact / impactCount;
                if (swingAngleCount > 0)
                    swingAngle = swingAngle / swingAngleCount;
                if (eversionAngleCount > 0)
                    eversionAngle = eversionAngle / eversionAngleCount;
                if (swolfCount > 0)
                    swolf = swolf / swolfCount;
                if (strokeRateCount > 0)
                    strokeRate = strokeRate / strokeRateCount;
                if (heartRateCount > 0)
                    heartRate = heartRate / heartRateCount;

                if (speedCount > 0) {
                    JSONObject speedJson = new JSONObject();
                    speedJson.put("value", speed / 10);
                    speedJson.put("unit", GBApplication.getContext().getString(R.string.meters_second));
                    jsonObject.put(GBApplication.getContext().getString(R.string.averageSpeed), speedJson);
                }

                if (stepRatePresent) {
                    JSONObject stepRateSumJson = new JSONObject();
                    stepRateSumJson.put("value", stepRate);
                    stepRateSumJson.put("unit", GBApplication.getContext().getString(R.string.steps_unit));
                    jsonObject.put(GBApplication.getContext().getString(R.string.stepRateSum), stepRateSumJson);

                    JSONObject stepRateAvgJson = new JSONObject();
                    stepRateAvgJson.put("value", avgStepRate);
                    stepRateAvgJson.put("unit", GBApplication.getContext().getString(R.string.spm));
                    jsonObject.put(GBApplication.getContext().getString(R.string.stepRateAvg), stepRateAvgJson);
                }

                if (cadenceCount > 0) {
                    JSONObject cadenceJson = new JSONObject();
                    cadenceJson.put("value", cadence);
                    cadenceJson.put("unit", GBApplication.getContext().getString(R.string.spm));
                    jsonObject.put(GBApplication.getContext().getString(R.string.averageCadence), cadenceJson);
                }

                if (stepLengthCount > 0) {
                    JSONObject stepLengthJson = new JSONObject();
                    stepLengthJson.put("value", stepLength);
                    stepLengthJson.put("unit", GBApplication.getContext().getString(R.string.cm));
                    jsonObject.put(GBApplication.getContext().getString(R.string.stepLengthAvg), stepLengthJson);
                }

                if (groundContactTimeCount > 0) {
                    JSONObject groundContactTimeJson = new JSONObject();
                    groundContactTimeJson.put("value", groundContactTime);
                    groundContactTimeJson.put("unit", GBApplication.getContext().getString(R.string.milliseconds));
                    jsonObject.put(GBApplication.getContext().getString(R.string.groundContactTimeAvg), groundContactTimeJson);
                }

                if (impactCount > 0) {
                    JSONObject impactJson = new JSONObject();
                    impactJson.put("value", impact);
                    impactJson.put("unit", "g");
                    jsonObject.put(GBApplication.getContext().getString(R.string.impactAvg), impactJson);

                    JSONObject maxImpactJson = new JSONObject();
                    maxImpactJson.put("value", maxImpact);
                    maxImpactJson.put("unit", "g");
                    jsonObject.put(GBApplication.getContext().getString(R.string.impactMax), maxImpactJson);
                }

                if (swingAngleCount > 0) {
                    JSONObject swingAngleJson = new JSONObject();
                    swingAngleJson.put("value", swingAngle);
                    swingAngleJson.put("unit", GBApplication.getContext().getString(R.string.degrees));
                    jsonObject.put(GBApplication.getContext().getString(R.string.swingAngleAvg), swingAngleJson);
                }

                if (footLandingPresent) {
                    JSONObject foreFootLandingJson = new JSONObject();
                    foreFootLandingJson.put("value", foreFootLanding);
                    foreFootLandingJson.put("unit", "");
                    jsonObject.put(GBApplication.getContext().getString(R.string.foreFootLandings), foreFootLandingJson);

                    JSONObject midFootLandingJson = new JSONObject();
                    midFootLandingJson.put("value", midFootLanding);
                    midFootLandingJson.put("unit", "");
                    jsonObject.put(GBApplication.getContext().getString(R.string.midFootLandings), midFootLandingJson);

                    JSONObject backFootLandingJson = new JSONObject();
                    backFootLandingJson.put("value", backFootLanding);
                    backFootLandingJson.put("unit", "");
                    jsonObject.put(GBApplication.getContext().getString(R.string.backFootLandings), backFootLandingJson);
                }

                if (eversionAngleCount > 0) {
                    JSONObject eversionAngleJson = new JSONObject();
                    eversionAngleJson.put("value", eversionAngle);
                    eversionAngleJson.put("unit", GBApplication.getContext().getString(R.string.degrees));
                    jsonObject.put(GBApplication.getContext().getString(R.string.eversionAngleAvg), eversionAngleJson);

                    JSONObject maxEversionAngleJson = new JSONObject();
                    maxEversionAngleJson.put("value", maxEversionAngle);
                    maxEversionAngleJson.put("unit", GBApplication.getContext().getString(R.string.degrees));
                    jsonObject.put(GBApplication.getContext().getString(R.string.eversionAngleMax), maxEversionAngleJson);
                }

                if (swolfCount > 0) {
                    JSONObject swolfJson = new JSONObject();
                    swolfJson.put("value", swolf);
                    swolfJson.put("unit", "");
                    jsonObject.put(GBApplication.getContext().getString(R.string.swolfAvg), swolfJson);

                    JSONObject maxSwolfJson = new JSONObject();
                    maxSwolfJson.put("value", maxSwolf);
                    maxSwolfJson.put("unit", "");
                    jsonObject.put(GBApplication.getContext().getString(R.string.swolfMax), maxSwolfJson);

                    JSONObject minSwolfJson = new JSONObject();
                    minSwolfJson.put("value", minSwolf);
                    minSwolfJson.put("unit", "");
                    jsonObject.put(GBApplication.getContext().getString(R.string.swolfMin), minSwolfJson);
                }

                if (strokeRateCount > 0) {
                    JSONObject strokeRateJson = new JSONObject();
                    strokeRateJson.put("value", strokeRate);
                    strokeRateJson.put("unit", ""); // TODO: find out unit?
                    jsonObject.put(GBApplication.getContext().getString(R.string.avgStrokeRate), strokeRateJson);

                    JSONObject maxStrokeRateJson = new JSONObject();
                    maxStrokeRateJson.put("value", maxStrokeRate);
                    maxStrokeRateJson.put("unit", ""); // TODO: find out unit?
                    jsonObject.put(GBApplication.getContext().getString(R.string.maxStrokeRate), maxStrokeRateJson);
                }

                if (heartRateCount > 0) {
                    JSONObject heartRateJson = new JSONObject();
                    heartRateJson.put("value", heartRate);
                    heartRateJson.put("unit", GBApplication.getContext().getString(R.string.bpm));
                    jsonObject.put(GBApplication.getContext().getString(R.string.averageHR), heartRateJson);

                    JSONObject maxHeartRateJson = new JSONObject();
                    maxHeartRateJson.put("value", maxHeartRate);
                    maxHeartRateJson.put("unit", GBApplication.getContext().getString(R.string.bpm));
                    jsonObject.put(GBApplication.getContext().getString(R.string.maxHR), maxHeartRateJson);

                    JSONObject minHeartRateJson = new JSONObject();
                    minHeartRateJson.put("value", minHeartRate);
                    minHeartRateJson.put("unit", GBApplication.getContext().getString(R.string.bpm));
                    jsonObject.put(GBApplication.getContext().getString(R.string.minHR), minHeartRateJson);
                }
            }

            ListIterator<HuaweiWorkoutPaceSample> it = qbPace.build().listIterator();
            int count = 0;
            int pace = 0;
            while (it.hasNext()) {
                int index = it.nextIndex();
                HuaweiWorkoutPaceSample sample = it.next();

                count += 1;
                pace += sample.getPace();

                JSONObject paceDistance = new JSONObject();
                paceDistance.put("value", sample.getDistance());
                paceDistance.put("unit", GBApplication.getContext().getString(R.string.km));
                jsonObject.put(String.format(GBApplication.getLanguage() , GBApplication.getContext().getString(R.string.fmtPaceDistance), index), paceDistance);

                JSONObject paceType = new JSONObject();
                paceType.put("value", sample.getType());
                paceType.put("unit", ""); // TODO: not sure
                jsonObject.put(String.format(GBApplication.getLanguage(), GBApplication.getContext().getString(R.string.fmtPaceType), index), paceType);

                JSONObject pacePace = new JSONObject();
                pacePace.put("value", sample.getPace());
                pacePace.put("unit", GBApplication.getContext().getString(R.string.seconds_km));
                jsonObject.put(String.format(GBApplication.getLanguage(), GBApplication.getContext().getString(R.string.fmtPacePace), index), pacePace);

                if (sample.getCorrection() != 0) {
                    JSONObject paceCorrection = new JSONObject();
                    paceCorrection.put("value", sample.getCorrection() / 10);
                    paceCorrection.put("unit", GBApplication.getContext().getString(R.string.meters));
                    jsonObject.put(String.format(GBApplication.getLanguage(), GBApplication.getContext().getString(R.string.fmtPaceCorrection), index), paceCorrection);
                }
            }

            if (count != 0) {
                // TODO: should probably be split on type?
                JSONObject avgPace = new JSONObject();
                avgPace.put("value", pace / count);
                avgPace.put("unit", "seconds_km");
                jsonObject.put("Average pace", avgPace); // TODO: translatable string
            }

            if (unknownData) {
                JSONObject unknownDataJson = new JSONObject();
                unknownDataJson.put("value", GBApplication.getContext().getString(R.string.yes).toUpperCase());
                unknownDataJson.put("unit", "string");
                jsonObject.put(GBApplication.getContext().getString(R.string.unknownDataEncountered), unknownDataJson);
            }

            BaseActivitySummary baseSummary;
            if (previous == null) {
                baseSummary = new BaseActivitySummary(
                        null,
                        "Workout " + summary.getWorkoutNumber(),
                        start,
                        end,
                        type,
                        null,
                        null,
                        null,
                        null,
                        null,
                        deviceId,
                        userId,
                        jsonObject.toString(),
                        null
                );
            } else {
                baseSummary = new BaseActivitySummary(
                        previous.getId(),
                        previous.getName(),
                        start,
                        end,
                        type,
                        previous.getBaseLongitude(),
                        previous.getBaseLatitude(),
                        previous.getBaseAltitude(),
                        previous.getGpxTrack(),
                        previous.getRawDetailsPath(),
                        deviceId,
                        userId,
                        jsonObject.toString(),
                        null
                );
            }
            db.getDaoSession().getBaseActivitySummaryDao().insertOrReplace(baseSummary);
        } catch (Exception e) {
            GB.toast("Exception parsing workout data", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Exception parsing workout data", e);
        }
    }
}
