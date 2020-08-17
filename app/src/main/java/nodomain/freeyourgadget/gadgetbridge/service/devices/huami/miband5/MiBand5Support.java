/*  Copyright (C) 2017-2020 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband5;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.miband5.MiBand5FWHelper;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband4.MiBand4Support;

public class MiBand5Support extends MiBand4Support {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand5Support.class);

    @Override
    protected MiBand5Support setDisplayItems(TransactionBuilder builder) {
        if (gbDevice.getFirmwareVersion() == null) {
            LOG.warn("Device not initialized yet, won't set menu items");
            return this;
        }

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        Set<String> pages = prefs.getStringSet(HuamiConst.PREF_DISPLAY_ITEMS, new HashSet<>(Arrays.asList(getContext().getResources().getStringArray(R.array.pref_miband5_display_items_default))));
        LOG.info("Setting display items to " + (pages == null ? "none" : pages));
        byte[] command = new byte[]{
                0x1E,
                0x00, 0x00, (byte) 0xFF, 0x12, // Display clock?
                0x01, 0x00, (byte) 0xFF, 0x01, // Status
                0x02, 0x00, (byte) 0xFF, 0x19, // PAI
                0x03, 0x00, (byte) 0xFF, 0x02, // HR
                0x04, 0x00, (byte) 0xFF, 0x06, // Notifications
                0x05, 0x00, (byte) 0xFF, 0x33, // Breathing
                0x06, 0x00, (byte) 0xFF, 0x15, // Events
                0x07, 0x00, (byte) 0xFF, 0x04, // Weather
                0x08, 0x00, (byte) 0xFF, 0x03, // Workout
                0x09, 0x00, (byte) 0xFF, 0x07, // More
                0x0A, 0x00, (byte) 0xFF, 0x1c, // Stress
                0x0B, 0x00, (byte) 0xFF, 0x1d  // Cycles
        };

        String[] keys = {"displayclock", "status", "pai", "hr", "notifications", "breathing", "eventreminder", "weather", "workout", "more", "stress", "cycles"};
        byte[] ids = {0x12, 0x01, 0x19, 0x02, 0x06, 0x33, 0x15, 0x04, 0x03, 0x07, 0x1c, 0x1d};

        if (pages != null) {
            pages.add("displayclock");
            // it seem that we first have to put all ENABLED items into the array
            int pos = 1;
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                byte id = ids[i];
                if (pages.contains(key)) {
                    command[pos + 1] = 0x00;
                    command[pos + 3] = id;
                    pos += 4;
                }
            }
            // And then all DISABLED ones
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                byte id = ids[i];
                if (!pages.contains(key)) {
                    command[pos + 1] = 0x01;
                    command[pos + 3] = id;
                    pos += 4;
                }
            }
            writeToChunked(builder, 2, command);
        }

        return this;
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new MiBand5FWHelper(uri, context);
    }

    @Override
    public int getActivitySampleSize() {
        return 8;
    }
}
