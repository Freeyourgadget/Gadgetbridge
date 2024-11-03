/*
 * Copyright (C) 2011 Markus Junginger, greenrobot (http://greenrobot.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nodomain.freeyourgadget.gadgetbridge.daogen;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

/**
 * Generates entities and DAOs for the example project DaoExample.
 * Automatically run during build.
 */
public class GBDaoGenerator {

    private static final String VALID_FROM_UTC = "validFromUTC";
    private static final String VALID_TO_UTC = "validToUTC";
    private static final String MAIN_PACKAGE = "nodomain.freeyourgadget.gadgetbridge";
    private static final String MODEL_PACKAGE = MAIN_PACKAGE + ".model";
    private static final String VALID_BY_DATE = MODEL_PACKAGE + ".ValidByDate";
    private static final String ACTIVITY_SUMMARY = MODEL_PACKAGE + ".ActivitySummary";
    private static final String OVERRIDE = "@Override";
    private static final String SAMPLE_RAW_INTENSITY = "rawIntensity";
    private static final String SAMPLE_STEPS = "steps";
    private static final String SAMPLE_RAW_KIND = "rawKind";
    private static final String SAMPLE_HEART_RATE = "heartRate";
    private static final String SAMPLE_HRV_WEEKLY_AVERAGE = "weeklyAverage";
    private static final String SAMPLE_HRV_LAST_NIGHT_AVERAGE = "lastNightAverage";
    private static final String SAMPLE_HRV_LAST_NIGHT_5MIN_HIGH = "lastNight5MinHigh";
    private static final String SAMPLE_HRV_BASELINE_LOW_UPPER = "baselineLowUpper";
    private static final String SAMPLE_HRV_BASELINE_BALANCED_LOWER = "baselineBalancedLower";
    private static final String SAMPLE_HRV_BASELINE_BALANCED_UPPER = "baselineBalancedUpper";
    private static final String SAMPLE_HRV_STATUS_NUM = "statusNum";
    private static final String SAMPLE_HRV_VALUE = "value";
    private static final String SAMPLE_TEMPERATURE = "temperature";
    private static final String SAMPLE_TEMPERATURE_TYPE = "temperatureType";
    private static final String SAMPLE_WEIGHT_KG = "weightKg";
    private static final String TIMESTAMP_FROM = "timestampFrom";
    private static final String TIMESTAMP_TO = "timestampTo";


    public static void main(String[] args) throws Exception {
        final Schema schema = new Schema(86, MAIN_PACKAGE + ".entities");

        Entity userAttributes = addUserAttributes(schema);
        Entity user = addUserInfo(schema, userAttributes);

        Entity deviceAttributes = addDeviceAttributes(schema);
        Entity device = addDevice(schema, deviceAttributes);

        // yeah deep shit, has to be here (after device) for db upgrade and column order
        // because addDevice adds a property to deviceAttributes also....
        deviceAttributes.addStringProperty("volatileIdentifier");

        Entity tag = addTag(schema);
        Entity userDefinedActivityOverlay = addActivityDescription(schema, tag, user);

        addMakibesHR3ActivitySample(schema, user, device);
        addMiBandActivitySample(schema, user, device);
        addHuamiExtendedActivitySample(schema, user, device);
        addHuamiStressSample(schema, user, device);
        addHuamiSpo2Sample(schema, user, device);
        addHuamiHeartRateManualSample(schema, user, device);
        addHuamiHeartRateMaxSample(schema, user, device);
        addHuamiHeartRateRestingSample(schema, user, device);
        addHuamiPaiSample(schema, user, device);
        addHuamiSleepRespiratoryRateSample(schema, user, device);
        addXiaomiActivitySample(schema, user, device);
        addXiaomiSleepTimeSamples(schema, user, device);
        addXiaomiSleepStageSamples(schema, user, device);
        addXiaomiManualSamples(schema, user, device);
        addXiaomiDailySummarySamples(schema, user, device);
        addCmfActivitySample(schema, user, device);
        addCmfStressSample(schema, user, device);
        addCmfSpo2Sample(schema, user, device);
        addCmfSleepSessionSample(schema, user, device);
        addCmfSleepStageSample(schema, user, device);
        addCmfHeartRateSample(schema, user, device);
        addCmfWorkoutGpsSample(schema, user, device);
        addPebbleHealthActivitySample(schema, user, device);
        addPebbleHealthActivityKindOverlay(schema, user, device);
        addPebbleMisfitActivitySample(schema, user, device);
        addPebbleMorpheuzActivitySample(schema, user, device);
        addHPlusHealthActivityKindOverlay(schema, user, device);
        addHPlusHealthActivitySample(schema, user, device);
        addNo1F1ActivitySample(schema, user, device);
        addXWatchActivitySample(schema, user, device);
        addZeTimeActivitySample(schema, user, device);
        addID115ActivitySample(schema, user, device);
        addJYouActivitySample(schema, user, device);
        addWatchXPlusHealthActivitySample(schema, user, device);
        addWatchXPlusHealthActivityKindOverlay(schema, user, device);
        addTLW64ActivitySample(schema, user, device);
        addLefunActivitySample(schema, user, device);
        addLefunBiometricSample(schema, user, device);
        addLefunSleepSample(schema, user, device);
        addSonySWR12Sample(schema, user, device);
        addBangleJSActivitySample(schema, user, device);
        addCasioGBX100Sample(schema, user, device);
        addFitProActivitySample(schema, user, device);
        addPineTimeActivitySample(schema, user, device);
        addWithingsSteelHRActivitySample(schema, user, device);
        addHybridHRActivitySample(schema, user, device);
        addVivomoveHrActivitySample(schema, user, device);
        addGarminFitFile(schema, user, device);
        addGarminActivitySample(schema, user, device);
        addGarminStressSample(schema, user, device);
        addGarminBodyEnergySample(schema, user, device);
        addGarminSpo2Sample(schema, user, device);
        addGarminSleepStageSample(schema, user, device);
        addGarminEventSample(schema, user, device);
        addGarminHrvSummarySample(schema, user, device);
        addGarminHrvValueSample(schema, user, device);
        addGarminRespiratoryRateSample(schema, user, device);
        addGarminHeartRateRestingSample(schema, user, device);
        addPendingFile(schema, user, device);
        addWena3EnergySample(schema, user, device);
        addWena3BehaviorSample(schema, user, device);
        addWena3CaloriesSample(schema, user, device);
        addWena3ActivitySample(schema, user, device);
        addWena3HeartRateSample(schema, user, device);
        addWena3Vo2Sample(schema, user, device);
        addWena3StressSample(schema, user, device);
        addFemometerVinca2TemperatureSample(schema, user, device);
        addMiScaleWeightSample(schema, user, device);
        addColmiActivitySample(schema, user, device);
        addColmiHeartRateSample(schema, user, device);
        addColmiSpo2Sample(schema, user, device);
        addColmiStressSample(schema, user, device);
        addColmiSleepSessionSample(schema, user, device);
        addColmiSleepStageSample(schema, user, device);
        addColmiHrvValueSample(schema, user, device);
        addColmiHrvSummarySample(schema, user, device);

        addHuaweiActivitySample(schema, user, device);

        Entity huaweiWorkoutSummary = addHuaweiWorkoutSummarySample(schema, user, device);
        addHuaweiWorkoutDataSample(schema, huaweiWorkoutSummary);
        addHuaweiWorkoutPaceSample(schema, huaweiWorkoutSummary);
        addHuaweiWorkoutSwimSegmentsSample(schema, huaweiWorkoutSummary);

        Entity huaweiDictData = addHuaweiDictData(schema, user, device);
        addHuaweiDictDataValues(schema, huaweiDictData);

        addCalendarSyncState(schema, device);
        addAlarms(schema, user, device);
        addReminders(schema, user, device);
        addWorldClocks(schema, user, device);
        addContacts(schema, user, device);
        addAppSpecificNotificationSettings(schema, device);
        addCyclingSample(schema, user, device);

        Entity notificationFilter = addNotificationFilters(schema);

        addNotificationFilterEntry(schema, notificationFilter);

        addActivitySummary(schema, user, device);
        addBatteryLevel(schema, device);
        new DaoGenerator().generateAll(schema, "app/src/main/java");
    }

