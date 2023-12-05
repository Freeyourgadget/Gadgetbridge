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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.MapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsMorningUpdatesService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMorningUpdatesService.class);

    private static final short ENDPOINT = 0x003f;

    private static final byte CMD_ENABLED_GET = 0x03;
    private static final byte CMD_ENABLED_RET = 0x04;
    private static final byte CMD_ENABLED_SET = 0x07;
    private static final byte CMD_ENABLED_SET_ACK = 0x08;
    private static final byte CMD_CATEGORIES_REQUEST = 0x05;
    private static final byte CMD_CATEGORIES_RESPONSE = 0x06;
    private static final byte CMD_CATEGORIES_SET = 0x09;
    private static final byte CMD_CATEGORIES_SET_ACK = 0x0a;

    private static final Map<Byte, String> MORNING_UPDATES_MAP = new HashMap<Byte, String>() {{
         put((byte) 0x02, "weather");
         put((byte) 0x03, "battery");
         put((byte) 0x04, "sleep");
         put((byte) 0x06, "event");
         put((byte) 0x07, "pai");
         put((byte) 0x08, "yesterdays_activity");
         put((byte) 0x09, "zepp_coach");
         put((byte) 0x0a, "cycle_tracking");
    }};

    public ZeppOsMorningUpdatesService(Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public void handlePayload(byte[] payload) {
        switch (payload[0]) {
            case CMD_ENABLED_RET:
                if (payload[1] != 0x01) {
                    LOG.warn("Unexpected morning updates enabled byte {}", payload[1]);
                    return;
                }
                final Boolean enabled = booleanFromByte(payload[2]);
                if (enabled == null) {
                    LOG.error("Unexpected morning updates enabled byte");
                    return;
                }
                final GBDeviceEventUpdatePreferences gbDeviceEventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                        .withPreference(DeviceSettingsUtils.getPrefKnownConfig(DeviceSettingsPreferenceConst.MORNING_UPDATES_ENABLED), true)
                        .withPreference(DeviceSettingsPreferenceConst.MORNING_UPDATES_ENABLED, enabled);
                getSupport().evaluateGBDeviceEvent(gbDeviceEventUpdatePreferences);
                LOG.info("Morning updates enabled = {}", enabled);
                return;
            case CMD_ENABLED_SET_ACK:
                LOG.info("Morning updates enabled set ack ACK, status = {}", payload[1]);
                return;
            case CMD_CATEGORIES_RESPONSE:
                LOG.info("Got morning update items from watch");
                decodeAndUpdateCategories(payload);
                return;
            case CMD_CATEGORIES_SET_ACK:
                LOG.info("Morning updates categories set ack ACK, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected morning updates byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.MORNING_UPDATES_ENABLED:
                final boolean morningUpdatesEnabled = prefs.getBoolean(config, false);
                LOG.info("Setting morning updates enabled = {}", morningUpdatesEnabled);
                setEnabled(morningUpdatesEnabled);
                return true;
            case DeviceSettingsPreferenceConst.MORNING_UPDATES_CATEGORIES_SORTABLE:
                final List<String> categories = new ArrayList<>(prefs.getList(config, Collections.emptyList()));
                final List<String> allCategories = new ArrayList<>(prefs.getList(DeviceSettingsUtils.getPrefPossibleValuesKey(config), Collections.emptyList()));
                LOG.info("Setting morning updates categories = {}", categories);
                setCategories(categories, allCategories);
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        getEnabled(builder);
        getCategories(builder);
    }

    public void getEnabled(final TransactionBuilder builder) {
        write(builder, CMD_ENABLED_GET);
    }

    public void setEnabled(final boolean enabled) {
        write("set morning updates enabled", new byte[] {CMD_ENABLED_SET, bool(enabled)});
    }

    public void getCategories(final TransactionBuilder builder) {
        write(builder, CMD_CATEGORIES_REQUEST);
    }

    public void setCategories(final List<String> categories, final List<String> allCategories) {
        // Build a sorted list, with the enabled at the start
        final List<String> categoriesSorted = new ArrayList<>(categories);
        for (String category : allCategories) {
            if (!categories.contains(category)) {
                categoriesSorted.add(category);
            }
        }

        final Map<String, Byte> idMap = MapUtils.reverse(MORNING_UPDATES_MAP);

        final ByteBuffer buf = ByteBuffer.allocate(2 + categoriesSorted.size() * 3)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_CATEGORIES_SET);
        buf.put((byte) categoriesSorted.size());

        for (final String category : categoriesSorted) {
            final byte id;
            if (idMap.containsKey(category)) {
                id = idMap.get(category);
            } else {
                // name doesn't match a known value, attempt to parse it as hex
                final Matcher matcher = Pattern.compile("^0[xX]([0-9a-fA-F]{1,2})$").matcher(category);
                if (matcher.find()) {
                    id = (byte) Integer.parseInt(matcher.group(1), 16);
                } else {
                    LOG.warn("Unknown category {}, and failed to parse as hex, not setting", category);
                    return;
                }
            }

            buf.put(id);
            buf.put((byte) 0x00);
            buf.put(bool(categories.contains(category)));
        }

        write("set morning updates categories", buf.array());
    }

    private void decodeAndUpdateCategories(final byte[] payload) {
        if (payload[1] != 0x01) {
            LOG.warn("Unexpected morning update items byte {}", payload[1]);
            return;
        }

        final int numCategories = payload[2];
        final int expectedLength = 3 + numCategories * 3;
        if (payload.length != expectedLength) {
            LOG.warn("Unexpected morning updates categories payload size {}, expected {}", payload.length, expectedLength);
            return;
        }

        final List<String> enabledCategories = new ArrayList<>(numCategories);
        final List<String> allCategories = new ArrayList<>(numCategories);

        for (int i = 3; i < expectedLength; i += 3) {
            final String itemName = MORNING_UPDATES_MAP.containsKey(payload[i])
                    ? MORNING_UPDATES_MAP.get(payload[i])
                    : String.format("0x%x", payload[i]);

            allCategories.add(itemName);

            // i + 1 is always 0
            final Boolean itemEnabled = booleanFromByte(payload[i + 2]);
            if (itemEnabled == null) {
                LOG.warn("Unexpected enabled byte {} for item at i={}", payload[i + 2], i);
            } else if(itemEnabled) {
                enabledCategories.add(itemName);
            }
        }

        final String prefKey = DeviceSettingsPreferenceConst.MORNING_UPDATES_CATEGORIES_SORTABLE;
        final String allCategoriesPrefKey = DeviceSettingsUtils.getPrefPossibleValuesKey(prefKey);

        final String allCategoriesPrefValue = StringUtils.join(",", allCategories.toArray(new String[0])).toString();
        final String prefValue = StringUtils.join(",", enabledCategories.toArray(new String[0])).toString();
        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsUtils.getPrefKnownConfig(prefKey), true)
                .withPreference(allCategoriesPrefKey, allCategoriesPrefValue)
                .withPreference(prefKey, prefValue);

        getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
    }
}
