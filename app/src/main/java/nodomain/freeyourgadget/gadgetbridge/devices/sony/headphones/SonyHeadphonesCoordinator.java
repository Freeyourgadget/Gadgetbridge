/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;

public abstract class SonyHeadphonesCoordinator extends AbstractBLClassicDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Sony";
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new SonyHeadphonesSettingsCustomizer(device);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return false;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return 0;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsAppsManagement() {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public boolean supportsPowerOff() {
        return supports(SonyHeadphonesCapabilities.PowerOffFromPhone);
    }

    @Override
    public int getBatteryCount() {
        if (supports(SonyHeadphonesCapabilities.BatterySingle)) {
            if (supports(SonyHeadphonesCapabilities.BatteryDual)) {
                throw new IllegalStateException("A device can't have both single and dual battery");
            } else if(supports(SonyHeadphonesCapabilities.BatteryCase)) {
                throw new IllegalStateException("Devices with single battery + case are not supported by the protocol");
            }
        }

        int batteryCount = 0;

        if (supports(SonyHeadphonesCapabilities.BatterySingle)) {
            batteryCount += 1;
        }

        if (supports(SonyHeadphonesCapabilities.BatteryCase)) {
            batteryCount += 1;
        }

        if (supports(SonyHeadphonesCapabilities.BatteryDual)) {
            batteryCount += 2;
        }

        return batteryCount;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        final List<Integer> settings = new ArrayList<>();

        if (supports(SonyHeadphonesCapabilities.AmbientSoundControl)) {
            if (supports(SonyHeadphonesCapabilities.WindNoiseReduction)) {
                settings.add(R.xml.devicesettings_sony_headphones_ambient_sound_control_wind_noise_reduction);
            } else {
                settings.add(R.xml.devicesettings_sony_headphones_ambient_sound_control);
            }

            if (supports(SonyHeadphonesCapabilities.AncOptimizer)) {
                settings.add(R.xml.devicesettings_sony_headphones_anc_optimizer);
            }
        }

        addSettingsUnderHeader(settings, R.xml.devicesettings_header_other, new LinkedHashMap<SonyHeadphonesCapabilities, Integer>() {{
            put(SonyHeadphonesCapabilities.AudioSettingsOnlyOnSbcCodec, R.xml.devicesettings_sony_warning_wh1000xm3);
            put(SonyHeadphonesCapabilities.Equalizer, R.xml.devicesettings_sony_headphones_equalizer);
            put(SonyHeadphonesCapabilities.SoundPosition, R.xml.devicesettings_sony_headphones_sound_position);
            put(SonyHeadphonesCapabilities.SurroundMode, R.xml.devicesettings_sony_headphones_surround_mode);
            put(SonyHeadphonesCapabilities.AudioUpsampling, R.xml.devicesettings_sony_headphones_audio_upsampling);
        }});

        addSettingsUnderHeader(settings, R.xml.devicesettings_header_system, new LinkedHashMap<SonyHeadphonesCapabilities, Integer>() {{
            put(SonyHeadphonesCapabilities.ButtonModesLeftRight, R.xml.devicesettings_sony_headphones_button_modes_left_right);
            put(SonyHeadphonesCapabilities.TouchSensorSingle, R.xml.devicesettings_sony_headphones_touch_sensor_single);
            put(SonyHeadphonesCapabilities.PauseWhenTakenOff, R.xml.devicesettings_sony_headphones_pause_when_taken_off);
            put(SonyHeadphonesCapabilities.AutomaticPowerOffWhenTakenOff, R.xml.devicesettings_automatic_power_off_when_taken_off);
            put(SonyHeadphonesCapabilities.AutomaticPowerOffByTime, R.xml.devicesettings_automatic_power_off_by_time);
            put(SonyHeadphonesCapabilities.VoiceNotifications, R.xml.devicesettings_sony_headphones_notifications_voice_guide);
        }});

        settings.add(R.xml.devicesettings_sony_headphones_device_info);

        return ArrayUtils.toPrimitive(settings.toArray(new Integer[0]));
    }

    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return Collections.emptyList();
    }

    public boolean supports(final SonyHeadphonesCapabilities capability) {
        return getCapabilities().contains(capability);
    }

    /**
     * Add the preference screens for capabilities under a header. The header is also only added if at least one capability is supported by the device.
     *
     * @param settings the list of settings to update
     * @param header the header to add, if any capability supported
     * @param capabilities the map of capability to preference screen
     */
    private void addSettingsUnderHeader(final List<Integer> settings,
                                        final int header,
                                        final Map<SonyHeadphonesCapabilities, Integer> capabilities) {
        final Set<SonyHeadphonesCapabilities> supportedCapabilities = new HashSet<>(capabilities.keySet());
        for (SonyHeadphonesCapabilities capability : capabilities.keySet()) {
            if (!supports(capability)) {
                supportedCapabilities.remove(capability);
            }
        }

        if (supportedCapabilities.isEmpty()) {
            // None of the capabilities in the map are supported
            return;
        }

        settings.add(header);

        for (Map.Entry<SonyHeadphonesCapabilities, Integer> capabilitiesSetting : capabilities.entrySet()) {
            if (supports(capabilitiesSetting.getKey())) {
                settings.add(capabilitiesSetting.getValue());
            }
        }
    }
}
