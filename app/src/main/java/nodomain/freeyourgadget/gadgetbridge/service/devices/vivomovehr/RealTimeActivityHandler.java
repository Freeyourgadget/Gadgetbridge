/*  Copyright (C) 2020-2023 Petr Kadlec

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveHrSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.VivomoveHrActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/* default */ class RealTimeActivityHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RealTimeActivityHandler.class);

    private final VivomoveHrSupport owner;
    private final VivomoveHrActivitySample lastSample = new VivomoveHrActivitySample();

    /* default */ RealTimeActivityHandler(VivomoveHrSupport owner) {
        this.owner = owner;
    }

    public boolean tryHandleChangedCharacteristic(UUID characteristicUUID, byte[] data) {
        if (VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_HEART_RATE.equals(characteristicUUID)) {
            processRealtimeHeartRate(data);
            return true;
        }
        if (VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_STEPS.equals(characteristicUUID)) {
            processRealtimeSteps(data);
            return true;
        }
        if (VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_CALORIES.equals(characteristicUUID)) {
            processRealtimeCalories(data);
            return true;
        }
        if (VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_STAIRS.equals(characteristicUUID)) {
            processRealtimeStairs(data);
            return true;
        }
        if (VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_INTENSITY.equals(characteristicUUID)) {
            processRealtimeIntensityMinutes(data);
            return true;
        }
        if (VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_HEART_RATE_VARIATION.equals(characteristicUUID)) {
            handleRealtimeHeartbeat(data);
            return true;
        }

        return false;
    }

    private void processRealtimeHeartRate(byte[] data) {
        int unknown1 = BLETypeConversions.toUnsigned(data, 0);
        int heartRate = BLETypeConversions.toUnsigned(data, 1);
        int unknown2 = BLETypeConversions.toUnsigned(data, 2);
        int unknown3 = BLETypeConversions.toUint16(data, 3);

        lastSample.setHeartRate(heartRate);
        processSample();

        LOG.debug("Realtime HR {} ({}, {}, {})", heartRate, unknown1, unknown2, unknown3);
    }

    private void processRealtimeSteps(byte[] data) {
        int steps = BLETypeConversions.toUint32(data, 0);
        int goal = BLETypeConversions.toUint32(data, 4);

        lastSample.setSteps(steps);
        processSample();

        LOG.debug("Realtime steps: {} steps (goal: {})", steps, goal);
    }

    private void processRealtimeCalories(byte[] data) {
        int calories = BLETypeConversions.toUint32(data, 0);
        int unknown = BLETypeConversions.toUint32(data, 4);

        lastSample.setCaloriesBurnt(calories);
        processSample();

        LOG.debug("Realtime calories: {} cal burned (unknown: {})", calories, unknown);
    }

    private void processRealtimeStairs(byte[] data) {
        int floorsClimbed = BLETypeConversions.toUint16(data, 0);
        int unknown = BLETypeConversions.toUint16(data, 2);
        int floorGoal = BLETypeConversions.toUint16(data, 4);

        lastSample.setFloorsClimbed(floorsClimbed);
        processSample();

        LOG.debug("Realtime stairs: {} floors climbed (goal: {}, unknown: {})", floorsClimbed, floorGoal, unknown);
    }

    private void processRealtimeIntensityMinutes(byte[] data) {
        int weeklyLimit = BLETypeConversions.toUint32(data, 10);

        LOG.debug("Realtime intensity recorded; weekly limit: {}", weeklyLimit);
    }

    private void handleRealtimeHeartbeat(byte[] data) {
        int interval = BLETypeConversions.toUint16(data, 0);
        int timer = BLETypeConversions.toUint32(data, 2);

        float heartRate = (60.0f * 1024.0f) / interval;
        LOG.debug("Realtime heartbeat frequency {} at {}", heartRate, timer);
    }

    private void processSample() {
        if (lastSample.getCaloriesBurnt() == null || lastSample.getFloorsClimbed() == null || lastSample.getHeartRate() == 0 || lastSample.getSteps() == 0) {
            LOG.debug("Skipping incomplete sample");
            return;
        }

        try (final DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();

            final GBDevice gbDevice = owner.getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);
            final int ts = (int) (System.currentTimeMillis() / 1000);
            final VivomoveHrSampleProvider provider = new VivomoveHrSampleProvider(gbDevice, session);
            final VivomoveHrActivitySample sample = createActivitySample(device, user, ts, provider);

            sample.setCaloriesBurnt(lastSample.getCaloriesBurnt());
            sample.setFloorsClimbed(lastSample.getFloorsClimbed());
            sample.setHeartRate(lastSample.getHeartRate());
            sample.setSteps(lastSample.getSteps());
            sample.setRawIntensity(ActivitySample.NOT_MEASURED);
            sample.setRawKind(ActivityKind.TYPE_ACTIVITY); // to make it visible in the charts TODO: add a MANUAL kind for that?

            LOG.debug("Publishing sample");
            provider.addGBActivitySample(sample);
        } catch (Exception e) {
            LOG.error("Error saving real-time activity data", e);
        }

        final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, lastSample);
        LocalBroadcastManager.getInstance(owner.getContext()).sendBroadcast(intent);
    }

    public VivomoveHrActivitySample createActivitySample(Device device, User user, int timestampInSeconds, VivomoveHrSampleProvider provider) {
        final VivomoveHrActivitySample sample = new VivomoveHrActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);

        return sample;
    }
}
