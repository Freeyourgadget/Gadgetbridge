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
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

/**
 * Generates entities and DAOs for the example project DaoExample.
 * Automatically run during build.
 */
public class GBDaoGenerator {

    public static final String VALID_FROM_UTC = "validFromUTC";
    public static final String VALID_TO_UTC = "validToUTC";
    private static final String MAIN_PACKAGE = "nodomain.freeyourgadget.gadgetbridge";
    private static final String MODEL_PACKAGE = MAIN_PACKAGE + ".model";
    private static final String VALID_BY_DATE = MODEL_PACKAGE + ".ValidByDate";

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(13, MAIN_PACKAGE + ".entities");

        addActivityDescription(schema);

        Entity userAttributes = addUserAttributes(schema);
        Entity user = addUserInfo(schema, userAttributes);

        Entity deviceAttributes = addDeviceAttributes(schema);
        Entity device = addDevice(schema, deviceAttributes);

        addMiBandActivitySample(schema, user, device);
        addPebbleHealthActivitySample(schema, user, device);
        addPebbleHealthActivityKindOverlay(schema, user, device);
        addPebbleMisfitActivitySample(schema, user, device);
        addPebbleMorpheuzActivitySample(schema, user, device);

        new DaoGenerator().generateAll(schema, "app/src/main/java");
    }

    private static Entity addActivityDescription(Schema schema) {
        Entity activityDescription = addEntity(schema, "ActivityDescription");
        activityDescription.addIdProperty();
        activityDescription.addIntProperty("fromTimestamp").notNull();
        activityDescription.addIntProperty("toTimestamp");
        return activityDescription;
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
        entity.addDateProperty(VALID_FROM_UTC);
        entity.addDateProperty(VALID_TO_UTC);

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
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        activitySample.addIntProperty("rawIntensity").notNull();
        activitySample.addIntProperty("steps").notNull();
        activitySample.addIntProperty("rawKind").notNull();
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static void addHeartRateProperties(Entity activitySample) {
        activitySample.addIntProperty("heartRate").notNull();
    }

    private static Entity addPebbleHealthActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PebbleHealthActivitySample");
        addCommonActivitySampleProperties("AbstractPebbleHealthActivitySample", activitySample, user, device);
        activitySample.addByteArrayProperty("rawPebbleHealthData");
        activitySample.addIntProperty("rawIntensity").notNull();
        activitySample.addIntProperty("steps").notNull();
        return activitySample;
    }

    private static Entity addPebbleHealthActivityKindOverlay(Schema schema, Entity user, Entity device) {
        Entity activityOverlay = addEntity(schema, "PebbleHealthActivityOverlay");

        activityOverlay.addIntProperty("timestampFrom").notNull().primaryKey();
        activityOverlay.addIntProperty("timestampTo").notNull().primaryKey();
        activityOverlay.addIntProperty("rawKind").notNull().primaryKey();
        Property deviceId = activityOverlay.addLongProperty("deviceId").primaryKey().getProperty();
        activityOverlay.addToOne(device, deviceId);

        Property userId = activityOverlay.addLongProperty("userId").getProperty();
        activityOverlay.addToOne(user, userId);
        activityOverlay.addByteArrayProperty("rawPebbleHealthData");

        return activityOverlay;
    }

    private static Entity addPebbleMisfitActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PebbleMisfitSample");
        addCommonActivitySampleProperties("AbstractPebbleMisfitActivitySample", activitySample, user, device);
        activitySample.addIntProperty("rawPebbleMisfitSample").notNull();
        return activitySample;
    }

    private static Entity addPebbleMorpheuzActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PebbleMorpheuzSample");
        addCommonActivitySampleProperties("AbstractPebbleMorpheuzActivitySample", activitySample, user, device);
        activitySample.addIntProperty("rawIntensity").notNull();
        return activitySample;
    }

    private static void addCommonActivitySampleProperties(String superClass, Entity activitySample, Entity user, Entity device) {
        activitySample.setSuperclass(superClass);
        activitySample.addImport(MAIN_PACKAGE + ".devices.SampleProvider");
        activitySample.setJavaDoc(
                "This class represents a sample specific to the device. Values like activity kind or\n" +
                        "intensity, are device specific. Normalized values can be retrieved through the\n" +
                        "corresponding {@link SampleProvider}.");
        activitySample.addIntProperty("timestamp").notNull().primaryKey();
        Property deviceId = activitySample.addLongProperty("deviceId").primaryKey().getProperty();
        activitySample.addToOne(device, deviceId);
        Property userId = activitySample.addLongProperty("userId").getProperty();
        activitySample.addToOne(user, userId);
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
