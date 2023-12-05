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

import static org.apache.commons.lang3.ArrayUtils.subarray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021MenuType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
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

    public ZeppOsDisplayItemsService(final Huami2021Support support) {
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
        switch (config) {
            case HuamiConst.PREF_DISPLAY_ITEMS:
            case HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE:
                setDisplayItems(
                        DISPLAY_ITEMS_MENU,
                        new ArrayList<>(prefs.getList(DeviceSettingsUtils.getPrefPossibleValuesKey(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE), Collections.emptyList())),
                        new ArrayList<>(prefs.getList(HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE, Collections.emptyList()))
                );
                return true;
            case HuamiConst.PREF_SHORTCUTS:
            case HuamiConst.PREF_SHORTCUTS_SORTABLE:
                setDisplayItems(
                        DISPLAY_ITEMS_SHORTCUTS,
                        new ArrayList<>(prefs.getList(DeviceSettingsUtils.getPrefPossibleValuesKey(HuamiConst.PREF_SHORTCUTS_SORTABLE), Collections.emptyList())),
                        new ArrayList<>(prefs.getList(HuamiConst.PREF_SHORTCUTS_SORTABLE, Collections.emptyList()))
                );
                return true;
            case HuamiConst.PREF_CONTROL_CENTER_SORTABLE:
                setDisplayItems(
                        DISPLAY_ITEMS_CONTROL_CENTER,
                        new ArrayList<>(prefs.getList(DeviceSettingsUtils.getPrefPossibleValuesKey(HuamiConst.PREF_CONTROL_CENTER_SORTABLE), Collections.emptyList())),
                        new ArrayList<>(prefs.getList(HuamiConst.PREF_CONTROL_CENTER_SORTABLE, Collections.emptyList()))
                );
                return true;
        }

        return false;
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

    private void decodeAndUpdateDisplayItems(final byte[] payload) {
        LOG.info("Got display items from band, type={}", payload[1]);

        final int numberScreens = payload[2];
        final int expectedLength = 4 + numberScreens * 12;
        if (payload.length != 4 + numberScreens * 12) {
            LOG.error("Unexpected display items payload length {}, expected {}", payload.length, expectedLength);
            return;
        }

        final String prefKey;
        final Map<String, String> idMap;
        switch (payload[1]) {
            case DISPLAY_ITEMS_MENU:
                LOG.info("Got {} display items", numberScreens);
                prefKey = HuamiConst.PREF_DISPLAY_ITEMS_SORTABLE;
                idMap = Huami2021MenuType.displayItemNameLookup;
                break;
            case DISPLAY_ITEMS_SHORTCUTS:
                LOG.info("Got {} shortcuts", numberScreens);
                prefKey = HuamiConst.PREF_SHORTCUTS_SORTABLE;
                idMap = Huami2021MenuType.shortcutsNameLookup;
                break;
            case DISPLAY_ITEMS_CONTROL_CENTER:
                LOG.info("Got {} control center", numberScreens);
                prefKey = HuamiConst.PREF_CONTROL_CENTER_SORTABLE;
                idMap = Huami2021MenuType.controlCenterNameLookup;
                break;
            default:
                LOG.error("Unknown display items type {}", String.format("0x%x", payload[1]));
                return;
        }
        final String allScreensPrefKey = DeviceSettingsUtils.getPrefPossibleValuesKey(prefKey);

        final boolean menuHasMoreSection;

        if (payload[1] == DISPLAY_ITEMS_MENU) {
            menuHasMoreSection = getCoordinator().mainMenuHasMoreSection();
        } else {
            menuHasMoreSection = false;
        }

        final String[] mainScreensArr = new String[numberScreens];
        final String[] moreScreensArr = new String[numberScreens];
        final List<String> allScreens = new LinkedList<>();
        if (menuHasMoreSection) {
            // The band doesn't report the "more" screen, so we add it
            allScreens.add("more");
        }

        for (int i = 0; i < numberScreens; i++) {
            // Screen IDs are sent as literal hex strings
            final String screenId = new String(subarray(payload, 4 + i * 12, 4 + i * 12 + 8));
            final String screenNameOrId = idMap.containsKey(screenId) ? idMap.get(screenId) : screenId;
            allScreens.add(screenNameOrId);

            final int screenSectionVal = payload[4 + i * 12 + 9];
            final int screenPosition = payload[4 + i * 12 + 10];

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
                .withPreference(allScreensPrefKey, allScreensPrefValue)
                .withPreference(prefKey, prefValue);

        evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    private void setDisplayItems(final byte menuType,
                                 final List<String> allSettings,
                                 List<String> enabledList) {
        try {
            final TransactionBuilder builder = new TransactionBuilder("set display items type " + menuType);
            setDisplayItems(builder, menuType, allSettings, enabledList);
            builder.queue(getSupport().getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to set display items", e);
        }
    }

    private void setDisplayItems(final TransactionBuilder builder,
                                 final byte menuType,
                                 final List<String> allSettings,
                                 List<String> enabledList) {
        final boolean isMainMenu = menuType == DISPLAY_ITEMS_MENU;
        final boolean isShortcuts = menuType == DISPLAY_ITEMS_SHORTCUTS;
        final boolean hasMoreSection;
        final Map<String, String> idMap;

        switch (menuType) {
            case DISPLAY_ITEMS_MENU:
                LOG.info("Setting menu items");
                hasMoreSection = getCoordinator().mainMenuHasMoreSection();
                idMap = MapUtils.reverse(Huami2021MenuType.displayItemNameLookup);
                break;
            case DISPLAY_ITEMS_SHORTCUTS:
                LOG.info("Setting shortcuts");
                hasMoreSection = false;
                idMap = MapUtils.reverse(Huami2021MenuType.shortcutsNameLookup);
                break;
            case DISPLAY_ITEMS_CONTROL_CENTER:
                LOG.info("Setting control center");
                hasMoreSection = false;
                idMap = MapUtils.reverse(Huami2021MenuType.controlCenterNameLookup);
                break;
            default:
                LOG.warn("Unknown menu type {}", menuType);
                return;
        }

        if (allSettings.isEmpty()) {
            LOG.warn("List of all display items is missing");
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

        final ByteBuffer buf = ByteBuffer.allocate(4 + numItems * 12);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_CREATE);
        buf.put(menuType);
        buf.put((byte) numItems);
        buf.put((byte) 0x00);

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

            final String id = idMap.containsKey(name) ? idMap.get(name) : name;
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
            buf.put((byte) (id.equals("00000013") ? 1 : 0));
        }

        // Set all disabled items
        pos = 0;
        for (final String name : allSettings) {
            if (enabledList.contains(name) || name.equals("more")) {
                continue;
            }

            final String id = idMap.containsKey(name) ? idMap.get(name) : name;
            if (!ID_REGEX.matcher(id).find()) {
                LOG.error("Screen item id '{}' is not 8-char hex string", id);
                continue;
            }

            // Screen IDs are sent as literal hex strings
            buf.put(id.getBytes(StandardCharsets.UTF_8));
            buf.put((byte) 0);
            buf.put(DISPLAY_ITEMS_SECTION_DISABLED);
            buf.put(pos++);
            buf.put((byte) (id.equals("00000013") ? 1 : 0));
        }

        write(builder, buf.array());
    }
}
