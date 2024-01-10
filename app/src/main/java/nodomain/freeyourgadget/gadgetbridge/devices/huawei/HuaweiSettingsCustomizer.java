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

import android.content.SharedPreferences;
import android.os.Parcel;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Set;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiWorkoutGbParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_DEBUG;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_DEBUG_REQUEST;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_TRUSLEEP;
import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.PREF_HUAWEI_WORKMODE;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO;

public class HuaweiSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    final GBDevice device;
    final HuaweiCoordinator coordinator;

    public HuaweiSettingsCustomizer(final GBDevice device) {
        this.device = device;
        this.coordinator = ((HuaweiCoordinatorSupplier) this.device.getDeviceCoordinator()).getHuaweiCoordinator();
    }

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
        if (preference.getKey().equals(PREF_DO_NOT_DISTURB)) {
            final String dndState = ((ListPreference) preference).getValue();
            final XTimePreference dndStart = handler.findPreference(PREF_DO_NOT_DISTURB_START);
            final XTimePreference dndEnd = handler.findPreference(PREF_DO_NOT_DISTURB_END);
            final SwitchPreferenceCompat dndLifWrist = handler.findPreference(PREF_DO_NOT_DISTURB_LIFT_WRIST);
            final SwitchPreferenceCompat dndNotWear = handler.findPreference(PREF_DO_NOT_DISTURB_NOT_WEAR);
            SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
            boolean statusLiftWrist = sharedPrefs.getBoolean(PREF_LIFTWRIST_NOSHED, false);

            dndStart.setEnabled(false);
            dndEnd.setEnabled(false);
            dndNotWear.setEnabled(false);
            dndLifWrist.setEnabled(false);
            if (dndState.equals("scheduled")) {
                dndStart.setEnabled(true);
                dndEnd.setEnabled(true);
            }
            if (statusLiftWrist && !dndState.equals("off")) {
                dndLifWrist.setEnabled(true);
            }
            if (dndState.equals("off")) {
                dndNotWear.setEnabled(true);
            }
        }
        if (preference.getKey().equals("huawei_reparse_workout_data")) {
            if (((SwitchPreferenceCompat) preference).isChecked()) {
                GB.toast("Starting workout reparse", Toast.LENGTH_SHORT, 0);
                HuaweiWorkoutGbParser.parseAllWorkouts();
                GB.toast("Workout reparse is complete", Toast.LENGTH_SHORT, 0);

                ((SwitchPreferenceCompat) preference).setChecked(false);
            }
        }
        if (preference.getKey().equals(PREF_FORCE_OPTIONS)) {
            final Preference dnd = handler.findPreference("screen_do_not_disturb");
            if (dnd != null) {
                dnd.setVisible(false);
                if (this.coordinator.supportsDoNotDisturb(handler.getDevice()))
                    dnd.setVisible(true);
            }
            final ListPreference wearLocation = handler.findPreference(PREF_WEARLOCATION);
            wearLocation.setVisible(false);
            if (this.coordinator.supportsWearLocation(handler.getDevice())) {
                wearLocation.setVisible(true);
            }
        }
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, Prefs prefs) {

        handler.addPreferenceHandlerFor(PREF_FORCE_OPTIONS);
        handler.addPreferenceHandlerFor(PREF_FORCE_ENABLE_SMART_ALARM);
        handler.addPreferenceHandlerFor(PREF_FORCE_ENABLE_WEAR_LOCATION);

        handler.addPreferenceHandlerFor(PREF_HUAWEI_WORKMODE);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_TRUSLEEP);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_DEBUG);
        handler.addPreferenceHandlerFor(PREF_HUAWEI_DEBUG_REQUEST);

        // Only supported on specific devices
        final ListPreference languageSetting = handler.findPreference(PREF_LANGUAGE);
        if (languageSetting != null) {
            languageSetting.setVisible(false);
            if (this.coordinator.supportsLanguageSetting())
                languageSetting.setVisible(true);
        }

        final Preference dnd = handler.findPreference("screen_do_not_disturb");
        if (dnd != null) {
            dnd.setVisible(false);
            if (this.coordinator.supportsDoNotDisturb(handler.getDevice()))
                dnd.setVisible(true);
        }

        final Preference trusleep = handler.findPreference(PREF_HUAWEI_TRUSLEEP);
        if (trusleep != null) {
            trusleep.setVisible(false);
            if (this.coordinator.supportsTruSleep())
                trusleep.setVisible(true);
        }

