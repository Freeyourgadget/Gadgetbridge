/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, HardLight, José Rebelo, odavo32nof

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitx;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitx.AmazfitXFWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class AmazfitXCoordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitXCoordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.AMAZFITX;
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        try {
            BluetoothDevice device = candidate.getDevice();
            String name = device.getName();
            if (name != null && name.equalsIgnoreCase(HuamiConst.AMAZFIT_X)) {
                return DeviceType.AMAZFITX;
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return DeviceType.UNKNOWN;

    }


    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        // TOOD Need to be checked first
        AmazfitXFWInstallHandler handler = new AmazfitXFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
//        return null;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_amazfitx,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_custom_emoji_font,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_dateformat,
                R.xml.devicesettings_nightmode,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_swipeunlock,
                R.xml.devicesettings_sync_calendar,
                R.xml.devicesettings_expose_hr_thirdparty,
                R.xml.devicesettings_bt_connected_advertisement,
                R.xml.devicesettings_device_actions,
                R.xml.devicesettings_pairingkey,
                R.xml.devicesettings_high_mtu
        };
    }

    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "en_US",
                "zh_CN",
                "zh_TW",
        };
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }
}
