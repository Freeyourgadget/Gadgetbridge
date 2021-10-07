package nodomain.freeyourgadget.gadgetbridge.devices.huami.miband6;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.R;
public class MiBand6Coordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand6Coordinator.class);
    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        try {
            BluetoothDevice device = candidate.getDevice();
            String name = device.getName();
            if (name != null && name.equalsIgnoreCase(HuamiConst.MI_BAND6_NAME)) {
                return DeviceType.MIBAND6;
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND6;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        MiBand6FWInstallHandler handler = new MiBand6FWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
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
    public boolean supportsUnicodeEmojis() {
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
                R.xml.devicesettings_miband6,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_custom_emoji_font,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_dateformat,
                R.xml.devicesettings_nightmode,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_swipeunlock,
                R.xml.devicesettings_sync_calendar,
                R.xml.devicesettings_expose_hr_thirdparty,
                R.xml.devicesettings_pairingkey,
                R.xml.devicesettings_high_mtu
        };
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "ar_SA",
                "cs_CZ",
                "de_DE",
                "el_GR",
                "en_US",
                "es_ES",
                "fr_FR",
                "he_IL",
                "id_ID",
                "it_IT",
                "nl_NL",
                "pt_PT",
                "pl_PL",
                "ro_RO",
                "ru_RU",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CN",
                "zh_TW",
        };
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }
}
