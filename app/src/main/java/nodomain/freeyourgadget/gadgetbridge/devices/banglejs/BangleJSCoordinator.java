/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Gordon Williams, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs.BangleJSDeviceSupport;

public class BangleJSCoordinator extends AbstractBLEDeviceCoordinator {

    @Override
    public String getManufacturer() {
        return "Espruino";
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        // TODO: filter on name beginning Bangle.js? Doesn't appear to be built-in :(
        // https://developer.android.com/reference/android/bluetooth/le/ScanFilter.Builder.html#setDeviceName(java.lang.String)
        ParcelUuid hpService = new ParcelUuid(BangleJSConstants.UUID_SERVICE_NORDIC_UART);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(hpService).build();
        return Collections.singletonList(filter);
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Bangle\\.js.*|Pixl\\.js.*|Puck\\.js.*|MDBT42Q.*|Espruino.*");
    }

    @Override
    public int getBondingStyle(){
        // Let the user decide whether to bond or not after discovery.
        return BONDING_STYLE_ASK;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public boolean supportsRealtimeData()  {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(GBDevice device) {
        /* we could do this, but the current code for onHeartRateTest
        looks completely broken. There's no way to stop heart rate measurements
        and it doesn't even appear to care what device it's getting the current
        heart rate measurements from. Fixing it is too much work so disabling
        for now.
         */
        return false;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 10;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return BuildConfig.INTERNET_ACCESS;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return BuildConfig.INTERNET_ACCESS ? AppsManagementActivity.class : null;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) {
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new BangleJSSampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        /* we say yes here (because we can't get a handle to our device's prefs to check)
        and then in 'renderUnicodeAsImage' we call EmojiConverter.convertUnicodeEmojiToAscii
        just like DeviceCommunicationService.sanitizeNotifText would have done if we'd
        reported false *if* conversion is disabled */
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        final List<Integer> settings = new ArrayList<>();

        settings.add(R.xml.devicesettings_banglejs_location);

        settings.add(R.xml.devicesettings_header_notifications);
        settings.add(R.xml.devicesettings_text_bitmaps);
        settings.add(R.xml.devicesettings_transliteration);

        settings.add(R.xml.devicesettings_header_calendar);
        settings.add(R.xml.devicesettings_sync_calendar);

        settings.add(R.xml.devicesettings_header_connection);
        settings.add(R.xml.devicesettings_high_mtu);
        if (BuildConfig.INTERNET_ACCESS)
            settings.add(R.xml.devicesettings_device_internet_access);

        settings.add(R.xml.devicesettings_banglejs_activity);

        settings.add(R.xml.devicesettings_header_apps);
        settings.add(R.xml.devicesettings_loyalty_cards);

        settings.add(R.xml.devicesettings_header_developer);
        settings.add(R.xml.devicesettings_banglejs_apploader);
        settings.add(R.xml.devicesettings_device_intents);

        return ArrayUtils.toPrimitive(settings.toArray(new Integer[0]));
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new BangleJSSettingsCustomizer(device);
    }

    @Override
    public boolean supportsNavigation() {
        return true;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return BangleJSDeviceSupport.class;
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_banglejs;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_banglejs;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_banglejs_disabled;
    }
}
