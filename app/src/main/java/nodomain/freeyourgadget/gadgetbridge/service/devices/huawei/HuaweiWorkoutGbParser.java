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

import android.content.Context;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.greenrobot.dao.query.CloseableListIterator;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryProgressEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryTableRowEntry;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryValue;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiSportHRZones;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Workout;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutPaceSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSwimSegmentsSample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSwimSegmentsSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * This class parses the Huawei workouts into the table GB uses to show the workouts
 * It also re-parses the unknown data from the workout tables
 * It is a separate class so it can easily be used to re-parse the data without database migrations
 */
public class HuaweiWorkoutGbParser implements ActivitySummaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiWorkoutGbParser.class);

    // TODO: Might be nicer to propagate the exceptions, so they can be handled upstream

    private final GBDevice gbDevice;
    private final Context context;

    public HuaweiWorkoutGbParser(final GBDevice gbDevice, final Context context) {
        this.gbDevice = gbDevice;
        this.context = context;
    }

    @Override
    public BaseActivitySummary parseBinaryData(final BaseActivitySummary summary, final boolean forDetails) {
        if (!forDetails) {
            // Our parsing is too slow, especially without a RecyclerView
            return summary;
        }

        // Find the existing HuaweiWorkoutSummarySample
        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            QueryBuilder<HuaweiWorkoutSummarySample> qb = session.getHuaweiWorkoutSummarySampleDao().queryBuilder();
            qb.where(HuaweiWorkoutSummarySampleDao.Properties.StartTimestamp.eq(summary.getStartTime().getTime() / 1000));
            qb.where(HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(device.getId()));
            qb.where(HuaweiWorkoutSummarySampleDao.Properties.UserId.eq(user.getId()));
            final List<HuaweiWorkoutSummarySample> huaweiSummaries = qb.build().list();
            if (huaweiSummaries.isEmpty()) {
                LOG.warn("Failed to find huawei summary for {}", summary.getStartTime());
                return summary;
            }
            updateBaseSummary(session, huaweiSummaries.get(0), summary);
        } catch (Exception e) {
            LOG.error("Failed to update summary");
        }

        return summary;
    }

    public enum HuaweiActivityType {
        // Based on nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport

        RUNNING(1, ActivityKind.RUNNING),
        WALKING(2, ActivityKind.WALKING),
        CYCLING(3, ActivityKind.CYCLING),
        MOUNTAIN_HIKE(4, ActivityKind.MOUNTAIN_HIKE),
        INDOOR_RUN(5, ActivityKind.INDOOR_RUNNING),
        POOL_SWIM(6, ActivityKind.POOL_SWIM),
        INDOOR_CYCLE(7, ActivityKind.INDOOR_CYCLING),
        OPEN_WATER_SWIM(8, ActivityKind.SWIMMING_OPENWATER),
        INDOOR_WALK(13, ActivityKind.INDOOR_WALKING),
        HIKING(14, ActivityKind.HIKING),
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
        OTHER(255, ActivityKind.EXERCISE),
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

    public void parseAllWorkouts() {
        parseUnknownWorkoutData();

        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            QueryBuilder<HuaweiWorkoutSummarySample> qb = session.getHuaweiWorkoutSummarySampleDao().queryBuilder();
            for (HuaweiWorkoutSummarySample summary : qb.listLazy()) {
                parseWorkout(session, summary.getWorkoutId(), summary.getDeviceId());
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

    private String getSwimStyle(byte swimType) {
        switch (swimType) {
            case 1:
                return "breaststroke";
            case 3:
                return "butterfly";
            case 4:
                return "backstroke";
            case 5:
                return "medley";
        }
        return "freestyle";
    }

    public void parseWorkout(Long workoutId) {
        LOG.debug("Parsing workout ID {}", workoutId);
        if (workoutId == null)
            return;

        try (DBHandler db = GBApplication.acquireDB()) {
            final DaoSession session = db.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            parseWorkout(session, workoutId, device.getId());
        } catch (Exception e) {
            GB.toast("Exception parsing workout data", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Exception parsing workout data", e);
        }
    }

    public void parseWorkout(final DaoSession session, final Long workoutId, final long deviceId) {
        if (workoutId == null)
            return;

        QueryBuilder<HuaweiWorkoutSummarySample> qbSummary = session.getHuaweiWorkoutSummarySampleDao().queryBuilder().where(
                HuaweiWorkoutSummarySampleDao.Properties.WorkoutId.eq(workoutId)
        );
        List<HuaweiWorkoutSummarySample> summarySamples = qbSummary.build().list();
        if (summarySamples.size() != 1)
            return;
        HuaweiWorkoutSummarySample summary = summarySamples.get(0);

        final BaseActivitySummary baseSummary = ActivitySummaryParser.findOrCreateBaseActivitySummary(
                session,
                deviceId,
                summary.getStartTimestamp()
        );

        updateBaseSummary(session, summary, baseSummary);

        session.getBaseActivitySummaryDao().insertOrReplace(baseSummary);
    }

    public void updateBaseSummary(final DaoSession session,
                                  final HuaweiWorkoutSummarySample summary,
                                  final BaseActivitySummary baseSummary) {
        try {
            QueryBuilder<HuaweiWorkoutDataSample> qbData = session.getHuaweiWorkoutDataSampleDao().queryBuilder().where(
                    HuaweiWorkoutDataSampleDao.Properties.WorkoutId.eq(summary.getWorkoutId())
            );
            List<HuaweiWorkoutDataSample> dataSamples = qbData.build().list();

            QueryBuilder<HuaweiWorkoutPaceSample> qbPace = session.getHuaweiWorkoutPaceSampleDao().queryBuilder().orderAsc(HuaweiWorkoutPaceSampleDao.Properties.PaceIndex).where(
                    HuaweiWorkoutPaceSampleDao.Properties.WorkoutId.eq(summary.getWorkoutId())
            );

            QueryBuilder<HuaweiWorkoutSwimSegmentsSample> qbSegments = session.getHuaweiWorkoutSwimSegmentsSampleDao().queryBuilder().orderAsc(HuaweiWorkoutSwimSegmentsSampleDao.Properties.SegmentIndex).where(
                    HuaweiWorkoutSwimSegmentsSampleDao.Properties.WorkoutId.eq(summary.getWorkoutId())
            );

            ActivityKind type = huaweiTypeToGbType(summary.getType());

            ActivitySummaryData summaryData = new ActivitySummaryData();

            summaryData.add(ActivitySummaryEntries.CALORIES_BURNT, summary.getCalories(), ActivitySummaryEntries.UNIT_KCAL);
            summaryData.add(ActivitySummaryEntries.DISTANCE_METERS, summary.getDistance(), ActivitySummaryEntries.UNIT_METERS);
            summaryData.add(ActivitySummaryEntries.STEPS, summary.getStepCount(), ActivitySummaryEntries.UNIT_STEPS);
            summaryData.add(ActivitySummaryEntries.ACTIVE_SECONDS, summary.getDuration(), ActivitySummaryEntries.UNIT_SECONDS);
            //summaryData.add(ActivitySummaryEntries.STATUS, summary.getStatus() & 0xFF, ActivitySummaryEntries.UNIT_NONE);
            summaryData.add(ActivitySummaryEntries.TYPE, summary.getType() & 0xFF, ActivitySummaryEntries.UNIT_NONE);

            if (summary.getStrokes() != -1) {
                summaryData.add(ActivitySummaryEntries.STROKES, summary.getStrokes(), ActivitySummaryEntries.UNIT_STROKES);
            }

            if (summary.getPoolLength() != -1) {
                summaryData.add(ActivitySummaryEntries.LANE_LENGTH, summary.getPoolLength(), ActivitySummaryEntries.UNIT_CM);
            }

            if (summary.getLaps() != -1) {
                summaryData.add(ActivitySummaryEntries.LAPS, summary.getLaps(), ActivitySummaryEntries.UNIT_LAPS);
            }

            if(summary.getWorkoutLoad() > 0) {
                summaryData.add(ActivitySummaryEntries.WORKOUT_LOAD, summary.getWorkoutLoad(), ActivitySummaryEntries.UNIT_NONE);
            }

            if(summary.getWorkoutAerobicEffect() > 0) {
                summaryData.add(ActivitySummaryEntries.TRAINING_EFFECT_AEROBIC, summary.getWorkoutAerobicEffect() / 10.0, ActivitySummaryEntries.UNIT_NONE);
            }

            if(summary.getWorkoutAnaerobicEffect() >= 0) {
                summaryData.add(ActivitySummaryEntries.TRAINING_EFFECT_ANAEROBIC, summary.getWorkoutAnaerobicEffect() / 10.0, ActivitySummaryEntries.UNIT_NONE);
            }

            if(summary.getRecoveryTime() > 0) {
                summaryData.add(ActivitySummaryEntries.RECOVERY_TIME, summary.getRecoveryTime() / 60.0, ActivitySummaryEntries.UNIT_HOURS);
            }

            if(summary.getSwimType() != -1) {
                summaryData.add(ActivitySummaryEntries.SWIM_STYLE, getSwimStyle(summary.getSwimType()));
            }

            if(summary.getMaxMET() > 0) {
                int value = (int) (((float) summary.getMaxMET() * 3.5f)) / 65536;
                summaryData.add(ActivitySummaryEntries.MAXIMUM_OXYGEN_UPTAKE, value, ActivitySummaryEntries.UNIT_ML_KG_MIN);
            }


            Integer summaryMinAltitude = summary.getMinAltitude();
            Integer summaryMaxAltitude = summary.getMaxAltitude();
            Integer elevationGain = summary.getElevationGain();
            Integer elevationLoss = summary.getElevationLoss();

            int minHeartRatePeak = summary.getMinHeartRatePeak() & 0xff;
            int maxHeartRatePeak = summary.getMaxHeartRatePeak() & 0xff;

            int avgStrokeRate = summary.getAvgStrokeRate();

            int avgSwolf = summary.getAvgSwolf();

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

                //NOTE: The method of retrieving HR zones from the Huawei watch is not discovered. It may not return zones.
                // So they are calculated based on config.
                HeartRateZonesConfig HRZonesCfg = null;
                Integer zoneType = HuaweiWorkoutUtils.getHRZoneTypeByActivity(type);
                int zoneCalculateMethod = summary.getHrZoneType();
                LOG.info("Workout HR Zone Calculate Type: {}", zoneCalculateMethod);
                if(zoneType != null && HeartRateZonesConfig.isCalculateMethodValidFroType(zoneType, zoneCalculateMethod)) {
                    ActivityUser activityUser = new ActivityUser();
                    HuaweiSportHRZones hrSportZones = new HuaweiSportHRZones(activityUser.getAge());
                    HRZonesCfg = hrSportZones.getHRZonesConfigByType(zoneType);
                }

                int dataDelta = 5;
                if (dataSamples.size() >= 2 && dataSamples.get(1).getTimestamp() - dataSamples.get(0).getTimestamp() >= 40) {
                    dataDelta = 60;
                }

                int[] HRZones = new int[5];

                int dataIdx = 0;
                for (HuaweiWorkoutDataSample dataSample : dataSamples) {

                    if(HRZonesCfg != null) {
                        int zoneIdx = HRZonesCfg.getZoneByMethod(dataSample.getHeartRate() & 0xFF, zoneCalculateMethod);
                        if (zoneIdx != -1 && dataIdx < (dataSamples.size() - 1)) {
                                 HRZones[zoneIdx] += dataDelta;
                        }
                        dataIdx++;
                    }

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


                if(HRZonesCfg != null) {
                    final double totalTime = Arrays.stream(HRZones).sum();
                    final List<String> zoneOrder = Arrays.asList(ActivitySummaryEntries.HR_ZONE_WARM_UP, ActivitySummaryEntries.HR_ZONE_FAT_BURN, ActivitySummaryEntries.HR_ZONE_AEROBIC, ActivitySummaryEntries.HR_ZONE_ANAEROBIC, ActivitySummaryEntries.HR_ZONE_EXTREME);
                    final int[] zoneColors = new int[]{
                            context.getResources().getColor(R.color.hr_zone_warm_up_color),
                            context.getResources().getColor(R.color.hr_zone_easy_color),
                            context.getResources().getColor(R.color.hr_zone_aerobic_color),
                            context.getResources().getColor(R.color.hr_zone_threshold_color),
                            context.getResources().getColor(R.color.hr_zone_maximum_color),
                    };
                    for (int i = zoneOrder.size() - 1; i >= 0; i--) {
                        double timeInZone = HRZones[i];
                        LOG.info("Zone: {} {}", zoneOrder.get(i), timeInZone);
                        summaryData.add(
                                zoneOrder.get(i),
                                new ActivitySummaryProgressEntry(
                                        timeInZone,
                                        ActivitySummaryEntries.UNIT_SECONDS,
                                        (int) (100 * timeInZone / totalTime),
                                        zoneColors[i]
                                )
                        );
                    }
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
                    summaryData.add(ActivitySummaryEntries.SPEED_AVG, speed / 10f, ActivitySummaryEntries.UNIT_METERS_PER_SECOND);
                }

                if (stepRatePresent) {
                    summaryData.add(ActivitySummaryEntries.STEP_RATE_AVG, avgStepRate, ActivitySummaryEntries.UNIT_SPM);
                }

                if (cadenceCount > 0) {
                    summaryData.add(ActivitySummaryEntries.CADENCE_AVG, cadence, ActivitySummaryEntries.UNIT_SPM);
                }

                if (stepLengthCount > 0) {
                    summaryData.add(ActivitySummaryEntries.STEP_LENGTH_AVG, stepLength, ActivitySummaryEntries.UNIT_CM);
                }

                if (groundContactTimeCount > 0) {
                    summaryData.add(ActivitySummaryEntries.GROUND_CONTACT_TIME_AVG, groundContactTime, ActivitySummaryEntries.UNIT_MILLISECONDS);
                }

                if (impactCount > 0) {
                    summaryData.add(ActivitySummaryEntries.IMPACT_AVG, impact, "g");
                    summaryData.add(ActivitySummaryEntries.IMPACT_MAX, maxImpact, "g");
                }

                if (swingAngleCount > 0) {
                    summaryData.add(ActivitySummaryEntries.SWING_ANGLE_AVG, swingAngle, ActivitySummaryEntries.UNIT_DEGREES);
                }

                if (footLandingPresent) {
                    summaryData.add(ActivitySummaryEntries.FORE_FOOT_LANDINGS, foreFootLanding, ActivitySummaryEntries.UNIT_NONE);
                    summaryData.add(ActivitySummaryEntries.MID_FOOT_LANDINGS, midFootLanding, ActivitySummaryEntries.UNIT_NONE);
                    summaryData.add(ActivitySummaryEntries.BACK_FOOT_LANDINGS, backFootLanding, ActivitySummaryEntries.UNIT_NONE);
                }

                if (eversionAngleCount > 0) {
                    summaryData.add(ActivitySummaryEntries.EVERSION_ANGLE_AVG, eversionAngle, ActivitySummaryEntries.UNIT_DEGREES);
                    summaryData.add(ActivitySummaryEntries.EVERSION_ANGLE_MAX, maxEversionAngle, ActivitySummaryEntries.UNIT_DEGREES);
                }

                if (swolfCount > 0) {
                    if(avgSwolf == -1) {
                        avgSwolf = swolf;
                    }
                    summaryData.add(ActivitySummaryEntries.SWOLF_MAX, maxSwolf, ActivitySummaryEntries.UNIT_NONE);
                    summaryData.add(ActivitySummaryEntries.SWOLF_MIN, minSwolf, ActivitySummaryEntries.UNIT_NONE);
                }

                if (strokeRateCount > 0) {
                    if(avgStrokeRate == -1) {
                        avgStrokeRate = strokeRate;
                    }
                    summaryData.add(ActivitySummaryEntries.STROKE_RATE_MAX, maxStrokeRate, ActivitySummaryEntries.UNIT_STROKES_PER_MINUTE);
                }

                if (heartRateCount > 0) {
                    summaryData.add(ActivitySummaryEntries.HR_AVG, heartRate, ActivitySummaryEntries.UNIT_BPM);
                    if(minHeartRatePeak == 0) {
                        minHeartRatePeak = minHeartRate;
                    }

                    if(maxHeartRatePeak == 0) {
                        maxHeartRatePeak = maxHeartRate;
                    }
                }

                if (sumCalories > 0) {
                    summaryData.add(ActivitySummaryEntries.CALORIES_BURNT, sumCalories, ActivitySummaryEntries.UNIT_KCAL);
                }

                if (cyclingPowerCount > 0) {
                    summaryData.add(ActivitySummaryEntries.CYCLING_POWER_AVERAGE, cyclingPower, ActivitySummaryEntries.UNIT_NONE);
                    summaryData.add(ActivitySummaryEntries.CYCLING_POWER_MIN, minCyclingPower, ActivitySummaryEntries.UNIT_NONE);
                    summaryData.add(ActivitySummaryEntries.CYCLING_POWER_MAX, maxCyclingPower, ActivitySummaryEntries.UNIT_NONE);
                }

                if (altitudeCount > 0) {
                    summaryData.add(ActivitySummaryEntries.ALTITUDE_AVG, avgAltitude / 10.0f, ActivitySummaryEntries.UNIT_METERS);

                    if(summaryMinAltitude == null) {
                        summaryMinAltitude = minAltitude;
                    }

                    if(summaryMaxAltitude == null) {
                        summaryMaxAltitude = maxAltitude;
                    }

                    if(elevationGain == null) {
                        elevationGain = sumAltitudeUp;
                    }

                    if(elevationLoss == null) {
                        elevationLoss = sumAltitudeDown;
                    }
                }
            }

            if(avgSwolf > 0) {
                summaryData.add(ActivitySummaryEntries.SWOLF_AVG, avgSwolf, ActivitySummaryEntries.UNIT_NONE);
            }

            if (avgStrokeRate > 0) {
                summaryData.add(ActivitySummaryEntries.STROKE_RATE_AVG, avgStrokeRate, ActivitySummaryEntries.UNIT_STROKES_PER_MINUTE);
            }

            if (minHeartRatePeak > 0) {
                summaryData.add(ActivitySummaryEntries.HR_MIN, minHeartRatePeak, ActivitySummaryEntries.UNIT_BPM);
            }
            if (maxHeartRatePeak > 0) {
                summaryData.add(ActivitySummaryEntries.HR_MAX, maxHeartRatePeak, ActivitySummaryEntries.UNIT_BPM);
            }

            if(summaryMinAltitude != null) {
                summaryData.add(ActivitySummaryEntries.ALTITUDE_MIN, summaryMinAltitude / 10.0f, ActivitySummaryEntries.UNIT_METERS);
            }

            if(summaryMaxAltitude != null) {
                summaryData.add(ActivitySummaryEntries.ALTITUDE_MAX, summaryMaxAltitude / 10.0f, ActivitySummaryEntries.UNIT_METERS);
            }
            if(elevationGain != null) {
                summaryData.add(ActivitySummaryEntries.ELEVATION_GAIN, elevationGain / 10.0f, ActivitySummaryEntries.UNIT_METERS);
            }
            if(elevationLoss != null) {
                summaryData.add(ActivitySummaryEntries.ELEVATION_LOSS, elevationLoss / 10.0f, ActivitySummaryEntries.UNIT_METERS);
            }


            final LinkedHashMap<String, ActivitySummaryTableRowEntry> pacesTable = new LinkedHashMap<>();

            pacesTable.put("paces_table",
                    new ActivitySummaryTableRowEntry(
                            ActivitySummaryEntries.GROUP_PACE,
                            Arrays.asList(
                                    new ActivitySummaryValue("#", ActivitySummaryEntries.UNIT_RAW_STRING),
                                    new ActivitySummaryValue("distance"),
                                    new ActivitySummaryValue("Pace")
                            ),
                            true,
                            true
                    )
            );

            String measurementSystem = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric");

            byte unitType = (byte) (measurementSystem.equals("metric") ? 0 : 1);
            try (CloseableListIterator<HuaweiWorkoutPaceSample> it = qbPace.build().listIterator()) {

                int paceCount = 0;
                int paceSum = 0;
                int paceFastest = Integer.MAX_VALUE;
                int paceSlowest = 0;

                int currentIndex = 1;
                while (it.hasNext()) {
                    int index = it.nextIndex();
                    HuaweiWorkoutPaceSample sample = it.next();

                    if (sample.getType() != unitType)
                        continue;

                    int pace = sample.getPace();

                    paceCount++;
                    paceSum += pace;

                    if (pace < paceFastest)
                        paceFastest = pace;

                    if (pace > paceSlowest)
                        paceSlowest = pace;

                    double distance = sample.getDistance();

                    if (sample.getCorrection() != null) {
                        distance += sample.getCorrection() / 10000d;
                    }

                    final List<ActivitySummaryValue> columns = new LinkedList<>();
                    // TODO: add proper units for type == 1. MILES and SECONDS PER MILE
                    columns.add(new ActivitySummaryValue(currentIndex++, ActivitySummaryEntries.UNIT_NONE));
                    columns.add(new ActivitySummaryValue(distance, ActivitySummaryEntries.UNIT_KILOMETERS));
                    columns.add(new ActivitySummaryValue(sample.getPace(), ActivitySummaryEntries.UNIT_SECONDS_PER_KM));

                    pacesTable.put("paces_table_" + index,
                            new ActivitySummaryTableRowEntry(
                                    ActivitySummaryEntries.GROUP_PACE,
                                    columns,
                                    false,
                                    true
                            )
                    );
                }

                if (paceCount != 0 && paceSum != 0) {
                    summaryData.add(
                            ActivitySummaryEntries.GROUP_PACE,
                            GBApplication.getContext().getString(R.string.fmtPaceAverage),
                            paceSum / (float) paceCount,
                            ActivitySummaryEntries.UNIT_SECONDS_PER_KM
                    );
                }

                if (paceFastest != Integer.MAX_VALUE) {
                    summaryData.add(
                            ActivitySummaryEntries.GROUP_PACE,
                            GBApplication.getContext().getString(R.string.maxPace),
                            paceFastest,
                            ActivitySummaryEntries.UNIT_SECONDS_PER_KM
                    );
                }

                if (paceSlowest != 0) {
                    summaryData.add(
                            ActivitySummaryEntries.GROUP_PACE,
                            GBApplication.getContext().getString(R.string.minPace),
                            paceSlowest,
                            ActivitySummaryEntries.UNIT_SECONDS_PER_KM
                    );
                }

                if (pacesTable.size() > 1) {
                    for (final Map.Entry<String, ActivitySummaryTableRowEntry> e : pacesTable.entrySet()) {
                        summaryData.add(e.getKey(), e.getValue());
                    }
                }
            }

            final LinkedHashMap<String, ActivitySummaryTableRowEntry> segmentsTable = new LinkedHashMap<>();

            try (CloseableListIterator<HuaweiWorkoutSwimSegmentsSample> it = qbSegments.build().listIterator()) {

                int currentIndex = 1;
                int tableIndex = 1;
                while (it.hasNext()) {
                    HuaweiWorkoutSwimSegmentsSample sample = it.next();

                    if (sample.getType() != unitType)
                        continue;

                    final List<ActivitySummaryValue> columns = new LinkedList<>();
                    // TODO: add proper units for type == 1. MILES
                    columns.add(new ActivitySummaryValue(currentIndex++, ActivitySummaryEntries.UNIT_NONE));
                    columns.add(new ActivitySummaryValue(getSwimStyle(sample.getSwimType()), ActivitySummaryEntries.UNIT_NONE));
                    columns.add(new ActivitySummaryValue(sample.getDistance(), ActivitySummaryEntries.UNIT_METERS));
                    columns.add(new ActivitySummaryValue(sample.getTime(), ActivitySummaryEntries.UNIT_SECONDS));


                    segmentsTable.put("segments_table_" + tableIndex++,
                            new ActivitySummaryTableRowEntry(
                                    ActivitySummaryEntries.GROUP_PACE,
                                    columns,
                                    true,
                                    true
                            )
                    );

                    final List<ActivitySummaryValue> columns2 = new LinkedList<>();
                    // TODO: add proper units for type == 1. MILES and SECONDS PER MILE
                    columns2.add(new ActivitySummaryValue("", ActivitySummaryEntries.UNIT_NONE));
                    columns2.add(new ActivitySummaryValue(sample.getStrokes(), ActivitySummaryEntries.UNIT_STROKES));
                    columns2.add(new ActivitySummaryValue(sample.getAvgSwolf(), ActivitySummaryEntries.UNIT_NONE));
                    columns2.add(new ActivitySummaryValue(sample.getPace(), ActivitySummaryEntries.UNIT_NONE)); //TODO: seconds / 100 meters



                    segmentsTable.put("segments_table_" + tableIndex++,
                            new ActivitySummaryTableRowEntry(
                                    ActivitySummaryEntries.GROUP_PACE,
                                    columns2,
                                    false,
                                    false
                            )
                    );
                    segmentsTable.put("segments_table_" + tableIndex++,
                            new ActivitySummaryTableRowEntry(
                                    ActivitySummaryEntries.GROUP_PACE,
                                    new ArrayList<>(),
                                    false,
                                    false
                            )
                    );
                }

                if (!segmentsTable.isEmpty()) {
                    for (final Map.Entry<String, ActivitySummaryTableRowEntry> e : segmentsTable.entrySet()) {
                        summaryData.add(e.getKey(), e.getValue());
                    }
                }
            }

            if (unknownData) {
                summaryData.add(
                        GBApplication.getContext().getString(R.string.unknownDataEncountered),
                        GBApplication.getContext().getString(R.string.yes).toUpperCase()
                );
            }

            if (baseSummary.getName() == null) {
                baseSummary.setName("Workout " + summary.getWorkoutNumber());
            }

            if (baseSummary.getGpxTrack() == null) {
                baseSummary.setGpxTrack(summary.getGpxFileLocation());
            }

            // start time never changes
            baseSummary.setEndTime(new Date(summary.getEndTimestamp() * 1000L));
            baseSummary.setActivityKind(type.getCode());
            baseSummary.setSummaryData(summaryData.toString());
        } catch (Exception e) {
            GB.toast("Exception parsing workout data", Toast.LENGTH_SHORT, GB.ERROR, e);
            LOG.error("Exception parsing workout data", e);
        }
    }
}
