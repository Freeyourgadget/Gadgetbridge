/*  Copyright (C) 2017-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband3;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3FWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Service;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class MiBand3Support extends AmazfitBipSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MiBand3Support.class);

    @Override
    protected MiBand3Support setDisplayItems(TransactionBuilder builder) {
        Prefs prefs = GBApplication.getPrefs();
        Set<String> pages = prefs.getStringSet("miband3_display_items", null);
        LOG.info("Setting display items to " + (pages == null ? "none" : pages));
        byte[] command = MiBand3Service.COMMAND_CHANGE_SCREENS.clone();

        byte pos = 1;
        if (pages != null) {
            if (pages.contains("notifications")) {
                command[1] |= 0x02;
                command[4] = pos++;
            }
            if (pages.contains("weather")) {
                command[1] |= 0x04;
                command[5] = pos++;
            }
            if (pages.contains("more")) {
                command[1] |= 0x10;
                command[7] = pos++;
            }
            if (pages.contains("status")) {
                command[1] |= 0x20;
                command[8] = pos++;
            }
            if (pages.contains("heart_rate")) {
                command[1] |= 0x40;
                command[9] = pos++;
            }
        }

        for (int i = 4; i <= 9; i++) {
            if (command[i] == 0) {
                command[i] = pos++;
            }
        }

        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), command);

        return this;
    }

    @Override
    public void onSendConfiguration(String config) {
        TransactionBuilder builder;
        try {
            builder = performInitialized("Sending configuration for option: " + config);
            switch (config) {
                case MiBandConst.PREF_MI3_BAND_SCREEN_UNLOCK:
                    setBandScreenUnlock(builder);
                    break;
                default:
                    super.onSendConfiguration(config);
                    return;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    protected MiBand3Support setLanguage(TransactionBuilder builder) {
        String localeString = GBApplication.getPrefs().getString("miband3_language", "auto");

        if (localeString.equals("auto")) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            if (country == null) {
                // sometimes country is null, no idea why, guess it.
                country = language;
            }
            localeString = language + "_" + country.toUpperCase();
        }
        LOG.info("Setting device to locale: " + localeString);
        byte[] command_new = HuamiService.COMMAND_SET_LANGUAGE_NEW_TEMPLATE.clone();
        System.arraycopy(localeString.getBytes(), 0, command_new, 3, localeString.getBytes().length);

        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), command_new);

        return this;
    }

    private MiBand3Support setBandScreenUnlock(TransactionBuilder builder) {
        boolean enable = MiBand3Coordinator.getBandScreenUnlock();
        LOG.info("Setting band screen unlock to " + enable);

        if (enable) {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), MiBand3Service.COMMAND_ENABLE_BAND_SCREEN_UNLOCK);
        } else {
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), MiBand3Service.COMMAND_DISABLE_BAND_SCREEN_UNLOCK);
        }

        return this;
    }

    @Override
    public void phase2Initialize(TransactionBuilder builder) {
        super.phase2Initialize(builder);
        LOG.info("phase2Initialize...");
        setBandScreenUnlock(builder);
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand3FWHelper(uri, context);
    }
}
