/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsMenuType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.MapUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsDisplayItemsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsDisplayItemsService.class);

    private static final short ENDPOINT = 0x0026;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_REQUEST = 0x03;
    public static final byte CMD_RESPONSE = 0x04;
    public static final byte CMD_CREATE = 0x05;
    public static final byte CMD_CREATE_ACK = 0x06;

    public static final byte DISPLAY_ITEMS_MENU = 0x01;
    public static final byte DISPLAY_ITEMS_SHORTCUTS = 0x02;
    public static final byte DISPLAY_ITEMS_CONTROL_CENTER = 0x03;

    public static final byte DISPLAY_ITEMS_SECTION_MAIN = 0x01;
    public static final byte DISPLAY_ITEMS_SECTION_MORE = 0x02;
    public static final byte DISPLAY_ITEMS_SECTION_DISABLED = 0x03;

    public ZeppOsDisplayItemsService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_RESPONSE:
                decodeAndUpdateDisplayItems(payload);
                break;
            case CMD_CREATE_ACK:
                LOG.info("Display items set ACK, type = {}, status = {}", payload[1], payload[2]);
                break;
            default:
                LOG.warn("Unexpected display items payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        final byte menuType;

        switch (config) {
            case HuamiConst.PREF_DISPLAY_ITEMS:
            case HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE:
                menuType = DISPLAY_ITEMS_MENU;
                break;
            case HuamiConst.PREF_SHORTCUTS:
            case HuamiConst.PREF_SHORTCUTS_SORTABLE:
                menuType = DISPLAY_ITEMS_SHORTCUTS;
                break;
            case HuamiConst.PREF_CONTROL_CENTER_SORTABLE:
                menuType = DISPLAY_ITEMS_CONTROL_CENTER;
                break;
            default:
                return false;
        }

        setDisplayItems(
                menuType,
                new ArrayList<>(prefs.getList(DeviceSettingsUtils.getPrefPossibleValuesKey(config), Collections.emptyList())),
                new ArrayList<>(prefs.getList(config, Collections.emptyList())),
                FlagsMap.fromPrefValue(prefs.getString(config + "_flags", "{}"))
        );


        return true;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestItems(builder, DISPLAY_ITEMS_MENU);
        requestItems(builder, DISPLAY_ITEMS_SHORTCUTS);
        if (getCoordinator().supportsControlCenter()) {
            requestItems(builder, DISPLAY_ITEMS_CONTROL_CENTER);
        }
    }

    public void requestItems(final TransactionBuilder builder, final byte type) {
        LOG.info("Requesting display items type={}", type);

        write(builder, new byte[]{CMD_REQUEST, type});
    }

    private void decodeAndUpdateDisplayItems(final byte[] payloadd) {
        final ByteBuffer buf = ByteBuffer.wrap(payloadd).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard the command byte

        final int type = buf.get();
        final int numberScreens = buf.getShort();

        LOG.info("Got {} display items of type {}", numberScreens, type);

        final String prefKey;
        final Map<String, String> idMap;
        switch (type) {
            case DISPLAY_ITEMS_MENU:
                prefKey = HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE;
                idMap = ZeppOsMenuType.displayItemNameLookup;
                break;
            case DISPLAY_ITEMS_SHORTCUTS:
                prefKey = HuamiConst.PREF_SHORTCUTS_SORTABLE;
                idMap = ZeppOsMenuType.shortcutsNameLookup;
                break;
            case DISPLAY_ITEMS_CONTROL_CENTER:
                prefKey = HuamiConst.PREF_CONTROL_CENTER_SORTABLE;
                idMap = ZeppOsMenuType.controlCenterNameLookup;
                break;
            default:
                LOG.error("Unknown display items type {}", String.format("0x%x", type));
                return;
        }
        final String allScreensPrefKey = DeviceSettingsUtils.getPrefPossibleValuesKey(prefKey);

        final boolean menuHasMoreSection;

        if (type == DISPLAY_ITEMS_MENU) {
            menuHasMoreSection = getCoordinator().mainMenuHasMoreSection();
        } else {
            menuHasMoreSection = false;
        }

        final String[] mainScreensArr = new String[numberScreens];
        final String[] moreScreensArr = new String[numberScreens];
        final List<String> allScreens = new LinkedList<>();

        // we need to save the flags so that we can send them when setting the screens
        final FlagsMap flagsMap = new FlagsMap();

        if (menuHasMoreSection) {
            // The band doesn't report the "more" screen, so we add it
            allScreens.add("more");
        }

        for (int i = 0; i < numberScreens; i++) {
            // Screen IDs are sent as literal hex strings
            final String screenId = StringUtils.untilNullTerminator(buf);
            final String screenNameOrId = idMap.containsKey(screenId) ? idMap.get(screenId) : screenId;
            allScreens.add(screenNameOrId);

            final int screenSectionVal = buf.get();
            final int screenPosition = buf.get();
            final int flags = buf.get();
            if (flags != 0) {
                final Flags flagsObj = new Flags();

                flagsObj.value = flags;

                // 1 = settings / can't be removed

                if ((flags & 2) != 0 || (flags & 4) != 0) {
                    // unsure which flag indicates the version and which indicates the unknown string
                    flagsObj.version = StringUtils.untilNullTerminator(buf);

                    if ((flags & 2) != 0 && (flags & 4) != 0) {
                        // but if both are set, we have a 2nd string
                        flagsObj.unk = StringUtils.untilNullTerminator(buf);
                    }
                }

                flagsMap.put(screenId, flagsObj);
            }

            if (screenPosition >= numberScreens) {
                LOG.warn("Invalid screen position {}, ignoring", screenPosition);
                continue;
            }

            switch (screenSectionVal) {
                case DISPLAY_ITEMS_SECTION_MAIN:
                    if (mainScreensArr[screenPosition] != null) {
                        LOG.warn("Duplicate position {} for main section", screenPosition);
                    }
                    //LOG.debug("mainScreensArr[{}] = {}", screenPosition, screenKey);
                    mainScreensArr[screenPosition] = screenNameOrId;
                    break;
                case DISPLAY_ITEMS_SECTION_MORE:
                    if (moreScreensArr[screenPosition] != null) {
                        LOG.warn("Duplicate position {} for more section", screenPosition);
                    }
                    //LOG.debug("moreScreensArr[{}] = {}", screenPosition, screenKey);
                    moreScreensArr[screenPosition] = screenNameOrId;
                    break;
                case DISPLAY_ITEMS_SECTION_DISABLED:
                    // Ignore disabled screens
                    //LOG.debug("Ignoring disabled screen {} {}", screenPosition, screenKey);
                    break;
                default:
                    LOG.warn("Unknown screen section {}, ignoring", String.format("0x%02x", screenSectionVal));
            }
        }

        final List<String> screens = new ArrayList<>(Arrays.asList(mainScreensArr));
        if (menuHasMoreSection) {
            screens.add("more");
            screens.addAll(Arrays.asList(moreScreensArr));
        }
        screens.removeAll(Collections.singleton(null));

        final String allScreensPrefValue = StringUtils.join(",", allScreens.toArray(new String[0])).toString();
        final String prefValue = StringUtils.join(",", screens.toArray(new String[0])).toString();
        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(prefKey + "_flags", flagsMap.toPrefValue())
                .withPreference(allScreensPrefKey, allScreensPrefValue)
                .withPreference(prefKey, prefValue);

        evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setDisplayItems(final byte menuType,
                                 final List<String> allSettings,
                                 List<String> enabledList,
                                 final FlagsMap flags) {
        try {
            final TransactionBuilder builder = new TransactionBuilder("set display items type " + menuType);
            setDisplayItems(builder, menuType, allSettings, enabledList, flags);
            builder.queue(getSupport().getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to set display items", e);
        }
    }

    private void setDisplayItems(final TransactionBuilder builder,
                                 final byte menuType,
                                 final List<String> allSettings,
                                 List<String> enabledList,
                                 final FlagsMap flagsMap) {
        final boolean isMainMenu = menuType == DISPLAY_ITEMS_MENU;
        final boolean isShortcuts = menuType == DISPLAY_ITEMS_SHORTCUTS;
        final boolean hasMoreSection;
        final Map<String, String> idMap;

        switch (menuType) {
            case DISPLAY_ITEMS_MENU:
                LOG.info("Setting menu items");
                hasMoreSection = getCoordinator().mainMenuHasMoreSection();
                idMap = MapUtils.reverse(ZeppOsMenuType.displayItemNameLookup);
                break;
            case DISPLAY_ITEMS_SHORTCUTS:
                LOG.info("Setting shortcuts");
                hasMoreSection = false;
                idMap = MapUtils.reverse(ZeppOsMenuType.shortcutsNameLookup);
                break;
            case DISPLAY_ITEMS_CONTROL_CENTER:
                LOG.info("Setting control center");
                hasMoreSection = false;
                idMap = MapUtils.reverse(ZeppOsMenuType.controlCenterNameLookup);
                break;
            default:
                LOG.warn("Unknown menu type {}", menuType);
                return;
        }

        if (allSettings.isEmpty()) {
            LOG.warn("List of all display items is missing");
            return;
        }

        if (flagsMap == null) {
            LOG.error("Flags map is missing");
            return;
        }

        if (isMainMenu && !enabledList.contains("settings")) {
            // Settings can't be disabled
            enabledList.add("settings");
        }

        if (isShortcuts && enabledList.size() > 10) {
            // Enforced by official app
            LOG.warn("Truncating shortcuts list to 10");
            enabledList = enabledList.subList(0, 10);
        }

        LOG.info("Setting display items (shortcuts={}): {}", isShortcuts, enabledList);

        int numItems = allSettings.size();
        if (hasMoreSection) {
            // Exclude the "more" item from the main menu, since it's not a real item
            numItems--;
        }

        int flagsOverhead = 0;
        for (final Map.Entry<String, Flags> e : flagsMap.entrySet()) {
            if (e.getValue().version != null) {
                flagsOverhead += e.getValue().version.getBytes(StandardCharsets.UTF_8).length + 1;
            }
            if (e.getValue().unk != null) {
                flagsOverhead += e.getValue().unk.getBytes(StandardCharsets.UTF_8).length + 1;
            }
        }

        final ByteBuffer buf = ByteBuffer.allocate(4 + numItems * 12 + flagsOverhead);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_CREATE);
        buf.put(menuType);
        buf.putShort((short) numItems);

        byte pos = 0;
        boolean inMoreSection = false;

        // IDs are 8-char hex strings, in upper case
        final Pattern ID_REGEX = Pattern.compile("^[0-9A-F]{8}$");

        for (final String name : enabledList) {
            if (name.equals("more")) {
                inMoreSection = true;
                pos = 0;
                continue;
            }

            final String id = idMap.containsKey(name) ? Objects.requireNonNull(idMap.get(name)) : name;
            if (!ID_REGEX.matcher(id).find()) {
                LOG.error("Screen item id '{}' is not 8-char hex string", id);
                continue;
            }

            final byte sectionKey;
            if (inMoreSection) {
                // In more section
                sectionKey = DISPLAY_ITEMS_SECTION_MORE;
            } else {
                // In main section
                sectionKey = DISPLAY_ITEMS_SECTION_MAIN;
            }

            // Screen IDs are sent as literal hex strings
            buf.put(id.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0);
            buf.put(sectionKey);
            buf.put(pos++);
            final Flags flags = flagsMap.get(id);
            if (flags != null && flags.value != 0) {
                buf.put((byte) (flags.value));
                if (flags.version != null) {
                    buf.put(flags.version.getBytes(StandardCharsets.UTF_8));
                    buf.put((byte) 0);
                }
                if (flags.unk != null) {
                    buf.put(flags.unk.getBytes(StandardCharsets.UTF_8));
                    buf.put((byte) 0);
                }
            } else {
                buf.put((byte) 0);
            }
        }

        // Set all disabled items
        pos = 0;
        for (final String name : allSettings) {
            if (enabledList.contains(name) || name.equals("more")) {
                continue;
            }

            final String id = idMap.containsKey(name) ? Objects.requireNonNull(idMap.get(name)) : name;
            if (!ID_REGEX.matcher(id).find()) {
                LOG.error("Screen item id '{}' is not 8-char hex string", id);
                continue;
            }

            // Screen IDs are sent as literal hex strings
            buf.put(id.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0);
            buf.put(DISPLAY_ITEMS_SECTION_DISABLED);
            buf.put(pos++);
            final Flags flags = flagsMap.get(id);
            if (flags != null && flags.value != 0) {
                buf.put((byte) (flags.value));
                if (flags.version != null) {
                    buf.put(flags.version.getBytes(StandardCharsets.UTF_8));
                    buf.put((byte) 0);
                }
                if (flags.unk != null) {
                    buf.put(flags.unk.getBytes(StandardCharsets.UTF_8));
                    buf.put((byte) 0);
                }
            } else {
                buf.put((byte) 0);
            }
        }

        write(builder, buf.array());
    }

    private static class Flags {
        private int value = 0;
        private String version = null;
        private String unk = null;
    }

    /**
     * When requesting the items from the watch, it sends a bitmask with flags, and a version / unknown
     * string. We need to persist this so that we can send it back when setting the display items.
     */
    private static class FlagsMap extends HashMap<String, Flags> {
        @Nullable
        private String toPrefValue() {
            final JSONObject jsonObject = new JSONObject();

            for (final Map.Entry<String, Flags> e : entrySet()) {
                final JSONObject entryObj = new JSONObject();

                try {
                    entryObj.put("value", e.getValue().value);
                    if (e.getValue().version != null) {
                        entryObj.put("version", e.getValue().version);
                    }
                    if (e.getValue().unk != null) {
                        entryObj.put("unk", e.getValue().unk);
                    }
                    jsonObject.put(e.getKey(), entryObj);
                } catch (final JSONException ex) {
                    LOG.error("This should never happen", ex);
                    return null;
                }
            }

            return jsonObject.toString();
        }

        @Nullable
        private static FlagsMap fromPrefValue(final String value) {
            final FlagsMap flagsMap = new FlagsMap();

            final JSONObject flagsMapObj;
            try {
                flagsMapObj = new JSONObject(value);

                final Iterator<String> keys = flagsMapObj.keys();
                while (keys.hasNext()) {
                    final String id = keys.next();
                    final JSONObject flagsObj = flagsMapObj.getJSONObject(id);

                    final Flags flags = new Flags();
                    if (flagsObj.has("value")) {
                        flags.value = flagsObj.getInt("value");
                    }
                    if (flagsObj.has("version")) {
                        flags.version = flagsObj.getString("version");
                    }
                    if (flagsObj.has("unk")) {
                        flags.unk = flagsObj.getString("unk");
                    }
                    flagsMap.put(id, flags);
                }

                return flagsMap;
            } catch (final JSONException ex) {
                LOG.error("This should never happen", ex);
                return null;
            }
        }
    }
}
