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
    private static final String OVERRIDE = "@Override";
    private static final String SAMPLE_RAW_INTENSITY = "rawIntensity";
    private static final String SAMPLE_STEPS = "steps";
    private static final String SAMPLE_RAW_KIND = "rawKind";
    private static final String SAMPLE_HEART_RATE = "heartRate";
    private static final String TIMESTAMP_FROM = "timestampFrom";
    private static final String TIMESTAMP_TO = "timestampTo";


    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(16, MAIN_PACKAGE + ".entities");

        Entity userAttributes = addUserAttributes(schema);
        Entity user = addUserInfo(schema, userAttributes);

        Entity deviceAttributes = addDeviceAttributes(schema);
        Entity device = addDevice(schema, deviceAttributes);

        // yeah deep shit, has to be here (after device) for db upgrade and column order
        // because addDevice adds a property to deviceAttributes also....
        deviceAttributes.addStringProperty("volatileIdentifier");

        Entity tag = addTag(schema);
        Entity userDefinedActivityOverlay = addActivityDescription(schema, tag, user);

        addMiBandActivitySample(schema, user, device);
        addPebbleHealthActivitySample(schema, user, device);
        addPebbleHealthActivityKindOverlay(schema, user, device);
        addPebbleMisfitActivitySample(schema, user, device);
        addPebbleMorpheuzActivitySample(schema, user, device);
        addHPlusHealthActivityKindOverlay(schema, user, device);
        addHPlusHealthActivitySample(schema, user, device);
        addNo1F1ActivitySample(schema, user, device);

        addCalendarSyncState(schema, device);

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
        device.addIntProperty("type").notNull().javaDocGetterAndSetter("The DeviceType key, i.e. the GBDevice's type.");
        device.addStringProperty("model").javaDocGetterAndSetter("An optional model, further specifying the kind of device-");
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
}
