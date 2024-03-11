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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications.NotificationConstraintsType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

public class HuaweiCoordinator {
    Logger LOG = LoggerFactory.getLogger(HuaweiCoordinator.class);

    TreeMap<Integer, byte[]> commandsPerService = new TreeMap<>();
    // Each byte of expandCapabilities represent a "service"
    // Each bit in a "service" represent a feature so 1 or 0 is used to check is support or not
    byte[] expandCapabilities = null;
    byte notificationCapabilities = -0x01;
    ByteBuffer notificationConstraints = null;

    private final HuaweiCoordinatorSupplier parent;
    private boolean transactionCrypted=true;

    public HuaweiCoordinator(HuaweiCoordinatorSupplier parent) {
        this.parent = parent;
        for (String key : getCapabilitiesSharedPreferences().getAll().keySet()) {
            int service;
            try {
                service = Integer.parseInt(key);
                byte[] commands = GB.hexStringToByteArray(getCapabilitiesSharedPreferences().getString(key, "00"));
                this.commandsPerService.put(service, commands);
            } catch (NumberFormatException e) {
                if (key.equals("expandCapabilities"))
                    this.expandCapabilities = GB.hexStringToByteArray(getCapabilitiesSharedPreferences().getString(key, "00"));
                if (key.equals("notificationCapabilities"))
                    this.notificationCapabilities = (byte)getCapabilitiesSharedPreferences().getInt(key, -0x01);
                if (key.equals("notificationConstraints"))
                    this.notificationConstraints = ByteBuffer.wrap(GB.hexStringToByteArray(
                                    getCapabilitiesSharedPreferences().getString(
                                            key,
                                            "00F00002001E0002001E0002001E")
                    ));
            }
        }
    }

    private SharedPreferences getCapabilitiesSharedPreferences() {
        return GBApplication.getContext().getSharedPreferences("huawei_coordinator_capatilities" + parent.getDeviceType().name(), Context.MODE_PRIVATE);
    }

    private SharedPreferences getDeviceSpecificSharedPreferences(GBDevice gbDevice) {
        return GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
    }

    public boolean getForceOption(GBDevice gbDevice, String option) {
        return getDeviceSpecificSharedPreferences(gbDevice).getBoolean(option, false);
    }

    private void saveCommandsForService(int service, byte[] commands) {
        commandsPerService.put(service, commands);
        getCapabilitiesSharedPreferences().edit().putString(String.valueOf(service), GB.hexdump(commands)).apply();
    }

    public void saveExpandCapabilities(byte[] capabilities) {
        expandCapabilities = capabilities;
        getCapabilitiesSharedPreferences().edit().putString("expandCapabilities", GB.hexdump(capabilities)).apply();
    }

    public void saveNotificationCapabilities(byte capabilities) {
        notificationCapabilities = capabilities;
        getCapabilitiesSharedPreferences().edit().putInt("notificationCapabilities", (int)capabilities).apply();
    }

    public void saveNotificationConstraints(ByteBuffer constraints) {
        notificationConstraints = constraints;
        getCapabilitiesSharedPreferences().edit().putString("notificationConstraints", GB.hexdump(constraints.array())).apply();
    }

    public void addCommandsForService(int service, byte[] commands) {
        if (!commandsPerService.containsKey(service)) {
            saveCommandsForService(service, commands);
            return;
        }
        byte[] saved = commandsPerService.get(service);
        if (saved == null) {
            saveCommandsForService(service, commands);
            return;
        }
        if (saved.length != commands.length) {
            saveCommandsForService(service, commands);
            return;
        }
        boolean changed = false;
        for (int i = 0; i < saved.length; i++) {
            if (saved[i] != commands[i]) {
                changed = true;
                break;
            }
        }
        if (changed)
            saveCommandsForService(service, commands);
    }

    public byte[] getCommandsForService(int service) {
        return commandsPerService.get(service);
    }

    // Print all Services ID and Commands ID
    public void printCommandsPerService() {
        StringBuilder msg = new StringBuilder();
        for(Map.Entry<Integer, byte[]> entry : commandsPerService.entrySet()) {
            msg.append("ServiceID: ").append(entry.getKey()).append(" => Commands: ");
            for (byte b: entry.getValue()) {
                msg.append(Integer.toHexString(b)).append(" ");
            }
            msg.append("\n");
        }
        LOG.info(msg.toString());
    }

    private boolean supportsCommandForService(int service, int command) {
        byte[] commands = commandsPerService.get(service);
        if (commands == null)
            return false;
        for (byte b : commands)
            if (b == (byte) command)
                return true;
        return false;
    }

    private boolean supportsExpandCapability(int which) {
        // capability is a number containing :
        //  - the index of the "service"
        //  - the real capability number
        if (which >= expandCapabilities.length * 8) {
            LOG.debug("Capability is not supported");
            return false;
        }
        int capability = 1 << (which % 8);
        if ((expandCapabilities[which / 8] & capability) == capability) return true;
        return false;
    }

