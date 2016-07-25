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

    public static final String VALID_FROM_UTC = "validFromUTC";
    public static final String VALID_TO_UTC = "validToUTC";
    private static final String MAIN_PACKAGE = "nodomain.freeyourgadget.gadgetbridge";
    private static final String MODEL_PACKAGE = MAIN_PACKAGE + ".model";
    private static final String VALID_BY_DATE = MODEL_PACKAGE + ".ValidByDate";

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(8, MAIN_PACKAGE + ".entities");

        addActivityDescription(schema);

        Entity userAttributes = addUserAttributes(schema);
        Entity user = addUserInfo(schema, userAttributes);

        Entity deviceAttributes = addDeviceAttributes(schema);
        Entity device = addDevice(schema, deviceAttributes);

        addMiBandActivitySample(schema, user, device);
        addPebbleActivitySample(schema, user, device);
        addPebbleMisfitActivitySample(schema, user, device);

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
        userAttributes.addIntProperty("sleepGoalHPD");
        userAttributes.addIntProperty("stepsGoalSPD");
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
//        public GBActivitySample(SampleProvider provider, int timestamp, int intensity, int steps, int type, int customValue) {
        Entity activitySample = addEntity(schema, "MiBandActivitySample");
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        addDefaultActivitySampleAttributes(activitySample);
        addCommonActivitySampleProperties2(activitySample, user, device);
        addHeartRateProperties(activitySample);
        return activitySample;
    }

    private static void addHeartRateProperties(Entity activitySample) {
        activitySample.addImport(MODEL_PACKAGE + ".HeartRateSample");
        activitySample.implementsInterface("HeartRateSample");
        activitySample.addIntProperty("heartRate");
    }

    private static Entity addPebbleActivitySample(Schema schema, Entity user, Entity device) {
//        public GBActivitySample(SampleProvider provider, int timestamp, int intensity, int steps, int type, int customValue) {
        Entity activitySample = addEntity(schema, "PebbleActivitySample");
        addCommonActivitySampleProperties("AbstractActivitySample", activitySample, user, device);
        addDefaultActivitySampleAttributes(activitySample);
        addCommonActivitySampleProperties2(activitySample, user, device);
        return activitySample;
    }

    private static Entity addPebbleMisfitActivitySample(Schema schema, Entity user, Entity device) {
        Entity activitySample = addEntity(schema, "PebbleMisfitSample");
        addCommonActivitySampleProperties("AbstractPebbleMisfitActivitySample", activitySample, user, device);
        activitySample.addIntProperty("rawPebbleMisfitSample").notNull();
        addCommonActivitySampleProperties2(activitySample, user, device);
        return activitySample;
    }

    private static void addCommonActivitySampleProperties(String superClass, Entity activitySample, Entity user, Entity device) {
        activitySample.setSuperclass(superClass);
        activitySample.addImport(MODEL_PACKAGE + ".ActivitySample");
        activitySample.addImport(MAIN_PACKAGE + ".devices.SampleProvider");
        activitySample.implementsInterface("ActivitySample");
        activitySample.setJavaDoc(
                "This class represents a sample specific to the device. Values like activity kind or\n" +
                        "intensity, are device specific. Normalized values can be retrieved through the\n" +
                        "corresponding {@link SampleProvider}.");
        activitySample.addIdProperty();
    }

    private static void addCommonActivitySampleProperties2(Entity activitySample, Entity user, Entity device) {
        Property timestamp = activitySample.addIntProperty("timestamp").notNull().getProperty();
        Property userId = activitySample.addLongProperty("userId").getProperty();
        activitySample.addToOne(user, userId);
        Property deviceId = activitySample.addLongProperty("deviceId").getProperty();
        activitySample.addToOne(device, deviceId);

        Index indexUnique = new Index();
        indexUnique.addProperty(timestamp);
        indexUnique.addProperty(deviceId);
        indexUnique.makeUnique();
        activitySample.addIndex(indexUnique);
    }

    private static void addDefaultActivitySampleAttributes(Entity activitySample) {
        activitySample.addIntProperty("rawIntensity").notNull();
        activitySample.addIntProperty("steps").notNull();
        activitySample.addIntProperty("rawKind").notNull();
    }

    private static Entity addEntity(Schema schema, String className) {
        Entity entity = schema.addEntity(className);
        entity.addImport("de.greenrobot.dao.AbstractDao");
        return entity;
    }
}
