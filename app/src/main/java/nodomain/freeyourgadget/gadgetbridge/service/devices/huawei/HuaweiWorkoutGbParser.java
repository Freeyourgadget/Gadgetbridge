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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity.ActivityType;
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

            // TODO: Use translatable strings

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
            jsonObject.put("Status", status);

            JSONObject typeJson = new JSONObject();
            typeJson.put("value", summary.getType() & 0xFF);
            typeJson.put("unit", "");
            jsonObject.put("Type", typeJson);

            JSONObject strokesJson = new JSONObject();
            strokesJson.put("value", summary.getStrokes());
            strokesJson.put("unit", "");
            jsonObject.put("Strokes", strokesJson);

            JSONObject avgStrokeRateJson = new JSONObject();
            avgStrokeRateJson.put("value", summary.getAvgStrokeRate());
            avgStrokeRateJson.put("unit", "");
            jsonObject.put("Average reported stroke rate", avgStrokeRateJson);

            JSONObject poolLengthJson = new JSONObject();
            poolLengthJson.put("value", summary.getPoolLength());
            poolLengthJson.put("unit", "cm");
            jsonObject.put("Pool length", poolLengthJson);

            JSONObject lapsJson = new JSONObject();
            lapsJson.put("value", summary.getLaps());
            lapsJson.put("unit", "");
            jsonObject.put("Laps", lapsJson);

            JSONObject avgSwolfJson = new JSONObject();
            avgSwolfJson.put("value", summary.getAvgSwolf());
            avgSwolfJson.put("unit", "");
            jsonObject.put("Average reported swolf", avgSwolfJson);

            boolean unknownData = false;
            if (dataSamples.size() != 0) {
                int speed = 0;
                int stepRate = 0;
                int cadence = 0;
                int stepLength = 0;
                int groundContactTime = 0;
                int impact = 0;
                int maxImpact = 0;
                int swingAngle = 0;
                int foreFootLanding = 0;
                int midFootLanding = 0;
                int backFootLanding = 0;
                int eversionAngle = 0;
                int maxEversionAngle = 0;
                int swolf = 0;
                int maxSwolf = 0;
                int strokeRate = 0;
                int maxStrokeRate = 0;
                for (HuaweiWorkoutDataSample dataSample : dataSamples) {
                    speed += dataSample.getSpeed();
                    stepRate += dataSample.getStepRate();
                    cadence += dataSample.getCadence();
                    stepLength += dataSample.getStepLength();
                    groundContactTime += dataSample.getGroundContactTime();
                    impact += dataSample.getImpact();
                    if (dataSample.getImpact() > maxImpact)
                        maxImpact = dataSample.getImpact();
                    swingAngle += dataSample.getSwingAngle();
                    foreFootLanding += dataSample.getForeFootLanding();
                    midFootLanding += dataSample.getMidFootLanding();
                    backFootLanding += dataSample.getBackFootLanding();
                    eversionAngle += dataSample.getEversionAngle();
                    if (dataSample.getEversionAngle() > maxEversionAngle)
                        maxEversionAngle = dataSample.getEversionAngle();
                    swolf += dataSample.getSwolf();
                    if (dataSample.getSwolf() > maxSwolf)
                        maxSwolf = dataSample.getSwolf();
                    strokeRate += dataSample.getStrokeRate();
                    if (dataSample.getStrokeRate() > maxStrokeRate)
                        maxStrokeRate = dataSample.getStrokeRate();
                    if (dataSample.getDataErrorHex() != null)
                        unknownData = true;
                }
                // Average the things that should probably be averaged
                speed = speed / dataSamples.size();
                cadence = cadence / dataSamples.size();
                int avgStepRate = stepRate / (summary.getDuration() / 60); // steps per minute

                stepLength = stepLength / dataSamples.size();
                groundContactTime = groundContactTime / dataSamples.size();
                impact = impact / dataSamples.size();
                swingAngle = swingAngle / dataSamples.size();
                eversionAngle = eversionAngle / dataSamples.size();
                swolf = swolf / dataSamples.size();
                strokeRate = strokeRate / dataSamples.size();

                JSONObject speedJson = new JSONObject();
                speedJson.put("value", speed);
                speedJson.put("unit", "cm/s");
                jsonObject.put("Reported speed (avg)", speedJson);

                JSONObject stepRateSumJson = new JSONObject();
                stepRateSumJson.put("value", stepRate);
                stepRateSumJson.put("unit", "");
                jsonObject.put("Step rate (sum)", stepRateSumJson);

                JSONObject stepRateAvgJson = new JSONObject();
                stepRateAvgJson.put("value", avgStepRate);
                stepRateAvgJson.put("unit", "steps/min");
                jsonObject.put("Step rate (avg)", stepRateAvgJson);

                JSONObject cadenceJson = new JSONObject();
                cadenceJson.put("value", cadence);
                cadenceJson.put("unit", "steps/min");
                jsonObject.put("Cadence (avg)", cadenceJson);

                JSONObject stepLengthJson = new JSONObject();
                stepLengthJson.put("value", stepLength);
                stepLengthJson.put("unit", "cm");
                jsonObject.put("Step Length (avg)", stepLengthJson);

                JSONObject groundContactTimeJson = new JSONObject();
                groundContactTimeJson.put("value", groundContactTime);
                groundContactTimeJson.put("unit", "milliseconds");
                jsonObject.put("Ground contact time (avg)", groundContactTimeJson);

                JSONObject impactJson = new JSONObject();
                impactJson.put("value", impact);
                impactJson.put("unit", "g");
                jsonObject.put("Impact (avg)", impactJson);

                JSONObject maxImpactJson = new JSONObject();
                maxImpactJson.put("value", maxImpact);
                maxImpactJson.put("unit", "g");
                jsonObject.put("Impact (max)", maxImpactJson);

                JSONObject swingAngleJson = new JSONObject();
                swingAngleJson.put("value", swingAngle);
                swingAngleJson.put("unit", "degrees");
                jsonObject.put("Swing angle (avg)", swingAngleJson);

                JSONObject foreFootLandingJson = new JSONObject();
                foreFootLandingJson.put("value", foreFootLanding);
                foreFootLandingJson.put("unit", "");
                jsonObject.put("Fore foot landings", foreFootLandingJson);

                JSONObject midFootLandingJson = new JSONObject();
                midFootLandingJson.put("value", midFootLanding);
                midFootLandingJson.put("unit", "");
                jsonObject.put("Mid foot landings", midFootLandingJson);

                JSONObject backFootLandingJson = new JSONObject();
                backFootLandingJson.put("value", backFootLanding);
                backFootLandingJson.put("unit", "");
                jsonObject.put("Back foot landings", backFootLandingJson);

                JSONObject eversionAngleJson = new JSONObject();
                eversionAngleJson.put("value", eversionAngle);
                eversionAngleJson.put("unit", "degrees");
                jsonObject.put("Eversion angle (avg)", eversionAngleJson);

                JSONObject maxEversionAngleJson = new JSONObject();
                maxEversionAngleJson.put("value", maxEversionAngle);
                maxEversionAngleJson.put("unit", "degrees");
                jsonObject.put("Eversion angle (max)", maxEversionAngleJson);

                JSONObject swolfJson = new JSONObject();
                swolfJson.put("value", swolf);
                swolfJson.put("unit", "");
                jsonObject.put("Swolf (avg calculated)", swolfJson);

                JSONObject maxSwolfJson = new JSONObject();
                maxSwolfJson.put("value", maxSwolf);
                maxSwolfJson.put("unit", "");
                jsonObject.put("Swolf (max)", maxSwolfJson);

                JSONObject strokeRateJson = new JSONObject();
                strokeRateJson.put("value", strokeRate);
                strokeRateJson.put("unit", "");
                jsonObject.put("Stroke rate (avg calculated)", strokeRateJson);

                JSONObject maxStrokeRateJson = new JSONObject();
                maxStrokeRateJson.put("value", maxStrokeRate);
                maxStrokeRateJson.put("unit", "");
                jsonObject.put("Stroke rate (max)", maxStrokeRateJson);
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
                paceDistance.put("unit", "kilometers");
                jsonObject.put(String.format(GBApplication.getLanguage() , "Pace %d distance", index), paceDistance);

                JSONObject paceType = new JSONObject();
                paceType.put("value", sample.getType());
                paceType.put("unit", ""); // TODO: not sure
                jsonObject.put(String.format(GBApplication.getLanguage(), "Pace %d type", index), paceType);

                JSONObject pacePace = new JSONObject();
                pacePace.put("value", sample.getPace());
                pacePace.put("unit", "seconds_km");
                jsonObject.put(String.format(GBApplication.getLanguage(), "Pace %d pace", index), pacePace);

                if (sample.getCorrection() != 0) {
                    JSONObject paceCorrection = new JSONObject();
                    paceCorrection.put("value", sample.getCorrection());
                    paceCorrection.put("unit", "m");
                    jsonObject.put(String.format(GBApplication.getLanguage(), "Pace %d correction", index), paceCorrection);
                }
            }

            if (count != 0) {
                JSONObject avgPace = new JSONObject();
                avgPace.put("value", pace / count);
                avgPace.put("unit", "seconds_km");
                jsonObject.put("Average pace", avgPace);
            }

            if (unknownData) {
                JSONObject unknownDataJson = new JSONObject();
                unknownDataJson.put("value", "YES");
                unknownDataJson.put("unit", "string");

                jsonObject.put("Unknown data encountered", unknownDataJson);
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
