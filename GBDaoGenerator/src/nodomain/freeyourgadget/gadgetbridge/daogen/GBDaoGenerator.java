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

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(7, "nodomain.freeyourgadget.gadgetbridge.entities");

        Entity userAttributes = addUserAttributes(schema);
        Entity user = addUserInfo(schema, userAttributes);

        Entity deviceAttributes = addDeviceAttributes(schema);
        Entity device = addDevice(schema, deviceAttributes);

        addActivitySample(schema, user, device);

        new DaoGenerator().generateAll(schema, "../app/src/main/gen");
    }

    private static Entity addUserInfo(Schema schema, Entity userAttributes) {
        Entity user = schema.addEntity("User");
        user.addIdProperty();
        user.addStringProperty("name").notNull();
        user.addDateProperty("birthday").notNull();
        user.addIntProperty("sex").notNull();
        Property userId = userAttributes.addLongProperty("userId").notNull().getProperty();
        user.addToMany(userAttributes, userId);

        return user;
    }

    private static Entity addUserAttributes(Schema schema) {
        // additional properties of a user, which may change during the lifetime of a user
        // this allows changing attributes while preserving user identity
        Entity userAttributes = schema.addEntity("UserAttributes");
        userAttributes.addIdProperty();
        userAttributes.addIntProperty("heightCM").notNull();
        userAttributes.addIntProperty("weightKG").notNull();
        userAttributes.addIntProperty("sleepGoalHPD");
        userAttributes.addIntProperty("stepsGoalSPD");
        userAttributes.addDateProperty(VALID_FROM_UTC);
        userAttributes.addDateProperty(VALID_TO_UTC);

        return userAttributes;
    }

    private static Entity addDevice(Schema schema, Entity deviceAttributes) {
        Entity device = schema.addEntity("Device");
        device.addIdProperty();
        device.addStringProperty("name").notNull();
        device.addStringProperty("manufacturer").notNull();
        device.addStringProperty("identifier").notNull().javaDocGetterAndSetter("The fixed identifier, i.e. MAC address of the device.");
        Property deviceId = deviceAttributes.addLongProperty("deviceId").notNull().getProperty();
        device.addToMany(deviceAttributes, deviceId);

        return device;
    }

    private static Entity addDeviceAttributes(Schema schema) {
        Entity deviceAttributes = schema.addEntity("DeviceAttributes");
        deviceAttributes.addIdProperty();
        deviceAttributes.addStringProperty("firmwareVersion1").notNull();
        deviceAttributes.addStringProperty("firmwareVersion2");
        deviceAttributes.addDateProperty(VALID_FROM_UTC);
        deviceAttributes.addDateProperty(VALID_TO_UTC);

        return deviceAttributes;
    }

    private static Entity addActivitySample(Schema schema, Entity user, Entity device) {
//        public GBActivitySample(SampleProvider provider, int timestamp, int intensity, int steps, int type, int customValue) {
        Entity activitySample = schema.addEntity("ActivitySample");
        activitySample.addIdProperty();
        activitySample.addIntProperty("timestamp").notNull();
        activitySample.addIntProperty("intensity").notNull();
        activitySample.addIntProperty("steps").notNull();
        activitySample.addIntProperty("type").notNull();
        activitySample.addIntProperty("customValue").notNull();
        Property userId = activitySample.addLongProperty("userId").getProperty();
        activitySample.addToOne(user, userId);
        Property deviceId = activitySample.addLongProperty("deviceId").getProperty();
        activitySample.addToOne(device, deviceId);

        return activitySample;
    }
}
