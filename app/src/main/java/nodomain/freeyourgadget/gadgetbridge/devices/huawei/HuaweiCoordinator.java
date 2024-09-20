/*  Copyright (C) 2024 Damien Gaignon, Martin.JM, Vitalii Tomin

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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.CameraActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications.NotificationConstraintsType;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Watchface;
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

    private boolean supportsTruSleepNewSync = false;

    private Watchface.WatchfaceDeviceParams watchfaceDeviceParams;

    private App.AppDeviceParams appDeviceParams;

    private final HuaweiCoordinatorSupplier parent;

    private boolean transactionCrypted=true;

    private int maxContactsCount = 0;

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
                                            GB.hexdump(Notifications.defaultConstraints)
                    )));
                if (key.equals("maxContactsCount"))
                    this.maxContactsCount = getCapabilitiesSharedPreferences().getInt(key, 0);

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

    public void saveMaxContactsCount(int maxContactsCount) {
        this.maxContactsCount = maxContactsCount;
        getCapabilitiesSharedPreferences().edit().putInt("maxContactsCount", maxContactsCount).apply();
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
            msg.append("ServiceID: ").append(Integer.toHexString(entry.getKey())).append(" => Commands: ");
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
        if (expandCapabilities == null) {
            LOG.debug("Expand capabilities is null");
            return false;
        }
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
        return (int)notificationConstraints.getShort(which);
    }

    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        // Health
        if (supportsInactivityWarnings())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_inactivity_sheduled);
        if (supportsTruSleep())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_trusleep);
        if (supportsHeartRate())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_heartrate_automatic_enable);
        if (supportsSPo2())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_spo_automatic_enable);
        if(supportsTemperature()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH, R.xml.devicesettings_temperature_automatic_enable);
        }

        // Notifications
        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.NOTIFICATIONS);
        notifications.add(R.xml.devicesettings_notifications_enable);
        if (supportsNotificationOnBluetoothLoss())
            notifications.add(R.xml.devicesettings_disconnectnotification_noshed);
        if (supportsDoNotDisturb(device))
            notifications.add(R.xml.devicesettings_donotdisturb_allday_liftwirst_notwear);

        // Workout
        if (supportsSendingGps())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.WORKOUT, R.xml.devicesettings_workout_send_gps_to_band);

        // Other
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_find_phone);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_disable_find_phone_with_dnd);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_allow_accept_reject_calls);

        // Camera control
        if (supportsCameraRemote())
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_camera_remote);

        //Contacts
        if (getContactsSlotCount(device) > 0) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_contacts);
        }

        // Time
        if (supportsDateFormat()) {
            final List<Integer> dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
            dateTime.add(R.xml.devicesettings_dateformat);
            dateTime.add(R.xml.devicesettings_timeformat);
        }

        // Display
        if (supportsWearLocation(device))
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_wearlocation);
        if (supportsAutoWorkMode())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_workmode);
        if (supportsActivateOnLift())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_liftwrist_display_noshed);
        if (supportsRotateToCycleInfo())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_rotatewrist_cycleinfo);
        // Currently on main setting menu.
        /*if (supportsLanguageSetting())
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_language_generic);*/
        if(supportsTemperature()) {
            deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY, R.xml.devicesettings_temperature_scale_cf);
        }

        // Developer
        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_huawei_debug);

        return deviceSpecificSettings;
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

    public boolean supportsCameraRemote() {
        return supportsCommandForService(0x01, 0x29) && CameraActivity.supportsCamera();
    }

    public boolean supportsContacts() {
        return supportsCommandForService(0x03, 0x1);
    }

    public boolean supportsAcceptAgreement() {
        return supportsCommandForService(0x01, 0x30);
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

    public boolean supportsDeviceReportThreshold() {
        return supportsCommandForService(0x07, 0x0e);
    }

    public boolean supportsTruSleep() {
        return supportsCommandForService(0x07, 0x16);
    }

    public boolean supportsHeartRate() {
        return supportsCommandForService(0x07, 0x17);
    }

    public boolean supportsHeartRate(GBDevice gbDevice) {
        return supportsHeartRate() || getForceOption(gbDevice, PREF_FORCE_ENABLE_HEARTRATE_SUPPORT);
    }

    public boolean supportsFitnessRestHeartRate() {
        return supportsCommandForService(0x07, 0x23);
    }

    public boolean supportsSPo2() {
        return supportsCommandForService(0x07, 0x24);
    }

    public boolean supportsSPo2(GBDevice gbDevice) {
        return supportsSPo2() || getForceOption(gbDevice, PREF_FORCE_ENABLE_SPO2_SUPPORT);
    }

    public boolean supportsRunPaceConfig() {
        return supportsCommandForService(0x07, 0x28);
    }

    public boolean supportsFitnessThresholdValue() {
        return supportsCommandForService(0x07, 0x29);
    }
    public boolean supportsFitnessThresholdValueV2() { return supportsExpandCapability(0x9a) || supportsExpandCapability(0x9c); }

    // 0x1d - SupportTemperature
    // 0xba - SupportTemperatureClassification
    // 0x43 - SupportTemperatureStudy
    public boolean supportsTemperature() { return supportsExpandCapability(0x1d); }


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

    public boolean supportsWatchfaceParams(){ return supportsCommandForService(0x27, 0x01);}

    public boolean supportsAppParams(){ return supportsCommandForService(0x2a, 0x06);}

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

    public boolean supportsWeatherUvIndex() {
        return supportsExpandCapability(0x2f);
    }

    public boolean supportsWorkouts() {
        return supportsCommandForService(0x17, 0x01);
    }

    public boolean supportsWorkoutsTrustHeartRate() {
        return supportsCommandForService(0x17, 0x17);
    }

    public boolean supportsSendingGps() {
        return supportsCommandForService(0x18, 0x02);
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

    public int getContentFormat() {
        return getNotificationConstraint(NotificationConstraintsType.contentFormat);
    }

    public int getYellowPagesFormat() {
        return getNotificationConstraint(NotificationConstraintsType.yellowPagesFormat);
    }

    public int getContentSignFormat() {
        return getNotificationConstraint(NotificationConstraintsType.contentSignFormat);
    }

    public int getIncomingFormatFormat() {
        return getNotificationConstraint(NotificationConstraintsType.incomingNumberFormat);
    }

    public int getContentLength() {
        return getNotificationConstraint(NotificationConstraintsType.contentLength);
    }

    public int getYellowPagesLength() {
        return getNotificationConstraint(NotificationConstraintsType.yellowPagesLength);
    }

    public int getContentSignLength() {
        return getNotificationConstraint(NotificationConstraintsType.contentSignLength);
    }

    public int getIncomingNumberLength() {
        return getNotificationConstraint(NotificationConstraintsType.incomingNumberLength);
    }

    public int getAlarmSlotCount(GBDevice gbDevice) {
        int alarmCount = 0;
        if (supportsEventAlarm())
            alarmCount += 5; // Always five event alarms
        if (supportsSmartAlarm(gbDevice))
            alarmCount += 1; // Always a single smart alarm
        return alarmCount;
    }

    public int getContactsSlotCount(GBDevice device) {
        return supportsContacts()?maxContactsCount:0;
    }

    public void setTransactionCrypted(boolean crypted) {
        this.transactionCrypted = crypted;
    }

    public boolean isTransactionCrypted() {
        return this.transactionCrypted;
    }

    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "cs_CZ",
                "de_DE",
                "en_US",
                "es_ES",
                "fr_FR",
                "it_IT",
                "pt_BR",
                "ru_RU",
                "tr_TR",
                "zh_CN",
                "zh_TW",
        };

    }

    public short getWidth() {
        return (short)watchfaceDeviceParams.width;
    }

    public short getHeight() {
        return (short)watchfaceDeviceParams.height;
    }

    public void setWatchfaceDeviceParams(Watchface.WatchfaceDeviceParams watchfaceDeviceParams) {
        this.watchfaceDeviceParams = watchfaceDeviceParams;
    }

    public void setAppDeviceParams(App.AppDeviceParams appDeviceParams) {
        this.appDeviceParams = appDeviceParams;
    }

    public App.AppDeviceParams getAppDeviceParams() {
        return appDeviceParams;
    }

    public Class<? extends Activity> getAppManagerActivity() {
        return AppManagerActivity.class;
    }

    public boolean getSupportsAppListFetching() { return true; }

    public boolean getSupportsAppsManagement(GBDevice device) {
        return true;
    }

    public boolean getSupportsInstalledAppManagement(GBDevice device) {
        return this.supportsAppParams(); // NOTE: this check can be incorrect. But looks like it works
    }

    public boolean getSupportsCachedAppManagement(GBDevice device) {
        return false;
    }

    public InstallHandler getInstallHandler(Uri uri, Context context) {
        HuaweiInstallHandler handler = new HuaweiInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    public boolean getSupportsTruSleepNewSync() {
        return supportsTruSleepNewSync;
    }

    public void setSupportsTruSleepNewSync(boolean supportsTruSleepNewSync) {
        this.supportsTruSleepNewSync = supportsTruSleepNewSync;
    }
}
