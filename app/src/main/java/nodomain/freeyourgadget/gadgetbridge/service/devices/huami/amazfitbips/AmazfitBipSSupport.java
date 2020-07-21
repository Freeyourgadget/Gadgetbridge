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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbips;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbips.AmazfitBipSFWHelper;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbip.AmazfitBipSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.UpdateFirmwareOperationNew;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;

public class AmazfitBipSSupport extends AmazfitBipSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBipSSupport.class);

    @Override
    public byte getCryptFlags() {
        return (byte) 0x80;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        super.sendNotificationNew(notificationSpec, true, 512);
    }

    @Override
    protected byte getAuthFlags() {
        return 0x00;
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            byte[] message = NotificationUtils.getPreferredTextFor(callSpec).getBytes();
            int length = 10 + message.length;
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(new byte[]{3, 0, 0, 0, 0, 0});
            buf.put(message);
            buf.put(new byte[]{0, 0, 0, 2});
            try {
                TransactionBuilder builder = performInitialized("incoming call");
                writeToChunked(builder, 0, buf.array());
                builder.queue(getQueue());
            } catch (IOException e) {
                LOG.error("Unable to send incoming call");
            }
        } else if ((callSpec.command == CallSpec.CALL_START) || (callSpec.command == CallSpec.CALL_END)) {
            try {
                TransactionBuilder builder = performInitialized("end call");
                writeToChunked(builder, 0, new byte[]{3, 3, 0, 0, 0, 0});
                builder.queue(getQueue());
            } catch (IOException e) {
                LOG.error("Unable to send end call");
            }
        }
    }

    @Override
    public HuamiFWHelper createFWHelper(Uri uri, Context context) throws IOException {
        return new AmazfitBipSFWHelper(uri, context);
    }

    @Override
    public UpdateFirmwareOperationNew createUpdateFirmwareOperation(Uri uri) {
        return new UpdateFirmwareOperationNew(uri, this);
    }

    @Override
    protected AmazfitBipSSupport setDisplayItems(TransactionBuilder builder) {
        if (gbDevice.getFirmwareVersion() == null) {
            LOG.warn("Device not initialized yet, won't set menu items");
            return this;
        }

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        Set<String> pages = prefs.getStringSet(HuamiConst.PREF_DISPLAY_ITEMS, new HashSet<>(Arrays.asList(getContext().getResources().getStringArray(R.array.pref_bips_display_items_default))));
        LOG.info("Setting display items to " + (pages == null ? "none" : pages));
        byte[] command = new byte[]{
                0x1E,
                0x00, 0x00, (byte) 0xFF, 0x01, // Status
                0x01, 0x00, (byte) 0xFF, 0x02, // HR
                0x02, 0x00, (byte) 0xFF, 0x19, // PAI
                0x03, 0x00, (byte) 0xFF, 0x03, // Workout
                0x04, 0x00, (byte) 0xFF, 0x11, // Alipay
                0x05, 0x00, (byte) 0xFF, 0x10, // NFC
                0x06, 0x00, (byte) 0xFF, 0x04, // Weather
                0x07, 0x00, (byte) 0xFF, 0x09, // Alarm
                0x08, 0x00, (byte) 0xFF, 0x1B, // Timer
                0x09, 0x00, (byte) 0xFF, 0x16, // Compass
                0x0A, 0x00, (byte) 0xFF, 0x1A, // World clock
                0x0B, 0x00, (byte) 0xFF, 0x0B, // Music
                0x0C, 0x00, (byte) 0xFF, 0x13  // Settings
        };

        String[] keys = {"status", "hr", "pai", "workout", "alipay", "nfc", "weather", "alarm", "timer", "compass", "worldclock", "music", "settings"};
        byte[] ids = {1, 2, 25, 3, 17, 16, 4, 9, 27, 22, 26, 11, 19};

        if (pages != null) {
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
    protected AmazfitBipSSupport setShortcuts(TransactionBuilder builder) {
        if (gbDevice.getFirmwareVersion() == null) {
            LOG.warn("Device not initialized yet, won't set shortcuts");
            return this;
        }

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
        Set<String> pages = prefs.getStringSet(HuamiConst.PREF_SHORTCUTS, new HashSet<>(Arrays.asList(getContext().getResources().getStringArray(R.array.pref_bips_shortcuts_default))));
        LOG.info("Setting shortcuts to " + (pages == null ? "none" : pages));
        byte[] command = new byte[]{
                0x1E,
                0x00, 0x00, (byte) 0xFD, 0x01, // Status
                0x01, 0x00, (byte) 0xFD, 0x11, // Alipay
                0x02, 0x00, (byte) 0xFD, 0x10, // NFC
                0x03, 0x00, (byte) 0xFD, 0x19, // PAI
                0x04, 0x00, (byte) 0xFD, 0x02, // HR
                0x05, 0x00, (byte) 0xFD, 0x0B, // Music
                0x06, 0x00, (byte) 0xFD, 0x04, // Weather
        };

        String[] keys = {"status", "alipay", "nfc", "pai", "hr", "music", "weather"};
        byte[] ids = {1, 17, 16, 25, 2, 11, 4};

        if (pages != null) {
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

}