    private boolean supportsNotificationConstraint(byte which) {
        return notificationConstraints.get(which) == 0x01;
    }

    private int getNotificationConstraint(byte which) {
        return notificationConstraints.get(which);
    }

    public int[] genericHuaweiSupportedDeviceSpecificSettings(int[] additionalDeviceSpecificSettings) {
        // Add all settings in the default table
        // Hide / show table in HuaweiSettingsCustommizer
        List<Integer> dynamicSupportedDeviceSpecificSettings = new ArrayList<>();

        // Could be limited to 0x04 0x01, but I don't know if that'll work properly
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_allow_accept_reject_calls);

        // Only supported on specific devices
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_huawei);
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_trusleep);
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_wearlocation);
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_dateformat);
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_timeformat);
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_workmode);
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_liftwrist_display_noshed);
        dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_rotatewrist_cycleinfo);

        int size = dynamicSupportedDeviceSpecificSettings.size();
        if (additionalDeviceSpecificSettings != null)
            size += additionalDeviceSpecificSettings.length;
        int[] result = new int[size];

        for (int i = 0; i < dynamicSupportedDeviceSpecificSettings.size(); i++)
            result[i] = dynamicSupportedDeviceSpecificSettings.get(i);

        if (additionalDeviceSpecificSettings != null)
            System.arraycopy(additionalDeviceSpecificSettings, 0, result, dynamicSupportedDeviceSpecificSettings.size(), additionalDeviceSpecificSettings.length);

        return result;
    }

    public boolean supportsDateFormat() {
        return supportsCommandForService(0x01, 0x04);
    }

    public boolean supportsActivateOnLift() {
        return supportsCommandForService(0x01, 0x09);
    }

    public boolean supportsDoNotDisturb() {
        return supportsCommandForService(0x01, 0x0a);
    }
    public boolean supportsDoNotDisturb(GBDevice gbDevice) {
        return supportsDoNotDisturb() || getForceOption(gbDevice, PREF_FORCE_DND_SUPPORT);
    }

    public boolean supportsActivityType() {
        return supportsCommandForService(0x01, 0x12);
    }

    public boolean supportsWearLocation() {
        return supportsCommandForService(0x01, 0x1a);
    }
    public boolean supportsWearLocation(GBDevice gbDevice) {
        return supportsWearLocation() || getForceOption(gbDevice, PREF_FORCE_ENABLE_WEAR_LOCATION);
    }

    public boolean supportsRotateToCycleInfo() {
        return supportsCommandForService(0x01, 0x1b);
    }

    public boolean supportsQueryDndLiftWristDisturbType() {
        return supportsCommandForService(0x01, 0x1d);
    }

    public boolean supportsSettingRelated() {
        return supportsCommandForService(0x01, 0x31);
    }

    public boolean supportsTimeAndZoneId() {
        return supportsCommandForService(0x01, 0x32);
    }

    public boolean supportsConnectStatus() {
        return supportsCommandForService(0x01, 0x35);
    }

    public boolean supportsExpandCapability() {
        return supportsCommandForService(0x01, 0x37);
    }

    public boolean supportsNotificationAlert() {
        return supportsCommandForService(0x02, 0x01);
    }

    public boolean supportsNotification() {
        return supportsCommandForService(0x02, 0x04);
    }

    public boolean supportsWearMessagePush() {
        return supportsCommandForService(0x02, 0x08);
    }


    public boolean supportsMotionGoal() {
        return supportsCommandForService(0x07, 0x01);
    }
    
    public boolean supportsInactivityWarnings() {
        return supportsCommandForService(0x07, 0x06);
    }

    public boolean supportsActivityReminder() {
        return supportsCommandForService(0x07, 0x07);
    }

    public boolean supportsTruSleep() {
        return supportsCommandForService(0x07, 0x16);
    }

    public boolean supportsHeartRate() {
        // TODO: this is not correct
        return supportsCommandForService(0x07, 0x17);
    }

    public boolean supportsFitnessRestHeartRate() {
        return supportsCommandForService(0x07, 0x23);
    }

    public boolean supportsFitnessThresholdValue() {
        return supportsCommandForService(0x07, 0x29);
    }

    public boolean supportsEventAlarm() {
        return supportsCommandForService(0x08, 0x01);
    }

    public boolean supportsSmartAlarm() {
        return supportsCommandForService(0x08, 0x02) ;
    }

    public boolean supportsSmartAlarm(GBDevice gbDevice) {
        return supportsSmartAlarm() || getForceOption(gbDevice, PREF_FORCE_ENABLE_SMART_ALARM);
    }

    public boolean supportsSmartAlarm(GBDevice gbDevice, int alarmPosition) {
        return supportsSmartAlarm(gbDevice) && alarmPosition == 0;
    }

    public boolean forcedSmartWakeup(GBDevice device, int alarmPosition) {
        return supportsSmartAlarm(device, alarmPosition) && alarmPosition == 0;
    }

    /**
     * @return True if alarms can be changed on the device, false otherwise
     */
    public boolean supportsChangingAlarm() {
        return supportsCommandForService(0x08, 0x03);
    }

    public boolean supportsNotificationOnBluetoothLoss() {
        return supportsCommandForService(0x0b, 0x03);
    }

    public boolean supportsLanguageSetting() {
        return supportsCommandForService(0x0c, 0x01);
    }

    public boolean supportsWeather() {
        return supportsCommandForService(0x0f, 0x01);
    }

    public boolean supportsWeatherUnit() {
        return supportsCommandForService(0x0f, 0x05);
    }

    public boolean supportsWeatherExtended() {
        return supportsCommandForService(0x0f, 0x06);
    }

    public boolean supportsWeatherForecasts() {
        return supportsCommandForService(0x0f, 0x08);
    }

    public boolean supportsWeatherMoonRiseSet() {
        return supportsCommandForService(0x0f, 0x0a);
    }

    public boolean supportsWeatherTides() {
        return supportsCommandForService(0x0f, 0x0b);
    }

    public boolean supportsWorkouts() {
        return supportsCommandForService(0x17, 0x01);
    }

    public boolean supportsWorkoutsTrustHeartRate() {
        return supportsCommandForService(0x17, 0x17);
    }

    public boolean supportsAccount() {
        return supportsCommandForService(0x1A, 0x01);
    }

    public boolean supportsAccountJudgment() {
        return supportsCommandForService(0x1A, 0x05);
    }

    public boolean supportsAccountSwitch() {
        return supportsCommandForService(0x1A, 0x06);
    }

    public boolean supportsDiffAccountPairingOptimization() {
        if (supportsExpandCapability())
            return supportsExpandCapability(0xac);
        return false;
    }

    public boolean supportsMusic() {
        return supportsCommandForService(0x25, 0x02);
    }

    public boolean supportsAutoWorkMode() {
        return supportsCommandForService(0x26, 0x02);
    }

    public boolean supportsMenstrual() {
        return supportsCommandForService(0x32, 0x01);
    }

    public boolean supportsMultiDevice() {
        if (supportsExpandCapability())
            return supportsExpandCapability(109);
        return false;
    }

    public boolean supportsPromptPushMessage () {
//              do not ask for capabilities under specific condition
//                  if (deviceType == 10 && deviceVersion == 73617766697368 && deviceSoftVersion == 372E312E31) -> leo device
//                  if V1V0Device
//                  if (serviceId = 0x01 && commandId = 0x03) && productType == 3
        return (((notificationCapabilities >> 1) & 1) == 0);
    }

    public boolean supportsOutgoingCall () {
        return (((notificationCapabilities >> 2) & 1) == 0);
    }

    public boolean supportsYellowPages() {
        return supportsNotificationConstraint(NotificationConstraintsType.yellowPagesSupport);
    }

    public boolean supportsContentSIgn() {
        return supportsNotificationConstraint(NotificationConstraintsType.contentSignSupport);
    }

    public boolean supportsIncomingNumber() {
        return supportsNotificationConstraint(NotificationConstraintsType.incomingNumberSupport);
    }

    public byte getYellowPagesFormat() {
        return (byte)getNotificationConstraint(NotificationConstraintsType.yellowPagesFormat);
    }

    public byte getContentSignFormat() {
        return (byte)getNotificationConstraint(NotificationConstraintsType.contentSignFormat);
    }

    public byte getIncomingFormatFormat() {
        return (byte)getNotificationConstraint(NotificationConstraintsType.incomingNumberFormat);
    }

    public short getContentLength() {
        return (short)getNotificationConstraint(NotificationConstraintsType.contentLength);
    }

    public short getYellowPagesLength() {
        return (short)getNotificationConstraint(NotificationConstraintsType.yellowPagesLength);
    }

    public short getContentSignLength() {
        return (short)getNotificationConstraint(NotificationConstraintsType.contentSignLength);
    }

    public short getIncomingNumberLength() {
        return (short)getNotificationConstraint(NotificationConstraintsType.incomingNumberLength);
    }

    public int getAlarmSlotCount(GBDevice gbDevice) {
        int alarmCount = 0;
        if (supportsEventAlarm())
            alarmCount += 5; // Always five event alarms
        if (supportsSmartAlarm(gbDevice))
            alarmCount += 1; // Always a single smart alarm
        return alarmCount;
    }

    public void setTransactionCrypted(boolean crypted) {
        this.transactionCrypted = crypted;
    }

    public boolean isTransactionCrypted() {
        return this.transactionCrypted;
    }
}