    private static Entity addTag(Schema schema) {
        Entity tag = addEntity(schema, "Tag");
        tag.addIdProperty();
        tag.addStringProperty("name").notNull();
        tag.addStringProperty("description").javaDocGetterAndSetter("An optional description of this tag.");
        tag.addLongProperty("userId").notNull();

        return tag;
    }

    private static Entity addActivityDescription(Schema schema, Entity tag, Entity user) {
        Entity activityDesc = addEntity(schema, "ActivityDescription");
        activityDesc.setJavaDoc("A user may further specify his activity with a detailed description and the help of tags.\nOne or more tags can be added to a given activity range.");
        activityDesc.addIdProperty();
        activityDesc.addIntProperty(TIMESTAMP_FROM).notNull();
        activityDesc.addIntProperty(TIMESTAMP_TO).notNull();
        activityDesc.addStringProperty("details").javaDocGetterAndSetter("An optional detailed description, specific to this very activity occurrence.");

        Property userId = activityDesc.addLongProperty("userId").notNull().getProperty();
        activityDesc.addToOne(user, userId);

        Entity activityDescTagLink = addEntity(schema, "ActivityDescTagLink");
        activityDescTagLink.addIdProperty();
        Property sourceId = activityDescTagLink.addLongProperty("activityDescriptionId").notNull().getProperty();
        Property targetId = activityDescTagLink.addLongProperty("tagId").notNull().getProperty();

        activityDesc.addToMany(tag, activityDescTagLink, sourceId, targetId);

        return activityDesc;
    }

    private static Entity addUserInfo(Schema schema, Entity userAttributes) {
        Entity user = addEntity(schema, "User");
        user.addIdProperty();
        user.addStringProperty("name").notNull();
        user.addDateProperty("birthday").notNull();
        user.addIntProperty("gender").notNull();
        Property userId = userAttributes.addLongProperty("userId").notNull().getProperty();

        // sorted by the from-date, newest first
        Property userAttributesSortProperty = getPropertyByName(userAttributes, VALID_FROM_UTC);
        user.addToMany(userAttributes, userId).orderDesc(userAttributesSortProperty);

        return user;
    }

    private static Property getPropertyByName(Entity entity, String propertyName) {
        for (Property prop : entity.getProperties()) {
            if (propertyName.equals(prop.getPropertyName())) {
                return prop;
            }
        }
        throw new IllegalStateException("Could not find property " + propertyName + " in entity " + entity.getClassName());
    }

    private static Entity addUserAttributes(Schema schema) {
        // additional properties of a user, which may change during the lifetime of a user
        // this allows changing attributes while preserving user identity
        Entity userAttributes = addEntity(schema, "UserAttributes");
        userAttributes.addIdProperty();
        userAttributes.addIntProperty("heightCM").notNull();
        userAttributes.addIntProperty("weightKG").notNull();
        userAttributes.addIntProperty("sleepGoalHPD").javaDocGetterAndSetter("Desired number of hours of sleep per day.");
        userAttributes.addIntProperty("stepsGoalSPD").javaDocGetterAndSetter("Desired number of steps per day.");
        addDateValidityTo(userAttributes);

        return userAttributes;
    }

    private static void addDateValidityTo(Entity entity) {
        entity.addDateProperty(VALID_FROM_UTC).codeBeforeGetter(OVERRIDE);
        entity.addDateProperty(VALID_TO_UTC).codeBeforeGetter(OVERRIDE);

        entity.implementsInterface(VALID_BY_DATE);
    }

    private static Entity addDevice(Schema schema, Entity deviceAttributes) {
        Entity device = addEntity(schema, "Device");
        device.addIdProperty();
        device.addStringProperty("name").notNull();
        device.addStringProperty("manufacturer").notNull();
        device.addStringProperty("identifier").notNull().unique().javaDocGetterAndSetter("The fixed identifier, i.e. MAC address of the device.");
        device.addIntProperty("type").notNull().javaDocGetterAndSetter("The DeviceType key, i.e. the GBDevice's type.").codeBeforeGetterAndSetter("@Deprecated");
        device.addStringProperty("typeName").notNull().javaDocGetterAndSetter("The DeviceType enum name, for example SONY_WH_1000XM3");
        device.addStringProperty("model").javaDocGetterAndSetter("An optional model, further specifying the kind of device.");
        device.addStringProperty("alias");
        device.addStringProperty("parentFolder").javaDocGetterAndSetter("Folder name containing this device.");
        Property deviceId = deviceAttributes.addLongProperty("deviceId").notNull().getProperty();
        // sorted by the from-date, newest first
        Property deviceAttributesSortProperty = getPropertyByName(deviceAttributes, VALID_FROM_UTC);
        device.addToMany(deviceAttributes, deviceId).orderDesc(deviceAttributesSortProperty);

        return device;
    }

    private static Entity addDeviceAttributes(Schema schema) {
        Entity deviceAttributes = addEntity(schema, "DeviceAttributes");
        deviceAttributes.addIdProperty();
        deviceAttributes.addStringProperty("firmwareVersion1").notNull();
        deviceAttributes.addStringProperty("firmwareVersion2");
        addDateValidityTo(deviceAttributes);

        return deviceAttributes;
    }

