/*
Copyright (C) 2024 enoint

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
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package nodomain.freeyourgadget.gadgetbridge.devices.hama.fit6900;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.hama.fit6900.HamaFit6900DeviceSupport;

public final class HamaFit6900DeviceCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Fit6900");
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 5;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_allow_accept_reject_calls, // reject only
                R.xml.devicesettings_camera_remote,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_liftwrist_display_no_on,
                R.xml.devicesettings_notifications_enable,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_transliteration,
                R.xml.devicesettings_donotdisturb_no_auto,
                R.xml.devicesettings_autoheartrate,
                R.xml.devicesettings_hydration_reminder
        };
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "en_US",
                "es_ES",
                "de_DE",
                "it_IT",
                "fr_FR",
                "sv_SE"
        };
    }

    @Override
    public String getManufacturer() {
        return "Hama";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return HamaFit6900DeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_hama_fit6900;
    }
}
