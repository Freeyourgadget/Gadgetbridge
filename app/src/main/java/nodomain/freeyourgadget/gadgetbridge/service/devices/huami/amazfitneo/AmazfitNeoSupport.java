/*  Copyright (C) 2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitneo;

import android.content.Context;
import android.net.Uri;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitneo.AmazfitNeoFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband5.MiBand5Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperation2020;


public class AmazfitNeoSupport extends MiBand5Support {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitNeoSupport.class);

    private boolean heartRateRealtimeStarted = false;
    private boolean heartRateTestStarted = false;
    private byte heartRateRealtimeCount = 0;

    @Override
    protected boolean notificationHasExtraHeader() {
        return false;
    }

    @Override
    protected AmazfitNeoSupport setDisplayItems(TransactionBuilder builder) {
        setDisplayItemsNew(builder, false, false, R.array.pref_neo_display_items_default);
        return this;
    }

    @Override
    protected AmazfitNeoSupport setFitnessGoal(TransactionBuilder builder) {
        LOG.info("Attempting to set Fitness Goal...");
        setNeoFitnessGoal(builder);
        return this;
    }

    @Override
    protected AmazfitNeoSupport setGoalNotification(TransactionBuilder builder) {
        LOG.info("Attempting to set goal notification...");
        setNeoFitnessGoal(builder);
        return this;
    }

    private void setNeoFitnessGoal(TransactionBuilder builder) {
        int fitnessGoal = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_STEPS_GOAL, ActivityUser.defaultUserStepsGoal);
        boolean fitnessGoalNotification = HuamiCoordinator.getGoalNotification(gbDevice.getAddress());
        LOG.info("Setting Amazfit Neo fitness goal to: " + fitnessGoal + ", notification: " + fitnessGoalNotification);
        byte[] bytes = ArrayUtils.addAll(
                new byte[] { 0x3a, 1, 0, 0, 0, (byte) (fitnessGoalNotification ? 1 : 0 ) },
                BLETypeConversions.fromUint16(fitnessGoal));
        bytes = ArrayUtils.addAll(bytes,
                HuamiService.COMMAND_SET_FITNESS_GOAL_END);
        writeToChunked(builder, 2, bytes);
    }

    @Override
    protected AmazfitNeoSupport requestAlarms(TransactionBuilder builder) {
        return this; //Neo always returns response array with '03' in it which marks alarms unused on connect
    }

    @Override
    public boolean supportsHourlyChime() { return true; }

    @Override
    protected AmazfitNeoSupport setHeartrateSleepSupport(TransactionBuilder builder) {
        final boolean enableHrSleepSupport = MiBandCoordinator.getHeartrateSleepSupport(gbDevice.getAddress());
        LOG.info("Setting Amazfit Neo heartrate sleep support to " + enableHrSleepSupport);
        writeToConfiguration(builder, new byte[] {0x06, 0x3c, 0x00, (byte) (enableHrSleepSupport ? 1 : 0 )});
        return this;
    }

    @Override
    public void onHeartRateTest() {
        if (characteristicHRControlPoint == null) {
            return;
        }
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            enableNotifyHeartRateMeasurements(true, builder);
            builder.write(characteristicHRControlPoint, new byte[]{ 0x15, 0x01, 0x01 });
            builder.queue(getQueue());
            heartRateTestStarted = true;
        } catch (IOException ex) {
            LOG.error("Unable to read heart rate from Huami device", ex);
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        heartRateTestStarted = false;
        if (characteristicHRControlPoint == null) {
            return;
        }
        try {
            TransactionBuilder builder = performInitialized("Enable realtime heart rate measurement");
            enableNotifyHeartRateMeasurements(enable, builder);
            if (enable) {
                if (heartRateRealtimeStarted) {
                    if(heartRateRealtimeCount >= 10) {
                        builder.write(characteristicHRControlPoint, new byte[]{ 0x16 }); //send continue every 10 seconds
                        heartRateRealtimeCount = 0;
                    }
                    heartRateRealtimeCount++;
                }
                else {
                    builder.write(characteristicHRControlPoint, new byte[]{ 0x15, 0x01, 0x01 });
                    heartRateRealtimeCount = 10; //sometimes first measurement times out, send first keep alive immediately
                }
                heartRateRealtimeStarted = true;
            } else {
                builder.write(characteristicHRControlPoint, new byte[] { 0x15, 0x01, 0x00 });
                heartRateRealtimeStarted = false;
            }
            builder.queue(getQueue());
            enableRealtimeSamplesTimer(enable);
        } catch (IOException ex) {
            LOG.error("Unable to enable realtime heart rate measurement", ex);
        }
    }

    @Override
    protected void handleHeartrate(byte[] value) {
        super.handleHeartrate(value);
        if (heartRateTestStarted)
            onEnableRealtimeHeartRateMeasurement(false); //stop test after single measurement, disable HR notify
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.name != null)
            if (callSpec.name.length() > 7) //if char 0 is number, +, ( or ) and char 1 is a number it renders in bold scrolling font with letters stripped, else it renders in thin font and displays 'incoming call' if the lengh is > 7
                if (!((Character.isDigit(callSpec.name.charAt(0)) || callSpec.name.charAt(0) == '+' || callSpec.name.charAt(0) == '(' || callSpec.name.charAt(0) == ')') && Character.isDigit(callSpec.name.charAt(1))))
                    callSpec.name = callSpec.name.substring(0, 7);
        super.onSetCallState(callSpec);
    }

    @Override
    protected int getFindDeviceInterval() {
        return 10000; //Neo find command lasts 10 seconds
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitNeoFWHelper(uri, context);
    }

    @Override
    public UpdateFirmwareOperation createUpdateFirmwareOperation(Uri uri) {
        return new UpdateFirmwareOperation2020(uri, this);
    }
}
