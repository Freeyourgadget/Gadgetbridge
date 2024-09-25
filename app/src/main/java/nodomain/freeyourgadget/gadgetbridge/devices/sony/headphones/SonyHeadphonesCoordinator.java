/*  Copyright (C) 2021-2024 Damien Gaignon, Daniel Dakhno, José Rebelo,
    Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.SonyHeadphonesSupport;

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
    public boolean suggestUnbindBeforePair() {
        return false;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }

    @Override
    public boolean supportsPowerOff() {
        return supports(SonyHeadphonesCapabilities.PowerOffFromPhone);
    }

    @Override
    public int getBatteryCount() {
        if (supports(SonyHeadphonesCapabilities.BatterySingle)) {
            if (supports(SonyHeadphonesCapabilities.BatteryDual) || supports(SonyHeadphonesCapabilities.BatteryDual2)) {
                throw new IllegalStateException("A device can't have both single and dual battery");
            } else if (supports(SonyHeadphonesCapabilities.BatteryCase)) {
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

        if (supports(SonyHeadphonesCapabilities.BatteryDual) || supports(SonyHeadphonesCapabilities.BatteryDual2)) {
            batteryCount += 2;
        }

        return batteryCount;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        final List<BatteryConfig> batteries = new ArrayList<>(3);

        if (supports(SonyHeadphonesCapabilities.BatterySingle)) {
            batteries.add(new BatteryConfig(batteries.size()));
        }

        if (supports(SonyHeadphonesCapabilities.BatteryCase)) {
            batteries.add(new BatteryConfig(batteries.size(), R.drawable.ic_tws_case, R.string.battery_case));
        }

        if (supports(SonyHeadphonesCapabilities.BatteryDual) || supports(SonyHeadphonesCapabilities.BatteryDual2)) {
            batteries.add(new BatteryConfig(batteries.size(), R.drawable.ic_galaxy_buds_l, R.string.left_earbud));
            batteries.add(new BatteryConfig(batteries.size(), R.drawable.ic_galaxy_buds_r, R.string.right_earbud));
        }

        return batteries.toArray(new BatteryConfig[0]);
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        if (supports(SonyHeadphonesCapabilities.AmbientSoundControl) || supports(SonyHeadphonesCapabilities.AmbientSoundControl2)) {
            if (supports(SonyHeadphonesCapabilities.WindNoiseReduction)) {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_ambient_sound_control_wind_noise_reduction);
            } else {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_ambient_sound_control);
            }

            if (supports(SonyHeadphonesCapabilities.AncOptimizer)) {
                deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_anc_optimizer);
            }
        }

        if (supports(SonyHeadphonesCapabilities.AdaptiveVolumeControl)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_adaptive_volume_control);
        }

        if (supports(SonyHeadphonesCapabilities.SpeakToChatConfig)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_speak_to_chat_with_settings);
        } else if (supports(SonyHeadphonesCapabilities.SpeakToChatEnabled)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_speak_to_chat_simple);
        }

        addSettingsUnderHeader(deviceSpecificSettings, R.xml.devicesettings_header_other, new LinkedHashMap<SonyHeadphonesCapabilities, Integer>() {{
            put(SonyHeadphonesCapabilities.AudioSettingsOnlyOnSbcCodec, R.xml.devicesettings_sony_warning_wh1000xm3);
            put(SonyHeadphonesCapabilities.EqualizerSimple, R.xml.devicesettings_sony_headphones_equalizer);
            put(SonyHeadphonesCapabilities.EqualizerWithCustomBands, R.xml.devicesettings_sony_headphones_equalizer_with_custom_bands);
            put(SonyHeadphonesCapabilities.SoundPosition, R.xml.devicesettings_sony_headphones_sound_position);
            put(SonyHeadphonesCapabilities.SurroundMode, R.xml.devicesettings_sony_headphones_surround_mode);
            put(SonyHeadphonesCapabilities.AudioUpsampling, R.xml.devicesettings_sony_headphones_audio_upsampling);
            put(SonyHeadphonesCapabilities.Volume, R.xml.devicesettings_volume);
        }});

        final List<Integer> callsAndNotif = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        callsAndNotif.add(R.xml.devicesettings_headphones);

        addSettingsUnderHeader(deviceSpecificSettings, R.xml.devicesettings_header_system, new LinkedHashMap<SonyHeadphonesCapabilities, Integer>() {{
            put(SonyHeadphonesCapabilities.WideAreaTap, R.xml.devicesettings_sony_headphones_wide_area_tap);
            put(SonyHeadphonesCapabilities.ButtonModesLeftRight, R.xml.devicesettings_sony_headphones_button_modes_left_right);
            put(SonyHeadphonesCapabilities.AmbientSoundControlButtonMode, R.xml.devicesettings_sony_headphones_ambient_sound_control_button_modes);
            put(SonyHeadphonesCapabilities.QuickAccess, R.xml.devicesettings_sony_headphones_quick_access);
            put(SonyHeadphonesCapabilities.TouchSensorSingle, R.xml.devicesettings_sony_headphones_touch_sensor_single);
            put(SonyHeadphonesCapabilities.PauseWhenTakenOff, R.xml.devicesettings_sony_headphones_pause_when_taken_off);
            put(SonyHeadphonesCapabilities.AutomaticPowerOffWhenTakenOff, R.xml.devicesettings_automatic_power_off_when_taken_off);
            put(SonyHeadphonesCapabilities.AutomaticPowerOffByTime, R.xml.devicesettings_automatic_power_off_by_time);
            put(SonyHeadphonesCapabilities.VoiceNotifications, R.xml.devicesettings_sony_headphones_notifications_voice_guide);
        }});

        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_header_developer);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_protocol_version);

        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_sony_headphones_device_info);

        return deviceSpecificSettings;
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
     * @param deviceSpecificSettings the device specific settings
     * @param header                 the header to add, if any capability supported
     * @param capabilities           the map of capability to preference screen
     */
    private void addSettingsUnderHeader(final DeviceSpecificSettings deviceSpecificSettings,
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

        deviceSpecificSettings.addRootScreen(header);

        for (Map.Entry<SonyHeadphonesCapabilities, Integer> capabilitiesSetting : capabilities.entrySet()) {
            if (supports(capabilitiesSetting.getKey())) {
                deviceSpecificSettings.addRootScreen(capabilitiesSetting.getValue());
            }
        }
    }

    public boolean preferServiceV2() {
        return false;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return SonyHeadphonesSupport.class;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_sony_overhead;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_sony_overhead_disabled;
    }
}
