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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import de.greenrobot.dao.query.CloseableListIterator;
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
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
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

    public enum HuaweiActivityType {
        // Based on nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport

        RUNNING(1, ActivityKind.RUNNING),
        WALKING(2, ActivityKind.WALKING),
        CYCLING(3, ActivityKind.CYCLING),
        INDOOR_RUN(5, ActivityKind.INDOOR_RUNNING),
        POOL_SWIM(6, ActivityKind.POOL_SWIM),
        INDOOR_CYCLE(7, ActivityKind.INDOOR_CYCLING),
        OPEN_WATER_SWIM(8, ActivityKind.SWIMMING_OPENWATER),
        INDOOR_WALK(13, ActivityKind.INDOOR_WALKING),
        JUMP_ROPING(21, ActivityKind.JUMP_ROPING),
        PING_PONG(128, ActivityKind.PINGPONG),
        BADMINTON(129, ActivityKind.BADMINTON),
        TENNIS(130, ActivityKind.TENNIS),
        SOCCER(131, ActivityKind.SOCCER),
        BASKETBALL(132, ActivityKind.BASKETBALL),
        VOLLEYBALL(133, ActivityKind.VOLLEYBALL),
        ELLIPTICAL_TRAINER(134, ActivityKind.ELLIPTICAL_TRAINER),
        ROWING_MACHINE(135, ActivityKind.ROWING_MACHINE),
        STEPPER(136, ActivityKind.STEPPER),
        YOGA(137, ActivityKind.YOGA),
        PILATES(138, ActivityKind.PILATES),
        AEROBICS(139, ActivityKind.AEROBICS),
        STRENGTH_TRAINING(140, ActivityKind.STRENGTH_TRAINING),
        SPINNING(141, ActivityKind.SPINNING),
        AIR_WALKER(142, ActivityKind.AIR_WALKER),
        HIIT(143, ActivityKind.HIIT),
        CROSSFIT(145, ActivityKind.CROSSFIT),
        FUNCTIONAL_TRAINING(146, ActivityKind.FUNCTIONAL_TRAINING),
        PHYSICAL_TRAINING(147, ActivityKind.PHYSICAL_TRAINING),
        TAEKWONDO(148, ActivityKind.TAEKWONDO),
        BOXING(149, ActivityKind.BOXING),
        FREE_SPARRING(150, ActivityKind.FREE_SPARRING),
        KARATE(151, ActivityKind.KARATE),
        FENCING(152, ActivityKind.FENCING),
        BELLY_DANCE(153, ActivityKind.BELLY_DANCE),
        JAZZ_DANCE(154, ActivityKind.JAZZ_DANCE),
        LATIN_DANCE(155, ActivityKind.LATIN_DANCE),
        BALLET(156, ActivityKind.BALLET),
        CORE_TRAINING(157, ActivityKind.CORE_TRAINING),
        BODY_COMBAT(158, ActivityKind.BODY_COMBAT),
        KENDO(159, ActivityKind.KENDO),
        SINGLE_BAR(160, ActivityKind.HORIZONTAL_BAR),
        PARALLEL_BARS(161, ActivityKind.PARALLEL_BARS),
        STREET_DANCE(162, ActivityKind.STREET_DANCE),
        ROLLER_SKATING(163, ActivityKind.ROLLER_SKATING),
        MARTIAL_ARTS(164, ActivityKind.MARTIAL_ARTS),
        PLAZA_DANCING(165, ActivityKind.PLAZA_DANCING),
        TAI_CHI(166, ActivityKind.TAI_CHI),
        DANCE(167, ActivityKind.DANCE),
        HULA_HOOP(168, ActivityKind.HULA_HOOP),
        FRISBEE(169, ActivityKind.FRISBEE),
        DARTS(170, ActivityKind.DARTS),
        ARCHERY(171, ActivityKind.ARCHERY),
        HORSE_RIDING(172, ActivityKind.HORSE_RIDING),
        LASER_TAG(173, ActivityKind.LASER_TAG),
        KITE_FLYING(174, ActivityKind.KITE_FLYING),
        TUG_OF_WAR(175, ActivityKind.TUG_OF_WAR),
        SWINGING(176, ActivityKind.SWING),
        STAIR_CLIMBING(177, ActivityKind.STAIRS),
        OBSTACLE_RACE(178, ActivityKind.OBSTACLE_RACE),
        BILLIARD_POOL(179, ActivityKind.BILLIARD_POOL),
        BOWLING(180, ActivityKind.BOWLING),
        SHUTTLECOCK(181, ActivityKind.SHUTTLECOCK),
        HANDBALL(182, ActivityKind.HANDBALL),
        BASEBALL(183, ActivityKind.BASEBALL),
        SOFTBALL(184, ActivityKind.SOFTBALL),
        CRICKET(185, ActivityKind.CRICKET),
        RUGBY(186, ActivityKind.RUGBY),
        BEACH_SOCCER(187, ActivityKind.BEACH_SOCCER),
        BEACH_VOLLEYBALL(188, ActivityKind.BEACH_VOLLEYBALL),
        GATEBALL(189, ActivityKind.GATEBALL),
        HOCKEY(190, ActivityKind.HOCKEY),
        SQUASH(191, ActivityKind.SQUASH),
        SEPAK_TAKRAW(192, ActivityKind.SEPAK_TAKRAW),
        DODGEBALL(193, ActivityKind.DODGEBALL),
        SAILING(194, ActivityKind.SAILING),
        SURFING(195, ActivityKind.SURFING),
        FISHING(196, ActivityKind.FISHING),
        RAFTING(197, ActivityKind.RAFTING),
        DRAGON_BOATING(198, ActivityKind.DRAGON_BOAT),
        CANOEING(199, ActivityKind.CANOEING),
        ROWING(200, ActivityKind.ROWING),
        WATER_SCOOTER(201, ActivityKind.WATER_SCOOTER),
        STAND_UP_PADDLEBOARDING(202, ActivityKind.STAND_UP_PADDLEBOARDING),
        ICE_SKATING(203, ActivityKind.ICE_SKATING),
        ICE_HOCKEY(204, ActivityKind.ICE_HOCKEY),
        CURLING(205, ActivityKind.CURLING),
        BOBSLEIGH(206, ActivityKind.BOBSLEIGH),
        SLEDDING(207, ActivityKind.SLEDDING),
        BIATHLON(208, ActivityKind.BIATHLON),
        SKATEBOARDING(209, ActivityKind.SKATEBOARDING),
        ROCK_CLIMBING(210, ActivityKind.ROCK_CLIMBING),
        BUNGEE_JUMPING(211, ActivityKind.BUNGEE_JUMPING),
        PARKOUR(212, ActivityKind.PARKOUR),
        BMX(213, ActivityKind.BMX),
        ORIENTEERING(214, ActivityKind.ORIENTEERING),
        PARACHUTING(215, ActivityKind.PARACHUTING),
        MOTOR_AUTO_RACING(216, ActivityKind.AUTO_RACING),
        ESPORTS(223, ActivityKind.ESPORTS),
        PADEL(224, ActivityKind.PADEL),
        OTHER(255, ActivityKind.EXERCISE)
        ;

        private final byte type;
        private final ActivityKind activityKind;

        HuaweiActivityType(final int type, final ActivityKind activityKind) {
            this.type = (byte) type;
            this.activityKind = activityKind;
        }

        public ActivityKind getActivityKind() {
            return activityKind;
        }

        public byte getType() {
            return type;
        }

        public static Optional<HuaweiActivityType> fromByte(final byte type) {
            for (final HuaweiActivityType value : HuaweiActivityType.values()) {
                if (value.getType() == type)
                    return Optional.of(value);
            }
            return Optional.empty();
        }
    }

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
                            dataErrorHex,
                            responseData.calories,
                            responseData.cyclingPower,
                            responseData.frequency,
                            responseData.altitude
                    );

                    dbHandler.getDaoSession().getHuaweiWorkoutDataSampleDao().insertOrReplace(dataSample);
                }
            }
        } catch (Exception e) {
            GB.toast("Exception parsing unknown workout data", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Exception parsing unknown workout data", e);
        }
    }

    public static ActivityKind huaweiTypeToGbType(byte huaweiType) {
        final Optional<HuaweiActivityType> type = HuaweiActivityType.fromByte(huaweiType);
        if (type.isPresent())
            return type.get().getActivityKind();
        else
            return ActivityKind.UNKNOWN;
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

            ActivityKind type = huaweiTypeToGbType(summary.getType());

            JSONObject jsonObject = new JSONObject();

            JSONObject calories = new JSONObject();
            calories.put("value", summary.getCalories());
            calories.put("unit", ActivitySummaryEntries.UNIT_KCAL);
            jsonObject.put(ActivitySummaryEntries.CALORIES_BURNT, calories);

            JSONObject distance = new JSONObject();
            distance.put("value", summary.getDistance());
            distance.put("unit", ActivitySummaryEntries.UNIT_METERS);
            jsonObject.put(ActivitySummaryEntries.DISTANCE_METERS, distance);

            JSONObject steps = new JSONObject();
            steps.put("value", summary.getStepCount());
            steps.put("unit", ActivitySummaryEntries.UNIT_STEPS);
            jsonObject.put(ActivitySummaryEntries.STEPS, steps);

            JSONObject time = new JSONObject();
            time.put("value", summary.getDuration());
            time.put("unit", ActivitySummaryEntries.UNIT_SECONDS);
            jsonObject.put(ActivitySummaryEntries.ACTIVE_SECONDS, time);

            JSONObject status = new JSONObject();
            status.put("value", summary.getStatus() & 0xFF);
            status.put("unit", "");
            jsonObject.put(ActivitySummaryEntries.STATUS, status);

            JSONObject typeJson = new JSONObject();
            typeJson.put("value", summary.getType() & 0xFF);
            typeJson.put("unit", "");
            jsonObject.put(ActivitySummaryEntries.TYPE, typeJson);

            if (summary.getStrokes() != -1) {
                JSONObject strokesJson = new JSONObject();
                strokesJson.put("value", summary.getStrokes());
                strokesJson.put("unit", ActivitySummaryEntries.UNIT_STROKES);
                jsonObject.put(ActivitySummaryEntries.STROKES, strokesJson);
            }

            if (summary.getAvgStrokeRate() != -1) {
                JSONObject avgStrokeRateJson = new JSONObject();
                avgStrokeRateJson.put("value", summary.getAvgStrokeRate());
                avgStrokeRateJson.put("unit", ""); // TODO: find out unit
                jsonObject.put(ActivitySummaryEntries.STROKE_RATE_AVG, avgStrokeRateJson);
            }

            if (summary.getPoolLength() != -1) {
                JSONObject poolLengthJson = new JSONObject();
                poolLengthJson.put("value", summary.getPoolLength());
                poolLengthJson.put("unit", ActivitySummaryEntries.UNIT_CM);
                jsonObject.put(ActivitySummaryEntries.LANE_LENGTH, poolLengthJson);
            }

            if (summary.getLaps() != -1) {
                JSONObject lapsJson = new JSONObject();
                lapsJson.put("value", summary.getLaps());
                lapsJson.put("unit", ActivitySummaryEntries.UNIT_LAPS);
                jsonObject.put(ActivitySummaryEntries.LAPS, lapsJson);
            }

            if (summary.getAvgSwolf() != -1) {
                JSONObject avgSwolfJson = new JSONObject();
                avgSwolfJson.put("value", summary.getAvgSwolf());
                avgSwolfJson.put("unit", "");
                jsonObject.put(ActivitySummaryEntries.SWOLF_AVG, avgSwolfJson);
            }

            boolean unknownData = false;
            if (!dataSamples.isEmpty()) {
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
                int sumCalories = 0;
                int minCyclingPower = Integer.MAX_VALUE;
                int maxCyclingPower = 0;
                int cyclingPower = 0;
                int cyclingPowerCount = 0;
                int avgAltitude = 0;
                int altitudeCount = 0;
                int minAltitude = Integer.MAX_VALUE;
                int maxAltitude = Integer.MIN_VALUE;
                Integer previousAlt = null;
                int sumAltitudeUp = 0;
                int sumAltitudeDown = 0;

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
                    if (dataSample.getCalories() != -1)
                        sumCalories += dataSample.getCalories();
                    if (dataSample.getCyclingPower() != -1) {
                        int cp = dataSample.getCyclingPower();
                        cyclingPower += cp;
                        cyclingPowerCount += 1;
                        if (cp > maxCyclingPower)
                            maxCyclingPower = cp;
                        if (cp < minCyclingPower)
                            minCyclingPower = cp;
                    }
                    if (dataSample.getAltitude() != null) {
                        int alt = dataSample.getAltitude();
                        avgAltitude += alt;
                        altitudeCount += 1;
                        if (alt > maxAltitude)
                            maxAltitude = alt;
                        if (alt < minAltitude)
                            minAltitude = alt;
                        if (previousAlt != null) {
                            if (alt > previousAlt)
                                sumAltitudeUp += alt - previousAlt;
                            else if (alt < previousAlt)
                                sumAltitudeDown += previousAlt - alt;
                        }
                        previousAlt = alt;
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
                if (cyclingPowerCount > 0)
                    cyclingPower = cyclingPower / cyclingPowerCount;
                if (altitudeCount > 0)
                    avgAltitude = avgAltitude / altitudeCount;

                if (speedCount > 0) {
                    JSONObject speedJson = new JSONObject();
                    speedJson.put("value", speed / 10);
                    speedJson.put("unit", ActivitySummaryEntries.UNIT_METERS_PER_SECOND);
                    jsonObject.put(ActivitySummaryEntries.SPEED_AVG, speedJson);
                }

                if (stepRatePresent) {
                    JSONObject stepRateSumJson = new JSONObject();
                    stepRateSumJson.put("value", stepRate);
                    stepRateSumJson.put("unit", ActivitySummaryEntries.UNIT_SPM);
                    jsonObject.put(ActivitySummaryEntries.STEP_RATE_SUM, stepRateSumJson);

                    JSONObject stepRateAvgJson = new JSONObject();
                    stepRateAvgJson.put("value", avgStepRate);
                    stepRateAvgJson.put("unit", ActivitySummaryEntries.UNIT_SPM);
                    jsonObject.put(ActivitySummaryEntries.STEP_RATE_AVG, stepRateAvgJson);
                }

                if (cadenceCount > 0) {
                    JSONObject cadenceJson = new JSONObject();
                    cadenceJson.put("value", cadence);
                    cadenceJson.put("unit", ActivitySummaryEntries.UNIT_SPM);
                    jsonObject.put(ActivitySummaryEntries.CADENCE_AVG, cadenceJson);
                }

                if (stepLengthCount > 0) {
                    JSONObject stepLengthJson = new JSONObject();
                    stepLengthJson.put("value", stepLength);
                    stepLengthJson.put("unit", ActivitySummaryEntries.UNIT_CM);
                    jsonObject.put(ActivitySummaryEntries.STEP_LENGTH_AVG, stepLengthJson);
                }

                if (groundContactTimeCount > 0) {
                    JSONObject groundContactTimeJson = new JSONObject();
                    groundContactTimeJson.put("value", groundContactTime);
                    groundContactTimeJson.put("unit", ActivitySummaryEntries.UNIT_MILLISECONDS);
                    jsonObject.put(ActivitySummaryEntries.GROUND_CONTACT_TIME_AVG, groundContactTimeJson);
                }

                if (impactCount > 0) {
                    JSONObject impactJson = new JSONObject();
                    impactJson.put("value", impact);
                    impactJson.put("unit", "g");
                    jsonObject.put(ActivitySummaryEntries.IMPACT_AVG, impactJson);

                    JSONObject maxImpactJson = new JSONObject();
                    maxImpactJson.put("value", maxImpact);
                    maxImpactJson.put("unit", "g");
                    jsonObject.put(ActivitySummaryEntries.IMPACT_MAX, maxImpactJson);
                }

                if (swingAngleCount > 0) {
                    JSONObject swingAngleJson = new JSONObject();
                    swingAngleJson.put("value", swingAngle);
                    swingAngleJson.put("unit", ActivitySummaryEntries.UNIT_DEGREES);
                    jsonObject.put(ActivitySummaryEntries.SWING_ANGLE_AVG, swingAngleJson);
                }

                if (footLandingPresent) {
                    JSONObject foreFootLandingJson = new JSONObject();
                    foreFootLandingJson.put("value", foreFootLanding);
                    foreFootLandingJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.FORE_FOOT_LANDINGS, foreFootLandingJson);

                    JSONObject midFootLandingJson = new JSONObject();
                    midFootLandingJson.put("value", midFootLanding);
                    midFootLandingJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.MID_FOOT_LANDINGS, midFootLandingJson);

                    JSONObject backFootLandingJson = new JSONObject();
                    backFootLandingJson.put("value", backFootLanding);
                    backFootLandingJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.BACK_FOOT_LANDINGS, backFootLandingJson);
                }

                if (eversionAngleCount > 0) {
                    JSONObject eversionAngleJson = new JSONObject();
                    eversionAngleJson.put("value", eversionAngle);
                    eversionAngleJson.put("unit", ActivitySummaryEntries.UNIT_DEGREES);
                    jsonObject.put(ActivitySummaryEntries.EVERSION_ANGLE_AVG, eversionAngleJson);

                    JSONObject maxEversionAngleJson = new JSONObject();
                    maxEversionAngleJson.put("value", maxEversionAngle);
                    maxEversionAngleJson.put("unit", ActivitySummaryEntries.UNIT_DEGREES);
                    jsonObject.put(ActivitySummaryEntries.EVERSION_ANGLE_MAX, maxEversionAngleJson);
                }

                if (swolfCount > 0) {
                    JSONObject swolfJson = new JSONObject();
                    swolfJson.put("value", swolf);
                    swolfJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.SWOLF_AVG, swolfJson);

                    JSONObject maxSwolfJson = new JSONObject();
                    maxSwolfJson.put("value", maxSwolf);
                    maxSwolfJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.SWOLF_MAX, maxSwolfJson);

                    JSONObject minSwolfJson = new JSONObject();
                    minSwolfJson.put("value", minSwolf);
                    minSwolfJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.SWOLF_MIN, minSwolfJson);
                }

                if (strokeRateCount > 0) {
                    JSONObject strokeRateJson = new JSONObject();
                    strokeRateJson.put("value", strokeRate);
                    strokeRateJson.put("unit", ""); // TODO: find out unit?
                    jsonObject.put(ActivitySummaryEntries.STROKE_RATE_AVG, strokeRateJson);

                    JSONObject maxStrokeRateJson = new JSONObject();
                    maxStrokeRateJson.put("value", maxStrokeRate);
                    maxStrokeRateJson.put("unit", ""); // TODO: find out unit?
                    jsonObject.put(ActivitySummaryEntries.STROKE_RATE_MAX, maxStrokeRateJson);
                }

                if (heartRateCount > 0) {
                    JSONObject heartRateJson = new JSONObject();
                    heartRateJson.put("value", heartRate);
                    heartRateJson.put("unit", ActivitySummaryEntries.UNIT_BPM);
                    jsonObject.put(ActivitySummaryEntries.HR_AVG, heartRateJson);

                    JSONObject maxHeartRateJson = new JSONObject();
                    maxHeartRateJson.put("value", maxHeartRate);
                    maxHeartRateJson.put("unit", ActivitySummaryEntries.UNIT_BPM);
                    jsonObject.put(ActivitySummaryEntries.HR_MAX, maxHeartRateJson);

                    JSONObject minHeartRateJson = new JSONObject();
                    minHeartRateJson.put("value", minHeartRate);
                    minHeartRateJson.put("unit", ActivitySummaryEntries.UNIT_BPM);
                    jsonObject.put(ActivitySummaryEntries.HR_MIN, minHeartRateJson);
                }

                if (sumCalories > 0) {
                    JSONObject caloriesSumJson = new JSONObject();
                    caloriesSumJson.put("value", sumCalories);
                    caloriesSumJson.put("unit", ActivitySummaryEntries.UNIT_KCAL);
                    jsonObject.put(ActivitySummaryEntries.CALORIES_BURNT, caloriesSumJson);
                }

                if (cyclingPowerCount > 0) {
                    JSONObject cyclingPowerJson = new JSONObject();
                    cyclingPowerJson.put("value", cyclingPower);
                    cyclingPowerJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.CYCLING_POWER_AVERAGE, cyclingPowerJson);

                    JSONObject minCyclingPowerJson = new JSONObject();
                    minCyclingPowerJson.put("value", minCyclingPower);
                    minCyclingPowerJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.CYCLING_POWER_MIN, minCyclingPowerJson);

                    JSONObject maxCyclingPowerJson = new JSONObject();
                    maxCyclingPowerJson.put("value", maxCyclingPower);
                    maxCyclingPowerJson.put("unit", "");
                    jsonObject.put(ActivitySummaryEntries.CYCLING_POWER_MAX, maxCyclingPowerJson);
                }

                if (altitudeCount > 0) {
                    JSONObject avgAltitudeJson = new JSONObject();
                    avgAltitudeJson.put("value", avgAltitude / 10.0);
                    avgAltitudeJson.put("unit", ActivitySummaryEntries.UNIT_METERS);
                    jsonObject.put(ActivitySummaryEntries.ALTITUDE_AVG, avgAltitudeJson);

                    JSONObject minAltitudeJson = new JSONObject();
                    minAltitudeJson.put("value", minAltitude / 10.0);
                    minAltitudeJson.put("unit", ActivitySummaryEntries.UNIT_METERS);
                    jsonObject.put(ActivitySummaryEntries.ALTITUDE_MIN, minAltitudeJson);

                    JSONObject maxAltitudeJson = new JSONObject();
                    maxAltitudeJson.put("value", maxAltitude / 10.0);
                    maxAltitudeJson.put("unit", ActivitySummaryEntries.UNIT_METERS);
                    jsonObject.put(ActivitySummaryEntries.ALTITUDE_MAX, maxAltitudeJson);

                    JSONObject sumUpAltitudeJson = new JSONObject();
                    sumUpAltitudeJson.put("value", sumAltitudeUp / 10.0);
                    sumUpAltitudeJson.put("unit", ActivitySummaryEntries.UNIT_METERS);
                    jsonObject.put(ActivitySummaryEntries.ELEVATION_GAIN, sumUpAltitudeJson);

                    JSONObject sumDownAltitudeJson = new JSONObject();
                    sumDownAltitudeJson.put("value", sumAltitudeDown / 10.0);
                    sumDownAltitudeJson.put("unit", ActivitySummaryEntries.UNIT_METERS);
                    jsonObject.put(ActivitySummaryEntries.ELEVATION_LOSS, sumDownAltitudeJson);
                }
            }

            try (CloseableListIterator<HuaweiWorkoutPaceSample> it = qbPace.build().listIterator()) {
                HashMap<Byte, Integer> typeCount = new HashMap<>();
                HashMap<Byte, Integer> typePace = new HashMap<>();

                while (it.hasNext()) {
                    int index = it.nextIndex();
                    HuaweiWorkoutPaceSample sample = it.next();

                    int count = 1;
                    int pace = sample.getPace();

                    Integer previousCount = typeCount.get(sample.getType());
                    Integer previousPace = typePace.get(sample.getType());
                    if (previousCount != null)
                        count += previousCount;
                    if (previousPace != null)
                        pace += previousPace;
                    typeCount.put(sample.getType(), count);
                    typePace.put(sample.getType(), pace);

                    JSONObject paceDistance = new JSONObject();
                    paceDistance.put("value", sample.getDistance());
                    paceDistance.put("unit", ActivitySummaryEntries.UNIT_KILOMETERS);
                    paceDistance.put("group", ActivitySummaryEntries.GROUP_PACE);
                    jsonObject.put(String.format(GBApplication.getLanguage(), GBApplication.getContext().getString(R.string.fmtPaceDistance), index), paceDistance);

                    JSONObject paceType = new JSONObject();
                    paceType.put("value", sample.getType());
                    paceType.put("unit", ""); // TODO: find out types
                    paceType.put("group", ActivitySummaryEntries.GROUP_PACE);
                    jsonObject.put(String.format(GBApplication.getLanguage(), GBApplication.getContext().getString(R.string.fmtPaceType), index), paceType);

                    JSONObject pacePace = new JSONObject();
                    pacePace.put("value", sample.getPace());
                    pacePace.put("unit", ActivitySummaryEntries.UNIT_SECONDS_PER_KM);
                    pacePace.put("group", ActivitySummaryEntries.GROUP_PACE);
                    jsonObject.put(String.format(GBApplication.getLanguage(), GBApplication.getContext().getString(R.string.fmtPacePace), index), pacePace);

                    if (sample.getCorrection() != 0) {
                        JSONObject paceCorrection = new JSONObject();
                        paceCorrection.put("value", sample.getCorrection() / 10);
                        paceCorrection.put("unit", ActivitySummaryEntries.UNIT_METERS);
                        paceCorrection.put("group", ActivitySummaryEntries.GROUP_PACE);
                        jsonObject.put(String.format(GBApplication.getLanguage(), GBApplication.getContext().getString(R.string.fmtPaceCorrection), index), paceCorrection);
                    }
                }

                for (Byte key : typeCount.keySet()) {
                    Integer count = typeCount.get(key);
                    Integer pace = typePace.get(key);
                    if (count == null || pace == null)
                        continue;
                    JSONObject avgPace = new JSONObject();
                    avgPace.put("value", pace / count);
                    avgPace.put("unit", ActivitySummaryEntries.UNIT_SECONDS_PER_KM);
                    avgPace.put("group", ActivitySummaryEntries.GROUP_PACE);
                    jsonObject.put(String.format(GBApplication.getContext().getString(R.string.fmtPaceTypeAverage), key), avgPace);
                }
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
                        type.getCode(),
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
                        type.getCode(),
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
