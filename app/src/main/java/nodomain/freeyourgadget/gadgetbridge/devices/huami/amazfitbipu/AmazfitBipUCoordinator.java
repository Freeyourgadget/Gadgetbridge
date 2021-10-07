/*  Copyright (C) 2017-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, João Paulo Barraca, Nephiel, Petr Vaněk, TinfoilSubmarine,
    Zhong Jianxin

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbipu;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class AmazfitBipUCoordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBipUCoordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.AMAZFITBIPU;
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        try {
            BluetoothDevice device = candidate.getDevice();
            String name = device.getName();
            if (name != null && (name.equalsIgnoreCase("Amazfit Bip U"))) {
                return DeviceType.AMAZFITBIPU;
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        AmazfitBipUFWInstallHandler handler = new AmazfitBipUFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_amazfitbipu,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_custom_emoji_font,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_sync_calendar,
                R.xml.devicesettings_expose_hr_thirdparty,
                R.xml.devicesettings_bt_connected_advertisement,
                R.xml.devicesettings_high_mtu,
                R.xml.devicesettings_device_actions,
                R.xml.devicesettings_pairingkey
        };
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "cs_CZ",
                "de_DE",
                "el_GR",
                "en_US",
                "es_ES",
                "fr_FR",
                "id_ID",
                "it_IT",
                "ja_JP",
                "ko_KO",
                "nl_NL",
                "pl_PL",
                "pt_BR",
                "ru_RU",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CH",
                "zh_TW",
        };
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }
}