//        if (this.coordinator.supportsHeartRate())
//            dynamicSupportedDeviceSpecificSettings.add(R.xml.devicesettings_heartrate_automatic_enable);

        final Preference inactivity = handler.findPreference("screen_inactivity");
        if (inactivity != null) {
            inactivity.setVisible(false);
            if (this.coordinator.supportsInactivityWarnings())
                inactivity.setVisible(true);
        }

        final ListPreference wearLocation = handler.findPreference(PREF_WEARLOCATION);
        if (wearLocation != null) {
            wearLocation.setVisible(false);
            if (this.coordinator.supportsWearLocation(handler.getDevice()))
                wearLocation.setVisible(true);
        }

        final ListPreference date = handler.findPreference(PREF_DATEFORMAT);
        final ListPreference time = handler.findPreference(PREF_TIMEFORMAT);
        if (date != null) {
            date.setVisible(false);
            time.setVisible(false);
            if (this.coordinator.supportsDateFormat()) {
                date.setVisible(true);
                time.setVisible(true);
            }
        }

        final ListPreference workmode = handler.findPreference(PREF_HUAWEI_WORKMODE);
        if (workmode != null) {
            workmode.setVisible(false);
            if (this.coordinator.supportsAutoWorkMode())
                workmode.setVisible(true);
        }

        final SwitchPreferenceCompat liftwirst = handler.findPreference(PREF_LIFTWRIST_NOSHED);
        if (liftwirst != null) {
            liftwirst.setVisible(false);
            if (this.coordinator.supportsActivateOnLift())
                liftwirst.setVisible(true);
        }

        final SwitchPreferenceCompat rotatewirst = handler.findPreference(PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO);
        if (rotatewirst != null) {
            rotatewirst.setVisible(false);
            if (this.coordinator.supportsRotateToCycleInfo())
                rotatewirst.setVisible(true);
        }

        final Preference forceOptions = handler.findPreference(PREF_FORCE_OPTIONS);
        if (forceOptions != null) {
            forceOptions.setVisible(false);
            boolean supportsSmartAlarm = this.coordinator.supportsSmartAlarm();
            boolean supportsWearLocation = this.coordinator.supportsWearLocation();
            if (!supportsSmartAlarm || !supportsWearLocation) {
                forceOptions.setVisible(true);
                final SwitchPreferenceCompat forceSmartAlarm = handler.findPreference(PREF_FORCE_ENABLE_SMART_ALARM);
                forceSmartAlarm.setVisible(false);
                if (!supportsSmartAlarm) {
                    forceSmartAlarm.setVisible(true);
                }
                final SwitchPreferenceCompat forceWearLocation = handler.findPreference(PREF_FORCE_ENABLE_WEAR_LOCATION);
                forceWearLocation.setVisible(false);
                if (!supportsWearLocation) {
                    forceWearLocation.setVisible(true);
                }
            }
        }

        final SwitchPreferenceCompat disconnectNotification = handler.findPreference(PREF_DISCONNECTNOTIF_NOSHED);
        if (disconnectNotification != null) {
            disconnectNotification.setVisible(false);
            if (this.coordinator.supportsNotificationOnBluetoothLoss())
                disconnectNotification.setVisible(true);
        }

        final SwitchPreferenceCompat reparseWorkout = handler.findPreference("huawei_reparse_workout_data");
        if (reparseWorkout != null) {
            reparseWorkout.setVisible(false);
            if (this.coordinator.supportsWorkouts())
                reparseWorkout.setVisible(true);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(device, 0);
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }


    public static final Creator<HuaweiSettingsCustomizer> CREATOR= new Creator<HuaweiSettingsCustomizer>() {

        @Override
        public HuaweiSettingsCustomizer createFromParcel(Parcel parcel) {
            final GBDevice device = parcel.readParcelable(HuaweiSettingsCustomizer.class.getClassLoader());
            return new HuaweiSettingsCustomizer(device);
        }

        @Override
        public HuaweiSettingsCustomizer[] newArray(int i) {
            return new HuaweiSettingsCustomizer[0];
        }
    };
}