    private static Entity addMakibesHR3ActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "MakibesHR3ActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addMiBandActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "MiBandActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addHuamiExtendedActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "HuamiExtendedActivitySample");
        addCommonActivitySampleProperties("MiBandActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("unknown1");
        activitySample.addIntProperty("sleep");
        activitySample.addIntProperty("deepSleep");
        activitySample.addIntProperty("remSleep");
        return activitySample;
    }

    private static Entity addHuamiStressSample(Schema schema, Entity user, Entity device) {
        Entity stressSample = addEntity(schema, "HuamiStressSample");
        addCommonTimeSampleProperties("AbstractStressSample", stressSample, user, device);
        stressSample.addIntProperty("typeNum").notNull().codeBeforeGetter(OVERRIDE);
        stressSample.addIntProperty("stress").notNull().codeBeforeGetter(OVERRIDE);
        return stressSample;
    }

    private static Entity addHuamiSpo2Sample(Schema schema, Entity user, Entity device) {
        Entity spo2sample = addEntity(schema, "HuamiSpo2Sample");
        addCommonTimeSampleProperties("AbstractSpo2Sample", spo2sample, user, device);
        spo2sample.addIntProperty("typeNum").notNull().codeBeforeGetter(OVERRIDE);
        spo2sample.addIntProperty("spo2").notNull().codeBeforeGetter(OVERRIDE);
        return spo2sample;
    }

    private static Entity addHuamiHeartRateManualSample(Schema schema, Entity user, Entity device) {
        Entity hrManualSample = addEntity(schema, "HuamiHeartRateManualSample");
        addCommonTimeSampleProperties("AbstractHeartRateSample", hrManualSample, user, device);
        hrManualSample.addIntProperty("utcOffset").notNull();
        hrManualSample.addIntProperty(SAMPLE_HEART_RATE).notNull().codeBeforeGetter(OVERRIDE);
        return hrManualSample;
    }

    private static Entity addHuamiHeartRateMaxSample(Schema schema, Entity user, Entity device) {
        Entity hrMaxSample = addEntity(schema, "HuamiHeartRateMaxSample");
        addCommonTimeSampleProperties("AbstractHeartRateSample", hrMaxSample, user, device);
        hrMaxSample.addIntProperty("utcOffset").notNull();
        hrMaxSample.addIntProperty(SAMPLE_HEART_RATE).notNull().codeBeforeGetter(OVERRIDE);
        return hrMaxSample;
    }

    private static Entity addHuamiHeartRateRestingSample(Schema schema, Entity user, Entity device) {
        Entity hrRestingSample = addEntity(schema, "HuamiHeartRateRestingSample");
        addCommonTimeSampleProperties("AbstractHeartRateSample", hrRestingSample, user, device);
        hrRestingSample.addIntProperty("utcOffset").notNull();
        hrRestingSample.addIntProperty(SAMPLE_HEART_RATE).notNull().codeBeforeGetter(OVERRIDE);
        return hrRestingSample;
    }

    private static Entity addHuamiPaiSample(Schema schema, Entity user, Entity device) {
        Entity paiSample = addEntity(schema, "HuamiPaiSample");
        addCommonTimeSampleProperties("AbstractPaiSample", paiSample, user, device);
        paiSample.addIntProperty("utcOffset").notNull();
        paiSample.addFloatProperty("paiLow").notNull().codeBeforeGetter(OVERRIDE);
        paiSample.addFloatProperty("paiModerate").notNull().codeBeforeGetter(OVERRIDE);
        paiSample.addFloatProperty("paiHigh").notNull().codeBeforeGetter(OVERRIDE);
        paiSample.addIntProperty("timeLow").notNull().codeBeforeGetter(OVERRIDE);
        paiSample.addIntProperty("timeModerate").notNull().codeBeforeGetter(OVERRIDE);
        paiSample.addIntProperty("timeHigh").notNull().codeBeforeGetter(OVERRIDE);
        paiSample.addFloatProperty("paiToday").notNull().codeBeforeGetter(OVERRIDE);
        paiSample.addFloatProperty("paiTotal").notNull().codeBeforeGetter(OVERRIDE);
        return paiSample;
    }

    private static Entity addHuamiSleepRespiratoryRateSample(Schema schema, Entity user, Entity device) {
        Entity sleepRespiratoryRateSample = addEntity(schema, "HuamiSleepRespiratoryRateSample");
        addCommonTimeSampleProperties("AbstractRespiratoryRateSample", sleepRespiratoryRateSample, user, device);
        sleepRespiratoryRateSample.addIntProperty("utcOffset").notNull();
        sleepRespiratoryRateSample.addIntProperty("rate").notNull().codeBeforeGetter(
                "@Override\n" +
                        "    public float getRespiratoryRate() {\n" +
                        "        return (float) getRate();\n" +
                        "    }\n\n"
        );
        return sleepRespiratoryRateSample;
    }

    private static Entity addXiaomiActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "XiaomiActivitySample");
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.implementsSerializable();
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("stress");
        activitySample.addIntProperty("spo2");
        return activitySample;
    }

    private static Entity addXiaomiSleepTimeSamples(Schema schema, Entity user, Entity device) {
        Entity sample = addEntity(schema, "XiaomiSleepTimeSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sample, user, device);
        sample.addLongProperty("wakeupTime");
        sample.addBooleanProperty("isAwake");
        sample.addIntProperty("totalDuration");
        sample.addIntProperty("deepSleepDuration");
        sample.addIntProperty("lightSleepDuration");
        sample.addIntProperty("remSleepDuration");
        sample.addIntProperty("awakeDuration");
        return sample;
    }

    private static Entity addXiaomiSleepStageSamples(Schema schema, Entity user, Entity device) {
        Entity sample = addEntity(schema, "XiaomiSleepStageSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sample, user, device);
        sample.addIntProperty("stage");
        return sample;
    }

    private static Entity addXiaomiManualSamples(Schema schema, Entity user, Entity device) {
        Entity sample = addEntity(schema, "XiaomiManualSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sample, user, device);
        sample.addIntProperty("type");
        sample.addIntProperty("value");
        return sample;
    }

    private static Entity addXiaomiDailySummarySamples(Schema schema, Entity user, Entity device) {
        Entity sample = addEntity(schema, "XiaomiDailySummarySample");
        addCommonTimeSampleProperties("AbstractTimeSample", sample, user, device);
        sample.addIntProperty("timezone");
        sample.addIntProperty("steps");
        sample.addIntProperty("hrResting");
        sample.addIntProperty("hrMax");
        sample.addIntProperty("hrMaxTs");
        sample.addIntProperty("hrMin");
        sample.addIntProperty("hrMinTs");
        sample.addIntProperty("hrAvg");
        sample.addIntProperty("stressAvg");
        sample.addIntProperty("stressMax");
        sample.addIntProperty("stressMin");
        sample.addIntProperty("standing");
        sample.addIntProperty("calories");
        sample.addIntProperty("spo2Max");
        sample.addIntProperty("spo2MaxTs");
        sample.addIntProperty("spo2Min");
        sample.addIntProperty("spo2MinTs");
        sample.addIntProperty("spo2Avg");
        sample.addIntProperty("trainingLoadDay");
        sample.addIntProperty("trainingLoadWeek");
        sample.addIntProperty("trainingLoadLevel");
        sample.addIntProperty("vitalityIncreaseLight");
        sample.addIntProperty("vitalityIncreaseModerate");
        sample.addIntProperty("vitalityIncreaseHigh");
        sample.addIntProperty("vitalityCurrent");
        return sample;
    }

    private static Entity addCmfActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "CmfActivitySample");
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.implementsSerializable();
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("distance");
        activitySample.addIntProperty("calories");
        return activitySample;
    }

    private static Entity addCmfStressSample(Schema schema, Entity user, Entity device) {
        Entity stressSample = addEntity(schema, "CmfStressSample");
        addCommonTimeSampleProperties("AbstractStressSample", stressSample, user, device);
        stressSample.addIntProperty("stress").notNull().codeBeforeGetter(OVERRIDE);
        return stressSample;
    }

    private static Entity addCmfSpo2Sample(Schema schema, Entity user, Entity device) {
        Entity spo2sample = addEntity(schema, "CmfSpo2Sample");
        addCommonTimeSampleProperties("AbstractSpo2Sample", spo2sample, user, device);
        spo2sample.addIntProperty("spo2").notNull().codeBeforeGetter(OVERRIDE);
        return spo2sample;
    }

    private static Entity addCmfSleepSessionSample(Schema schema, Entity user, Entity device) {
        Entity sleepSessionSample = addEntity(schema, "CmfSleepSessionSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sleepSessionSample, user, device);
        sleepSessionSample.addLongProperty("wakeupTime");
        sleepSessionSample.addByteArrayProperty("metadata");
        return sleepSessionSample;
    }

    private static Entity addCmfSleepStageSample(Schema schema, Entity user, Entity device) {
        Entity sleepStageSample = addEntity(schema, "CmfSleepStageSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sleepStageSample, user, device);
        sleepStageSample.addIntProperty("duration").notNull();
        sleepStageSample.addIntProperty("stage").notNull();
        return sleepStageSample;
    }

    private static Entity addCmfHeartRateSample(Schema schema, Entity user, Entity device) {
        Entity heartRateSample = addEntity(schema, "CmfHeartRateSample");
        addCommonTimeSampleProperties("AbstractHeartRateSample", heartRateSample, user, device);
        heartRateSample.addIntProperty(SAMPLE_HEART_RATE).notNull().codeBeforeGetter(OVERRIDE);
        return heartRateSample;
    }

    private static Entity addCmfWorkoutGpsSample(Schema schema, Entity user, Entity device) {
        Entity sample = addEntity(schema, "CmfWorkoutGpsSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sample, user, device);
        sample.addIntProperty("latitude");
        sample.addIntProperty("longitude");
        return sample;
    }

    private static Entity addColmiActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "ColmiActivitySample");
        addCommonActivitySampleProperties("AbstractColmiActivitySample", activitySample, user, device);
        activitySample.implementsSerializable();
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("distance").notNull();
        activitySample.addIntProperty("calories").notNull();
        return activitySample;
    }

    private static Entity addColmiHeartRateSample(Schema schema, Entity user, Entity device) {
        Entity heartRateSample = addEntity(schema, "ColmiHeartRateSample");
        heartRateSample.implementsSerializable();
        addCommonTimeSampleProperties("AbstractHeartRateSample", heartRateSample, user, device);
        heartRateSample.addIntProperty(SAMPLE_HEART_RATE).notNull();
        return heartRateSample;
    }

    private static Entity addColmiStressSample(Schema schema, Entity user, Entity device) {
        Entity stressSample = addEntity(schema, "ColmiStressSample");
        addCommonTimeSampleProperties("AbstractStressSample", stressSample, user, device);
        stressSample.addIntProperty("stress").notNull().codeBeforeGetter(OVERRIDE);
        return stressSample;
    }

    private static Entity addColmiSpo2Sample(Schema schema, Entity user, Entity device) {
        Entity spo2sample = addEntity(schema, "ColmiSpo2Sample");
        addCommonTimeSampleProperties("AbstractSpo2Sample", spo2sample, user, device);
        spo2sample.addIntProperty("spo2").notNull().codeBeforeGetter(OVERRIDE);
        return spo2sample;
    }

    private static Entity addColmiSleepSessionSample(Schema schema, Entity user, Entity device) {
        Entity sleepSessionSample = addEntity(schema, "ColmiSleepSessionSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sleepSessionSample, user, device);
        sleepSessionSample.addLongProperty("wakeupTime");
        return sleepSessionSample;
    }

    private static Entity addColmiSleepStageSample(Schema schema, Entity user, Entity device) {
        Entity sleepStageSample = addEntity(schema, "ColmiSleepStageSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sleepStageSample, user, device);
        sleepStageSample.addIntProperty("duration").notNull();
        sleepStageSample.addIntProperty("stage").notNull();
        return sleepStageSample;
    }

    private static Entity addColmiHrvValueSample(Schema schema, Entity user, Entity device) {
        Entity hrvValueSample = addEntity(schema, "ColmiHrvValueSample");
        addCommonTimeSampleProperties("AbstractHrvValueSample", hrvValueSample, user, device);
        hrvValueSample.addIntProperty(SAMPLE_HRV_VALUE).notNull().codeBeforeGetter(OVERRIDE);
        return hrvValueSample;
    }

    private static Entity addColmiHrvSummarySample(Schema schema, Entity user, Entity device) {
        Entity hrvSummarySample = addEntity(schema, "ColmiHrvSummarySample");
        addCommonTimeSampleProperties("AbstractHrvSummarySample", hrvSummarySample, user, device);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_WEEKLY_AVERAGE).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_LAST_NIGHT_AVERAGE).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_LAST_NIGHT_5MIN_HIGH).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_BASELINE_LOW_UPPER).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_BASELINE_BALANCED_LOWER).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_BASELINE_BALANCED_UPPER).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_STATUS_NUM).codeBeforeGetter(OVERRIDE);
        return hrvSummarySample;
    }

    private static void addHeartRateProperties(Entity activitySample) {
        activitySample.addIntProperty(SAMPLE_HEART_RATE).notNull().codeBeforeGetterAndSetter(OVERRIDE);
    }

    private static Entity addPebbleHealthActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PebbleHealthActivitySample");
        addCommonActivitySampleProperties("AbstractPebbleHealthActivitySample", activitySample, user, device);
        activitySample.addByteArrayProperty("rawPebbleHealthData").codeBeforeGetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addPebbleHealthActivityKindOverlay(Schema schema, Entity user, Entity device) {
        Entity activityOverlay = addEntity(schema, "PebbleHealthActivityOverlay");

        activityOverlay.addIntProperty(TIMESTAMP_FROM).notNull().primaryKey();
        activityOverlay.addIntProperty(TIMESTAMP_TO).notNull().primaryKey();
        activityOverlay.addIntProperty(SAMPLE_RAW_KIND).notNull().primaryKey();
        Property deviceId = activityOverlay.addLongProperty("deviceId").primaryKey().notNull().getProperty();
        activityOverlay.addToOne(device, deviceId);

        Property userId = activityOverlay.addLongProperty("userId").notNull().getProperty();
        activityOverlay.addToOne(user, userId);
        activityOverlay.addByteArrayProperty("rawPebbleHealthData");

        return activityOverlay;
    }

    private static Entity addPebbleMisfitActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PebbleMisfitSample");
        addCommonActivitySampleProperties("AbstractPebbleMisfitActivitySample", activitySample, user, device);
        activitySample.addIntProperty("rawPebbleMisfitSample").notNull().codeBeforeGetter(OVERRIDE);
        return activitySample;
    }

    private static Entity addPebbleMorpheuzActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PebbleMorpheuzSample");
        addCommonActivitySampleProperties("AbstractPebbleMorpheuzActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        return activitySample;
    }

    private static Entity addHPlusHealthActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "HPlusHealthActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addByteArrayProperty("rawHPlusHealthData");
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().primaryKey();
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("distance");
        activitySample.addIntProperty("calories");

        return activitySample;
    }

    private static Entity addHPlusHealthActivityKindOverlay(Schema schema, Entity user, Entity device) {
        Entity activityOverlay = addEntity(schema, "HPlusHealthActivityOverlay");

        activityOverlay.addIntProperty(TIMESTAMP_FROM).notNull().primaryKey();
        activityOverlay.addIntProperty(TIMESTAMP_TO).notNull().primaryKey();
        activityOverlay.addIntProperty(SAMPLE_RAW_KIND).notNull().primaryKey();
        Property deviceId = activityOverlay.addLongProperty("deviceId").primaryKey().notNull().getProperty();
        activityOverlay.addToOne(device, deviceId);

        Property userId = activityOverlay.addLongProperty("userId").notNull().getProperty();
        activityOverlay.addToOne(user, userId);
        activityOverlay.addByteArrayProperty("rawHPlusHealthData");
        return activityOverlay;
    }

    private static Entity addNo1F1ActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "No1F1ActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addXWatchActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "XWatchActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addZeTimeActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "ZeTimeActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("caloriesBurnt");
        activitySample.addIntProperty("distanceMeters");
        activitySample.addIntProperty("activeTimeMinutes");
        return activitySample;
    }

    private static Entity addID115ActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "ID115ActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("caloriesBurnt");
        activitySample.addIntProperty("distanceMeters");
        activitySample.addIntProperty("activeTimeMinutes");
        return activitySample;
    }

    private static Entity addJYouActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "JYouActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("caloriesBurnt");
        activitySample.addIntProperty("distanceMeters");
        activitySample.addIntProperty("activeTimeMinutes");
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addHybridHRActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "HybridHRActivitySample");
        activitySample.implementsSerializable();

        addCommonActivitySampleProperties("AbstractHybridHRActivitySample", activitySample, user, device);

        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("calories").notNull();
        activitySample.addIntProperty("variability").notNull();
        activitySample.addIntProperty("max_variability").notNull();
        activitySample.addIntProperty("heartrate_quality").notNull();
        activitySample.addBooleanProperty("active").notNull();
        activitySample.addByteProperty("wear_type").notNull();
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addCyclingSample(Schema schema, Entity user, Entity device) {
        Entity cyclingSample = addEntity(schema, "CyclingSample");
        addCommonTimeSampleProperties("AbstractTimeSample", cyclingSample, user, device);

        cyclingSample.implementsSerializable();

        cyclingSample.addIntProperty("RevolutionCount");
        cyclingSample.addFloatProperty("Distance");
        cyclingSample.addFloatProperty("Speed");
        return cyclingSample;
    }

    private static Entity addVivomoveHrActivitySample(Schema schema, Entity user, Entity device) {
        final Entity activitySample = addEntity(schema, "VivomoveHrActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_HEART_RATE).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("caloriesBurnt");
        activitySample.addIntProperty("floorsClimbed");
        return activitySample;
    }

    private static Entity addGarminFitFile(Schema schema, Entity user, Entity device) {
        final Entity downloadedFitFile = addEntity(schema, "GarminFitFile");
        downloadedFitFile.implementsSerializable();
        downloadedFitFile.setJavaDoc("This class represents a single FIT file downloaded from a FIT-compatible Garmin device.");
        downloadedFitFile.addIdProperty().autoincrement();
        downloadedFitFile.addLongProperty("downloadTimestamp").notNull();
        final Property deviceId = downloadedFitFile.addLongProperty("deviceId").notNull().getProperty();
        downloadedFitFile.addToOne(device, deviceId);
        final Property userId = downloadedFitFile.addLongProperty("userId").notNull().getProperty();
        downloadedFitFile.addToOne(user, userId);
        final Property fileNumber = downloadedFitFile.addIntProperty("fileNumber").notNull().getProperty();
        downloadedFitFile.addIntProperty("fileDataType").notNull();
        downloadedFitFile.addIntProperty("fileSubType").notNull();
        downloadedFitFile.addLongProperty("fileTimestamp").notNull();
        downloadedFitFile.addIntProperty("specificFlags").notNull();
        downloadedFitFile.addIntProperty("fileSize").notNull();
        downloadedFitFile.addByteArrayProperty("fileData");

        final Index indexUnique = new Index();
        indexUnique.addProperty(deviceId);
        indexUnique.addProperty(userId);
        indexUnique.addProperty(fileNumber);
        indexUnique.makeUnique();

        downloadedFitFile.addIndex(indexUnique);

        return downloadedFitFile;
    }

    private static Entity addGarminActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "GarminActivitySample");
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.implementsSerializable();
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("distanceCm").notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("activeCalories").notNull().codeBeforeGetterAndSetter(OVERRIDE);
        return activitySample;
    }

    private static Entity addGarminStressSample(Schema schema, Entity user, Entity device) {
        Entity stressSample = addEntity(schema, "GarminStressSample");
        addCommonTimeSampleProperties("AbstractStressSample", stressSample, user, device);
        stressSample.addIntProperty("stress").notNull().codeBeforeGetter(OVERRIDE);
        return stressSample;
    }

    private static Entity addGarminBodyEnergySample(Schema schema, Entity user, Entity device) {
        Entity stressSample = addEntity(schema, "GarminBodyEnergySample");
        addCommonTimeSampleProperties("AbstractBodyEnergySample", stressSample, user, device);
        stressSample.addIntProperty("energy").notNull().codeBeforeGetter(OVERRIDE);
        return stressSample;
    }

    private static Entity addGarminSpo2Sample(Schema schema, Entity user, Entity device) {
        Entity spo2sample = addEntity(schema, "GarminSpo2Sample");
        addCommonTimeSampleProperties("AbstractSpo2Sample", spo2sample, user, device);
        spo2sample.addIntProperty("spo2").notNull().codeBeforeGetter(OVERRIDE);
        return spo2sample;
    }

    private static Entity addGarminSleepStageSample(Schema schema, Entity user, Entity device) {
        Entity sleepStageSample = addEntity(schema, "GarminSleepStageSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sleepStageSample, user, device);
        sleepStageSample.addIntProperty("stage").notNull();
        return sleepStageSample;
    }

    private static Entity addGarminEventSample(Schema schema, Entity user, Entity device) {
        Entity sleepStageSample = addEntity(schema, "GarminEventSample");
        addCommonTimeSampleProperties("AbstractTimeSample", sleepStageSample, user, device);
        sleepStageSample.addIntProperty("event").notNull().primaryKey();
        sleepStageSample.addIntProperty("eventType");
        sleepStageSample.addLongProperty("data");
        return sleepStageSample;
    }

    private static Entity addGarminHrvSummarySample(Schema schema, Entity user, Entity device) {
        Entity hrvSummarySample = addEntity(schema, "GarminHrvSummarySample");
        addCommonTimeSampleProperties("AbstractHrvSummarySample", hrvSummarySample, user, device);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_WEEKLY_AVERAGE).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_LAST_NIGHT_AVERAGE).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_LAST_NIGHT_5MIN_HIGH).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_BASELINE_LOW_UPPER).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_BASELINE_BALANCED_LOWER).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_BASELINE_BALANCED_UPPER).codeBeforeGetter(OVERRIDE);
        hrvSummarySample.addIntProperty(SAMPLE_HRV_STATUS_NUM).codeBeforeGetter(OVERRIDE);
        return hrvSummarySample;
    }

    private static Entity addGarminHrvValueSample(Schema schema, Entity user, Entity device) {
        Entity hrvValueSample = addEntity(schema, "GarminHrvValueSample");
        addCommonTimeSampleProperties("AbstractHrvValueSample", hrvValueSample, user, device);
        hrvValueSample.addIntProperty("value").notNull().codeBeforeGetter(OVERRIDE);
        return hrvValueSample;
    }

    private static Entity addGarminRespiratoryRateSample(Schema schema, Entity user, Entity device) {
        Entity garminRespiratoryRateSample = addEntity(schema, "GarminRespiratoryRateSample");
        addCommonTimeSampleProperties("AbstractRespiratoryRateSample", garminRespiratoryRateSample, user, device);
        garminRespiratoryRateSample.addFloatProperty("respiratoryRate").notNull().codeBeforeGetter(OVERRIDE);
        return garminRespiratoryRateSample;
    }

    private static Entity addGarminHeartRateRestingSample(Schema schema, Entity user, Entity device) {
        Entity hrRestingSample = addEntity(schema, "GarminHeartRateRestingSample");
        addCommonTimeSampleProperties("AbstractHeartRateSample", hrRestingSample, user, device);
        hrRestingSample.addIntProperty(SAMPLE_HEART_RATE).notNull().codeBeforeGetter(OVERRIDE);
        return hrRestingSample;
    }

    private static Entity addPendingFile(Schema schema, Entity user, Entity device) {
        Entity pendingFile = addEntity(schema, "PendingFile");
        pendingFile.setJavaDoc(
                "This class represents a file that was fetched from the device and is pending processing."
        );

        // We need a single-column primary key so that we can delete records
        pendingFile.addIdProperty().autoincrement();

        Property path = pendingFile.addStringProperty("path").notNull().getProperty();
        Property deviceId = pendingFile.addLongProperty("deviceId").notNull().getProperty();
        pendingFile.addToOne(device, deviceId);

        final Index indexUnique = new Index();
        indexUnique.addProperty(deviceId);
        indexUnique.addProperty(path);
        indexUnique.makeUnique();
        pendingFile.addIndex(indexUnique);

        return pendingFile;
    }

    private static Entity addWatchXPlusHealthActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "WatchXPlusActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addByteArrayProperty("rawWatchXPlusHealthData");
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().primaryKey();
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("distance");
        activitySample.addIntProperty("calories");
        return activitySample;
    }

    private static Entity addWatchXPlusHealthActivityKindOverlay(Schema schema, Entity user, Entity device) {
        Entity activityOverlay = addEntity(schema, "WatchXPlusHealthActivityOverlay");

        activityOverlay.addIntProperty(TIMESTAMP_FROM).notNull().primaryKey();
        activityOverlay.addIntProperty(TIMESTAMP_TO).notNull().primaryKey();
        activityOverlay.addIntProperty(SAMPLE_RAW_KIND).notNull().primaryKey();
        Property deviceId = activityOverlay.addLongProperty("deviceId").primaryKey().notNull().getProperty();
        activityOverlay.addToOne(device, deviceId);

        Property userId = activityOverlay.addLongProperty("userId").notNull().getProperty();
        activityOverlay.addToOne(user, userId);
        activityOverlay.addByteArrayProperty("rawWatchXPlusHealthData");

        return activityOverlay;
    }

    private static Entity addTLW64ActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "TLW64ActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        return activitySample;
    }

    private static Entity addSonySWR12Sample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "SonySWR12Sample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        return activitySample;
    }

    private static Entity addCasioGBX100Sample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "CasioGBX100ActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractGBX100ActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("calories").notNull();
        return activitySample;
    }

    private static Entity addLefunActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "LefunActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("distance").notNull();
        activitySample.addIntProperty("calories").notNull();
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addLefunBiometricSample(Schema schema, Entity user, Entity device) {
        Entity biometricSample = addEntity(schema, "LefunBiometricSample");
        biometricSample.implementsSerializable();

        biometricSample.addIntProperty("timestamp").notNull().primaryKey();
        Property deviceId = biometricSample.addLongProperty("deviceId").primaryKey().notNull().getProperty();
        biometricSample.addToOne(device, deviceId);
        Property userId = biometricSample.addLongProperty("userId").notNull().getProperty();
        biometricSample.addToOne(user, userId);

        biometricSample.addIntProperty("type").notNull();
        biometricSample.addIntProperty("value1").notNull();
        biometricSample.addIntProperty("value2");
        return biometricSample;
    }

    private static Entity addLefunSleepSample(Schema schema, Entity user, Entity device) {
        Entity sleepSample = addEntity(schema, "LefunSleepSample");
        sleepSample.implementsSerializable();

        sleepSample.addIntProperty("timestamp").notNull().primaryKey();
        Property deviceId = sleepSample.addLongProperty("deviceId").primaryKey().notNull().getProperty();
        sleepSample.addToOne(device, deviceId);
        Property userId = sleepSample.addLongProperty("userId").notNull().getProperty();
        sleepSample.addToOne(user, userId);

        sleepSample.addIntProperty("type").notNull();
        return sleepSample;
    }

    private static Entity addBangleJSActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "BangleJSActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static void addCommonActivitySampleProperties(String superClass, Entity activitySample, Entity user, Entity device) {
        activitySample.setSuperclass(superClass);
        activitySample.addImport(MAIN_PACKAGE + ".devices.SampleProvider");
        activitySample.setJavaDoc(
                "This class represents a sample specific to the device. Values like activity kind or\n" +
                        "intensity, are device specific. Normalized values can be retrieved through the\n" +
                        "corresponding {@link SampleProvider}.");
        activitySample.addIntProperty("timestamp").notNull().codeBeforeGetterAndSetter(OVERRIDE).primaryKey();
        Property deviceId = activitySample.addLongProperty("deviceId").primaryKey().notNull().codeBeforeGetterAndSetter(OVERRIDE).getProperty();
        activitySample.addToOne(device, deviceId);
        Property userId = activitySample.addLongProperty("userId").notNull().codeBeforeGetterAndSetter(OVERRIDE).getProperty();
        activitySample.addToOne(user, userId);
    }

    private static void addCommonTimeSampleProperties(String superClass, Entity timeSample, Entity user, Entity device) {
        timeSample.setSuperclass(superClass);
        timeSample.addImport(MAIN_PACKAGE + ".devices.TimeSampleProvider");
        timeSample.setJavaDoc(
                "This class represents a sample specific to the device. Values might be device specific, depending on the sample type.\n" +
                        "Normalized values can be retrieved through the corresponding {@link TimeSampleProvider}.");
        timeSample.addLongProperty("timestamp").notNull().codeBeforeGetterAndSetter(OVERRIDE).primaryKey();
        Property deviceId = timeSample.addLongProperty("deviceId").primaryKey().notNull().codeBeforeGetterAndSetter(OVERRIDE).getProperty();
        timeSample.addToOne(device, deviceId);
        Property userId = timeSample.addLongProperty("userId").notNull().codeBeforeGetterAndSetter(OVERRIDE).getProperty();
        timeSample.addToOne(user, userId);
    }

    private static void addCalendarSyncState(Schema schema, Entity device) {
        Entity calendarSyncState = addEntity(schema, "CalendarSyncState");
        calendarSyncState.addIdProperty();
        Property deviceId = calendarSyncState.addLongProperty("deviceId").notNull().getProperty();
        Property calendarEntryId = calendarSyncState.addLongProperty("calendarEntryId").notNull().getProperty();
        Index indexUnique = new Index();
        indexUnique.addProperty(deviceId);
        indexUnique.addProperty(calendarEntryId);
        indexUnique.makeUnique();
        calendarSyncState.addIndex(indexUnique);
        calendarSyncState.addToOne(device, deviceId);
        calendarSyncState.addIntProperty("hash").notNull();
    }

    private static void addAlarms(Schema schema, Entity user, Entity device) {
        Entity alarm = addEntity(schema, "Alarm");
        alarm.implementsInterface("nodomain.freeyourgadget.gadgetbridge.model.Alarm");
        Property deviceId = alarm.addLongProperty("deviceId").notNull().getProperty();
        Property userId = alarm.addLongProperty("userId").notNull().getProperty();
        Property position = alarm.addIntProperty("position").notNull().getProperty();
        Index indexUnique = new Index();
        indexUnique.addProperty(deviceId);
        indexUnique.addProperty(userId);
        indexUnique.addProperty(position);
        indexUnique.makeUnique();
        alarm.addIndex(indexUnique);
        alarm.addBooleanProperty("enabled").notNull();
        alarm.addBooleanProperty("smartWakeup").notNull();
        alarm.addIntProperty("smartWakeupInterval");
        alarm.addBooleanProperty("snooze").notNull();
        alarm.addIntProperty("repetition").notNull().codeBeforeGetter(
                "public boolean isRepetitive() { return getRepetition() != ALARM_ONCE; } " +
                        "public boolean getRepetition(int dow) { return (this.repetition & dow) > 0; }"
        );
        alarm.addIntProperty("hour").notNull();
        alarm.addIntProperty("minute").notNull();
        alarm.addBooleanProperty("unused").notNull();
        alarm.addStringProperty("title");
        alarm.addStringProperty("description");
        alarm.addToOne(user, userId);
        alarm.addToOne(device, deviceId);
    }

    private static void addReminders(Schema schema, Entity user, Entity device) {
        Entity reminder = addEntity(schema, "Reminder");
        reminder.implementsInterface("nodomain.freeyourgadget.gadgetbridge.model.Reminder");
        Property deviceId = reminder.addLongProperty("deviceId").notNull().getProperty();
        Property userId = reminder.addLongProperty("userId").notNull().getProperty();
        Property reminderId = reminder.addStringProperty("reminderId").notNull().primaryKey().getProperty();
        Index indexUnique = new Index();
        indexUnique.addProperty(deviceId);
        indexUnique.addProperty(userId);
        indexUnique.addProperty(reminderId);
        indexUnique.makeUnique();
        reminder.addIndex(indexUnique);
        reminder.addStringProperty("message").notNull();
        reminder.addDateProperty("date").notNull();
        reminder.addIntProperty("repetition").notNull();
        reminder.addToOne(user, userId);
        reminder.addToOne(device, deviceId);
    }

    private static void addWorldClocks(Schema schema, Entity user, Entity device) {
        Entity worldClock = addEntity(schema, "WorldClock");
        worldClock.implementsInterface("nodomain.freeyourgadget.gadgetbridge.model.WorldClock");
        Property deviceId = worldClock.addLongProperty("deviceId").notNull().getProperty();
        Property userId = worldClock.addLongProperty("userId").notNull().getProperty();
        Property worldClockId = worldClock.addStringProperty("worldClockId").notNull().primaryKey().getProperty();
        Index indexUnique = new Index();
        indexUnique.addProperty(deviceId);
        indexUnique.addProperty(userId);
        indexUnique.addProperty(worldClockId);
        indexUnique.makeUnique();
        worldClock.addIndex(indexUnique);
        worldClock.addStringProperty("label").notNull();
        worldClock.addBooleanProperty("enabled");
        worldClock.addStringProperty("code");
        worldClock.addStringProperty("timeZoneId").notNull();
        worldClock.addToOne(user, userId);
        worldClock.addToOne(device, deviceId);
    }

    private static void addContacts(Schema schema, Entity user, Entity device) {
        Entity contact = addEntity(schema, "Contact");
        contact.implementsInterface("nodomain.freeyourgadget.gadgetbridge.model.Contact");
        Property deviceId = contact.addLongProperty("deviceId").notNull().getProperty();
        Property userId = contact.addLongProperty("userId").notNull().getProperty();
        Property contactId = contact.addStringProperty("contactId").notNull().primaryKey().getProperty();
        Index indexUnique = new Index();
        indexUnique.addProperty(deviceId);
        indexUnique.addProperty(userId);
        indexUnique.addProperty(contactId);
        indexUnique.makeUnique();
        contact.addIndex(indexUnique);
        contact.addStringProperty("name").notNull();
        contact.addStringProperty("number").notNull();
        contact.addToOne(user, userId);
        contact.addToOne(device, deviceId);
    }

    private static void addNotificationFilterEntry(Schema schema, Entity notificationFilterEntity) {
        Entity notificatonFilterEntry = addEntity(schema, "NotificationFilterEntry");
        notificatonFilterEntry.addIdProperty().autoincrement();
        Property notificationFilterId = notificatonFilterEntry.addLongProperty("notificationFilterId").notNull().getProperty();
        notificatonFilterEntry.addStringProperty("notificationFilterContent").notNull().getProperty();
        notificatonFilterEntry.addToOne(notificationFilterEntity, notificationFilterId);
    }

    private static Entity addNotificationFilters(Schema schema) {
        Entity notificatonFilter = addEntity(schema, "NotificationFilter");
        Property appIdentifier = notificatonFilter.addStringProperty("appIdentifier").notNull().getProperty();

        notificatonFilter.addIdProperty().autoincrement();

        Index indexUnique = new Index();
        indexUnique.addProperty(appIdentifier);
        indexUnique.makeUnique();
        notificatonFilter.addIndex(indexUnique);

        Property notificationFilterMode = notificatonFilter.addIntProperty("notificationFilterMode").notNull().getProperty();
        Property notificationFilterSubMode = notificatonFilter.addIntProperty("notificationFilterSubMode").notNull().getProperty();
        return notificatonFilter;
    }

    private static void addActivitySummary(Schema schema, Entity user, Entity device) {
        Entity summary = addEntity(schema, "BaseActivitySummary");
        summary.implementsInterface(ACTIVITY_SUMMARY);
        summary.addIdProperty();

        summary.setJavaDoc(
                "This class represents the summary of a user's activity event. I.e. a walk, hike, a bicycle tour, etc.");

        summary.addStringProperty("name").codeBeforeGetter(OVERRIDE);
        summary.addDateProperty("startTime").notNull().codeBeforeGetter(OVERRIDE);
        summary.addDateProperty("endTime").notNull().codeBeforeGetter(OVERRIDE);
        summary.addIntProperty("activityKind").notNull().codeBeforeGetter(OVERRIDE);

        summary.addIntProperty("baseLongitude").javaDocGetterAndSetter("Temporary, bip-specific");
        summary.addIntProperty("baseLatitude").javaDocGetterAndSetter("Temporary, bip-specific");
        summary.addIntProperty("baseAltitude").javaDocGetterAndSetter("Temporary, bip-specific");

        summary.addStringProperty("gpxTrack").codeBeforeGetter(OVERRIDE);
        summary.addStringProperty("rawDetailsPath");

        Property deviceId = summary.addLongProperty("deviceId").notNull().codeBeforeGetter(OVERRIDE).getProperty();
        summary.addToOne(device, deviceId);
        Property userId = summary.addLongProperty("userId").notNull().codeBeforeGetter(OVERRIDE).getProperty();
        summary.addToOne(user, userId);
        summary.addStringProperty("summaryData");
        summary.addByteArrayProperty("rawSummaryData");
    }

    private static Property findProperty(Entity entity, String propertyName) {
        for (Property prop : entity.getProperties()) {
            if (propertyName.equals(prop.getPropertyName())) {
                return prop;
            }
        }
        throw new IllegalArgumentException("Property " + propertyName + " not found in Entity " + entity.getClassName());
    }

    private static Entity addEntity(Schema schema, String className) {
        Entity entity = schema.addEntity(className);
        entity.addImport("de.greenrobot.dao.AbstractDao");
        return entity;
    }

    private static Entity addBatteryLevel(Schema schema, Entity device) {
        Entity batteryLevel = addEntity(schema, "BatteryLevel");
        batteryLevel.implementsSerializable();
        batteryLevel.addIntProperty("timestamp").notNull().primaryKey();
        Property deviceId = batteryLevel.addLongProperty("deviceId").primaryKey().notNull().getProperty();
        batteryLevel.addToOne(device, deviceId);
        batteryLevel.addIntProperty("level").notNull();
        batteryLevel.addIntProperty("batteryIndex").notNull().primaryKey();
        return batteryLevel;
    }

    private static Entity addFitProActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "FitProActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractFitProActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty("caloriesBurnt");
        activitySample.addIntProperty("distanceMeters");
        activitySample.addIntProperty("spo2Percent");
        activitySample.addIntProperty("pressureLowMmHg");
        activitySample.addIntProperty("pressureHighMmHg");
        activitySample.addIntProperty("activeTimeMinutes");
        return activitySample;
    }

    private static Entity addPineTimeActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PineTimeActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }


    private static Entity addWithingsSteelHRActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "WithingsSteelHRActivitySample");
        activitySample.implementsSerializable();
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty("duration").notNull();
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("distance").notNull();
        activitySample.addIntProperty("calories").notNull();
        addHeartRateProperties(activitySample);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        return activitySample;
    }

    private static Entity addWena3BehaviorSample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "Wena3BehaviorSample");
        addCommonTimeSampleProperties("AbstractTimeSample", activitySample, user, device);

        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull();
        activitySample.addLongProperty(TIMESTAMP_FROM).notNull();
        activitySample.addLongProperty(TIMESTAMP_TO).notNull();
        return activitySample;
    }

    private static Entity addWena3Vo2Sample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "Wena3Vo2Sample");
        addCommonTimeSampleProperties("AbstractTimeSample", activitySample, user, device);
        activitySample.addIntProperty("vo2").notNull();
        activitySample.addIntProperty("datapoint").notNull().primaryKey();
        return activitySample;
    }

    private static Entity addWena3StressSample(Schema schema, Entity user, Entity device) {
        Entity stressSample = addEntity(schema, "Wena3StressSample");
        addCommonTimeSampleProperties("AbstractStressSample", stressSample, user, device);
        stressSample.addIntProperty("typeNum").notNull().codeBeforeGetter(OVERRIDE);
        stressSample.addIntProperty("stress").notNull().codeBeforeGetter(OVERRIDE);
        return stressSample;
    }

    private static Entity addWena3ActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "Wena3ActivitySample");
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull();
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetter(OVERRIDE);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static Entity addWena3HeartRateSample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "Wena3HeartRateSample");
        addCommonTimeSampleProperties("AbstractHeartRateSample", activitySample, user, device);
        activitySample.addIntProperty(SAMPLE_HEART_RATE).notNull();
        return activitySample;
    }

    private static Entity addWena3EnergySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "Wena3EnergySample");
        addCommonTimeSampleProperties("AbstractTimeSample", activitySample, user, device);
        activitySample.addIntProperty("energy").notNull();
        return activitySample;
    }

    private static Entity addWena3CaloriesSample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "Wena3CaloriesSample");
        addCommonTimeSampleProperties("AbstractTimeSample", activitySample, user, device);
        activitySample.addIntProperty("calories").notNull();
        return activitySample;
    }

    private static Entity addAppSpecificNotificationSettings(Schema schema, Entity device) {
        Entity perAppSetting = addEntity(schema, "AppSpecificNotificationSetting");
        perAppSetting.addStringProperty("packageId").notNull().primaryKey();
        Property deviceId = perAppSetting.addLongProperty("deviceId").primaryKey().notNull().getProperty();
        perAppSetting.addToOne(device, deviceId);
        perAppSetting.addStringProperty("ledPattern");
        perAppSetting.addStringProperty("vibrationPattern");
        perAppSetting.addStringProperty("vibrationRepetition");
        return perAppSetting;
    }

    private static Entity addHuaweiActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "HuaweiActivitySample");
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty("otherTimestamp").notNull().primaryKey();
        activitySample.addByteProperty("source").notNull().primaryKey();
        activitySample.addIntProperty(SAMPLE_RAW_KIND).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_RAW_INTENSITY).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_STEPS).notNull().codeBeforeGetterAndSetter(OVERRIDE);
        activitySample.addIntProperty("calories").notNull().codeBeforeGetter(
                "@Override\n" +
                "    public int getActiveCalories() {\n" +
                "        return getCalories();\n" +
                "    }\n"
        );
        activitySample.addIntProperty("distance").notNull().codeBeforeGetter(
                "@Override\n" +
                "    public int getDistanceCm() {\n" +
                "        return getDistance() * 100;\n" +
                "    }\n"
        );
        activitySample.addIntProperty("spo").notNull();
        activitySample.addIntProperty("heartRate").notNull();
        return activitySample;
    }

    private static Entity addHuaweiWorkoutSummarySample(Schema schema, Entity user, Entity device) {
        Entity workoutSummary = addEntity(schema, "HuaweiWorkoutSummarySample");

        workoutSummary.setJavaDoc("Contains Huawei Workout Summary samples (one per workout)");

        workoutSummary.addLongProperty("workoutId").primaryKey().autoincrement();

        Property deviceId = workoutSummary.addLongProperty("deviceId").notNull().getProperty();
        workoutSummary.addToOne(device, deviceId);
        Property userId = workoutSummary.addLongProperty("userId").notNull().getProperty();
        workoutSummary.addToOne(user, userId);

        workoutSummary.addShortProperty("workoutNumber").notNull();
        workoutSummary.addByteProperty("status").notNull();
        workoutSummary.addIntProperty("startTimestamp").notNull();
        workoutSummary.addIntProperty("endTimestamp").notNull();
        workoutSummary.addIntProperty("calories").notNull();
        workoutSummary.addIntProperty("distance").notNull();
        workoutSummary.addIntProperty("stepCount").notNull();
        workoutSummary.addIntProperty("totalTime").notNull();
        workoutSummary.addIntProperty("duration").notNull();
        workoutSummary.addByteProperty("type").notNull();
        workoutSummary.addShortProperty("strokes").notNull();
        workoutSummary.addShortProperty("avgStrokeRate").notNull();
        workoutSummary.addShortProperty("poolLength").notNull();
        workoutSummary.addShortProperty("laps").notNull();
        workoutSummary.addShortProperty("avgSwolf").notNull();

        workoutSummary.addByteArrayProperty("rawData");

        workoutSummary.addStringProperty("gpxFileLocation");

        workoutSummary.addIntProperty("maxAltitude");
        workoutSummary.addIntProperty("minAltitude");
        workoutSummary.addIntProperty("elevationGain");
        workoutSummary.addIntProperty("elevationLoss");

        workoutSummary.addIntProperty("workoutLoad").notNull();
        workoutSummary.addIntProperty("workoutAerobicEffect").notNull();
        workoutSummary.addByteProperty("workoutAnaerobicEffect").notNull();
        workoutSummary.addShortProperty("recoveryTime").notNull();

        workoutSummary.addByteProperty("minHeartRatePeak").notNull();
        workoutSummary.addByteProperty("maxHeartRatePeak").notNull();

        workoutSummary.addByteArrayProperty("recoveryHeartRates");

        workoutSummary.addByteProperty("swimType").notNull();

        return workoutSummary;
    }

    private static Entity addHuaweiWorkoutDataSample(Schema schema, Entity summaryEntity) {
        Entity workoutDataSample = addEntity(schema, "HuaweiWorkoutDataSample");

        workoutDataSample.setJavaDoc("Contains Huawei Workout data samples (multiple per workout)");

        Property id = workoutDataSample.addLongProperty("workoutId").primaryKey().notNull().getProperty();
        workoutDataSample.addToOne(summaryEntity, id);

        workoutDataSample.addIntProperty("timestamp").notNull().primaryKey();
        workoutDataSample.addByteProperty("heartRate").notNull();
        workoutDataSample.addShortProperty("speed").notNull();
        workoutDataSample.addByteProperty("stepRate").notNull();
        workoutDataSample.addShortProperty("cadence").notNull();
        workoutDataSample.addShortProperty("stepLength").notNull();
        workoutDataSample.addShortProperty("groundContactTime").notNull();
        workoutDataSample.addByteProperty("impact").notNull();
        workoutDataSample.addShortProperty("swingAngle").notNull();
        workoutDataSample.addByteProperty("foreFootLanding").notNull();
        workoutDataSample.addByteProperty("midFootLanding").notNull();
        workoutDataSample.addByteProperty("backFootLanding").notNull();
        workoutDataSample.addByteProperty("eversionAngle").notNull();
        workoutDataSample.addShortProperty("swolf").notNull();
        workoutDataSample.addShortProperty("strokeRate").notNull();

        workoutDataSample.addByteArrayProperty("dataErrorHex");

        workoutDataSample.addShortProperty("calories").notNull();
        workoutDataSample.addShortProperty("cyclingPower").notNull();

        workoutDataSample.addShortProperty("frequency").notNull();
        workoutDataSample.addIntProperty("altitude");

        return workoutDataSample;
    }

    private static Entity addHuaweiWorkoutPaceSample(Schema schema, Entity summaryEntity) {
        Entity workoutPaceSample = addEntity(schema, "HuaweiWorkoutPaceSample");

        workoutPaceSample.setJavaDoc("Contains Huawei Workout pace data samples (one per workout)");

        Property id = workoutPaceSample.addLongProperty("workoutId").primaryKey().notNull().getProperty();
        workoutPaceSample.addToOne(summaryEntity, id);

        workoutPaceSample.addIntProperty("paceIndex").notNull().primaryKey();
        workoutPaceSample.addIntProperty("distance").notNull().primaryKey();
        workoutPaceSample.addByteProperty("type").notNull().primaryKey();
        workoutPaceSample.addIntProperty("pace").notNull();
        workoutPaceSample.addIntProperty("pointIndex").notNull();
        workoutPaceSample.addIntProperty("correction");

        return workoutPaceSample;
    }

    private static Entity addHuaweiWorkoutSwimSegmentsSample(Schema schema, Entity summaryEntity) {
        Entity workoutSwimSegmentsSample = addEntity(schema, "HuaweiWorkoutSwimSegmentsSample");

        workoutSwimSegmentsSample.setJavaDoc("Contains Huawei Workout swim segments data samples");

        Property id = workoutSwimSegmentsSample.addLongProperty("workoutId").primaryKey().notNull().getProperty();
        workoutSwimSegmentsSample.addToOne(summaryEntity, id);

        workoutSwimSegmentsSample.addIntProperty("segmentIndex").notNull().primaryKey();
        workoutSwimSegmentsSample.addIntProperty("distance").notNull().primaryKey();
        workoutSwimSegmentsSample.addByteProperty("type").notNull().primaryKey();
        workoutSwimSegmentsSample.addIntProperty("pace").notNull();
        workoutSwimSegmentsSample.addIntProperty("pointIndex").notNull();
        workoutSwimSegmentsSample.addIntProperty("segment").notNull();
        workoutSwimSegmentsSample.addByteProperty("swimType").notNull();
        workoutSwimSegmentsSample.addIntProperty("strokes").notNull();
        workoutSwimSegmentsSample.addIntProperty("avgSwolf").notNull();
        workoutSwimSegmentsSample.addIntProperty("time").notNull();

        return workoutSwimSegmentsSample;
    }

    private static Entity addHuaweiDictData(Schema schema, Entity user, Entity device) {
        Entity dictData = addEntity(schema, "HuaweiDictData");

        dictData.setJavaDoc("Contains Huawei Dict Data");

        dictData.addLongProperty("dictId").primaryKey().autoincrement();

        Property deviceId = dictData.addLongProperty("deviceId").notNull().getProperty();
        dictData.addToOne(device, deviceId);
        Property userId = dictData.addLongProperty("userId").notNull().getProperty();
        dictData.addToOne(user, userId);

        dictData.addIntProperty("dictClass").notNull();
        dictData.addLongProperty("startTimestamp").notNull();
        dictData.addLongProperty("endTimestamp");
        dictData.addLongProperty("modifyTimestamp");

        return dictData;
    }

    private static Entity addHuaweiDictDataValues(Schema schema, Entity summaryEntity) {
        Entity dictDataValues = addEntity(schema, "HuaweiDictDataValues");

        dictDataValues.setJavaDoc("Contains Huawei Dict data values");

        Property id = dictDataValues.addLongProperty("dictId").primaryKey().notNull().getProperty();
        dictDataValues.addToOne(summaryEntity, id);

        dictDataValues.addIntProperty("dictType").notNull().primaryKey();
        dictDataValues.addByteProperty("tag").notNull().primaryKey();
        dictDataValues.addByteArrayProperty("value");

        return dictDataValues;
    }


    private static void addTemperatureProperties(Entity activitySample) {
        activitySample.addFloatProperty(SAMPLE_TEMPERATURE).notNull().codeBeforeGetter(OVERRIDE);
        activitySample.addIntProperty(SAMPLE_TEMPERATURE_TYPE).notNull().codeBeforeGetter(OVERRIDE);
    }

    private static Entity addFemometerVinca2TemperatureSample(Schema schema, Entity user, Entity device) {
        Entity sample = addEntity(schema, "FemometerVinca2TemperatureSample");
        addCommonTimeSampleProperties("AbstractTemperatureSample", sample, user, device);
        addTemperatureProperties(sample);
        return sample;
    }

    private static Entity addMiScaleWeightSample(Schema schema, Entity user, Entity device) {
        Entity sample = addEntity(schema, "MiScaleWeightSample");
        addCommonTimeSampleProperties("AbstractWeightSample", sample, user, device);
        sample.addFloatProperty(SAMPLE_WEIGHT_KG).notNull().codeBeforeGetter(OVERRIDE);
        return sample;
    }
}
