/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_WATCHFACE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeppOsWatchfaceService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsWatchfaceService.class);

    private static final short ENDPOINT = 0x0023;

    public static final byte CMD_LIST_GET = 0x05;
    public static final byte CMD_LIST_RET = 0x06;
    public static final byte CMD_SET = 0x07;
    public static final byte CMD_SET_ACK = 0x08;
    public static final byte CMD_CURRENT_GET = 0x09;
    public static final byte CMD_CURRENT_RET = 0x0a;

    public enum Watchface {
        // Codes are from GTR 4, not sure if they match on other watches
        RED_FANTASY(0x00002D38),
        MULTIPLE_DATA(0x00002D10),
        RUSH(0x00002D37),
        MINIMALIST(0x00002D0E),
        SIMPLICITY_DATA(0x00002D08),
        VIBRANT(0x00002D09),
        BUSINESS_STYLE(0x00002D0D),
        EMERALD_MOONLIGHT(0x00002D0A),
        ROTATING_EARTH(0x00002D0F),
        SUPERPOSITION(0x00002D0C),
        ;

        private final int code;

        Watchface(final int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Watchface fromCode(final int code) {
            for (final Watchface watchface : values()) {
                if (watchface.getCode() == code) {
                    return watchface;
                }
            }

            return null;
        }
    }

    public ZeppOsWatchfaceService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return true;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_LIST_RET:
                LOG.info("Got watchface list, status = {}", payload[1]);
                if (payload[1] != 1) {
                    LOG.warn("Unexpected status byte {}", payload[1]);
                    return;
                }
                parseWatchfaceList(payload);
                break;
            case CMD_SET_ACK:
                LOG.info("Got watchface set ack, status = {}", payload[1]);
                break;
            case CMD_CURRENT_RET:
                final int watchface = BLETypeConversions.toUint32(payload, 1);
                final String watchfaceHex = String.format(Locale.ROOT, "0x%08X", watchface);
                LOG.info("Got current watchface = {}", watchfaceHex);
                getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_WATCHFACE, watchfaceHex));
                break;
            default:
                LOG.warn("Unexpected watchface byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_WATCHFACE:
                final String watchface = prefs.getString(DeviceSettingsPreferenceConst.PREF_WATCHFACE, null);
                LOG.info("Setting watchface to {}", watchface);
                setWatchface(watchface);
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestWatchfaces(builder);
        requestCurrentWatchface(builder);
    }

    public void requestWatchfaces(final TransactionBuilder builder) {
        write(builder, CMD_LIST_GET);
    }

    public void requestCurrentWatchface(final TransactionBuilder builder) {
        write(builder, CMD_CURRENT_GET);
    }

    public void parseWatchfaceList(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard the command byte
        buf.get(); // discard the status byte
        final int numWatchfaces = buf.get() & 0xFF;

        final List<String> watchfaces = new ArrayList<>();

        for (int i = 0; i < numWatchfaces; i++) {
            final int watchfaceCode = buf.getInt();
            final Watchface watchface = Watchface.fromCode(watchfaceCode);
            if (watchface != null) {
                watchfaces.add(watchface.name().toLowerCase(Locale.ROOT));
            } else {
                final String watchfaceHex = String.format(Locale.ROOT, "0x%08X", watchfaceCode);
                LOG.warn("Unknown watchface code {}", watchfaceHex);
                watchfaces.add(watchfaceHex);
            }
        }

        final GBDeviceEventUpdatePreferences evt = new GBDeviceEventUpdatePreferences()
                .withPreference(Huami2021Coordinator.getPrefPossibleValuesKey(PREF_WATCHFACE), String.join(",", watchfaces));
        getSupport().evaluateGBDeviceEvent(evt);
    }

    public void setWatchface(final String watchfacePrefValue) {
        if (watchfacePrefValue == null) {
            LOG.warn("watchface is null");
            return;
        }

        int watchfaceInt;

        try {
            final Watchface watchfaceEnum = Watchface.valueOf(watchfacePrefValue.toUpperCase(Locale.ROOT));
            watchfaceInt = watchfaceEnum.getCode();
        } catch (final IllegalArgumentException e) {
            // attempt to parse as hex
            final Matcher matcher = Pattern.compile("^0[xX]([0-9a-fA-F]+)$").matcher(watchfacePrefValue);
            if (!matcher.find()) {
                LOG.warn("Failed to parse watchface '{}' as hex", watchfacePrefValue);
                return;
            }
            watchfaceInt = Integer.parseInt(matcher.group(1), 16);
        }

        final ByteBuffer buf = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_SET);
        buf.putInt(watchfaceInt);

        write("set watchface", buf.array());
    }
}
