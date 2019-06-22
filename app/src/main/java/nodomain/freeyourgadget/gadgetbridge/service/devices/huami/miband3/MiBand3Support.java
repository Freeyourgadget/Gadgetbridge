/*  Copyright (C) 2017-2019 Andreas Shimokawa, Carsten Pfeiffer, José Rebelo

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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3FWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3.MiBand3Service;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class MiBand3Support extends AmazfitBipSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MiBand3Support.class);

    @Override
    protected byte getAuthFlags() {
        return 0x00;
    }

    @Override
    protected MiBand3Support setDisplayItems(TransactionBuilder builder) {
        Set<String> pages = HuamiCoordinator.getDisplayItems(gbDevice.getAddress());

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
            if (pages.contains("activity")) {
                command[1] |= 0x08;
                command[6] = pos++;
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
            if (pages.contains("timer")) {
                command[1] |= 0x80;
                command[10] = pos++;
            }
        }

        for (int i = 4; i <= 10; i++) {
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
                case MiBandConst.PREF_NIGHT_MODE:
                case MiBandConst.PREF_NIGHT_MODE_START:
                case MiBandConst.PREF_NIGHT_MODE_END:
                    setNightMode(builder);
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

    private MiBand3Support setNightMode(TransactionBuilder builder) {
        String nightMode = MiBand3Coordinator.getNightMode(gbDevice.getAddress());
        LOG.info("Setting night mode to " + nightMode);

        switch (nightMode) {
            case MiBandConst.PREF_NIGHT_MODE_SUNSET:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), MiBand3Service.COMMAND_NIGHT_MODE_SUNSET);
                break;
            case MiBandConst.PREF_NIGHT_MODE_OFF:
                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), MiBand3Service.COMMAND_NIGHT_MODE_OFF);
                break;
            case MiBandConst.PREF_NIGHT_MODE_SCHEDULED:
                byte[] cmd = MiBand3Service.COMMAND_NIGHT_MODE_SCHEDULED.clone();

                Calendar calendar = GregorianCalendar.getInstance();

                Date start = MiBand3Coordinator.getNightModeStart(gbDevice.getAddress());
                calendar.setTime(start);
                cmd[2] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[3] = (byte) calendar.get(Calendar.MINUTE);

                Date end = MiBand3Coordinator.getNightModeEnd(gbDevice.getAddress());
                calendar.setTime(end);
                cmd[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                cmd[5] = (byte) calendar.get(Calendar.MINUTE);

                builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), cmd);
                break;
            default:
                LOG.error("Invalid night mode: " + nightMode);
                break;
        }

        return this;
    }

    @Override
    public void phase2Initialize(TransactionBuilder builder) {
        super.phase2Initialize(builder);
        LOG.info("phase2Initialize...");
        setLanguage(builder);
        setBandScreenUnlock(builder);
        setNightMode(builder);
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand3FWHelper(uri, context);
    }
}
