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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.LocaleConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.LocaleConfig.SetLanguageSetting;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetLanguageSettingRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetLanguageSettingRequest.class);

    public SetLanguageSettingRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = LocaleConfig.id;
        this.commandId = SetLanguageSetting.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        String localeString = GBApplication
            .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
            .getString(DeviceSettingsPreferenceConst.PREF_LANGUAGE, "auto");
        if (localeString == null || localeString.equals("auto")) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            if (country.equals("")) {
                country = language;
            }
            localeString = language + "-" + country.toUpperCase();
        } else {
            localeString = localeString.replace("_", "-");
        }
        LOG.debug("localeString: " + localeString);
        String measurementString = GBApplication
            .getPrefs()
            .getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, getContext().getString(R.string.p_unit_metric));
        LOG.debug("measurementString: " + measurementString);
        byte measurement = measurementString.equals("metric") ? LocaleConfig.MeasurementSystem.metric : LocaleConfig.MeasurementSystem.imperial;
        try {
            return new SetLanguageSetting(paramsProvider, localeString.getBytes(StandardCharsets.UTF_8), measurement).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Set Locale");
    }
}
