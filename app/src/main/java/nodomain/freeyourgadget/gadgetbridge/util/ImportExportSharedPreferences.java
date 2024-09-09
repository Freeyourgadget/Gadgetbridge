/*  Copyright (C) 2017-2024 Alberto, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Joel Beckmeyer, José Rebelo, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.util;


import android.content.SharedPreferences;
import android.util.Xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.test.TestDeviceConst;

@Deprecated // use JsonBackupPreferences
public class ImportExportSharedPreferences {
    private static final Logger LOG = LoggerFactory.getLogger(ImportExportSharedPreferences.class);

    private static final String BOOLEAN = Boolean.class.getSimpleName();
    private static final String FLOAT = Float.class.getSimpleName();
    private static final String INTEGER = Integer.class.getSimpleName();
    private static final String LONG = Long.class.getSimpleName();
    private static final String STRING = String.class.getSimpleName();
    private static final String HASHSET = HashSet.class.getSimpleName();

    private static final String NAME = "name";
    private static final String PREFERENCES = "preferences";

    public static void exportToFile(SharedPreferences sharedPreferences, File outFile) throws IOException {
        try (FileWriter outputWriter = new FileWriter(outFile)) {
            export(sharedPreferences, outputWriter);
        }
    }

    public static void export(SharedPreferences sharedPreferences, Writer writer) throws IOException {
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(writer);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startDocument("UTF-8", true);
        serializer.startTag("", PREFERENCES);
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            String key = entry.getKey();

            Object valueObject = entry.getValue();
            // Skip this entry if the value is null;
            if (valueObject == null) continue;

            String valueType = valueObject.getClass().getSimpleName();
            String value = valueObject.toString();
            serializer.startTag("", valueType);
            serializer.attribute("", NAME, key);
            serializer.text(value);
            serializer.endTag("", valueType);
        }
        serializer.endTag("", PREFERENCES);
        serializer.endDocument();
    }

    public static boolean importFromFile(SharedPreferences sharedPreferences, File inFile)
            throws Exception {
        return importFromReader(sharedPreferences, new FileReader(inFile));
    }

    public static boolean importFromReader(SharedPreferences sharedPreferences, Reader in)
            throws Exception {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in);
        int eventType = parser.getEventType();
        String name = null;
        String key = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    key = parser.getAttributeValue("", NAME);
                    break;
                case XmlPullParser.TEXT:
                    // The parser is reading text outside an element if name is null,
                    // so simply ignore this text part (which is usually something like '\n')
                    if (name == null) break;
                    String text = parser.getText();
                    if (BOOLEAN.equals(name)) {
                        editor.putBoolean(key, Boolean.parseBoolean(text));
                    } else if (FLOAT.equals(name)) {
                        editor.putFloat(key, Float.parseFloat(text));
                    } else if (INTEGER.equals(name)) {
                        editor.putInt(key, Integer.parseInt(text));
                    } else if (LONG.equals(name)) {
                        editor.putLong(key, Long.parseLong(text));
                    } else if (STRING.equals(name)) {
                        editor.putString(key, text);
                    } else if (HASHSET.equals(name)) {
                        // FIXME: We can only deserialize values that are guaranteed to not contain commas,
                        // spaces at the end or start, square brackets
                        switch (key) {
                            case GBPrefs.PACKAGE_BLACKLIST:
                                GBApplication.setAppsNotifBlackList(stringToSet(text), editor);
                                break;
                            case GBPrefs.PACKAGE_PEBBLEMSG_BLACKLIST:
                                GBApplication.setAppsPebbleBlackList(stringToSet(text), editor);
                                break;
                            // @array/device_action_values
                            case DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_FELL_SLEEP_SELECTIONS:
                            case DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_START_NON_WEAR_SELECTIONS:
                            case DeviceSettingsPreferenceConst.PREF_DEVICE_ACTION_WOKE_UP_SELECTIONS:
                            // GarminCapability enum
                            case GarminPreferences.PREF_GARMIN_CAPABILITIES:
                            // mac addresses
                            case "dashboard_devices_multiselect":
                            case GBPrefs.LAST_DEVICE_ADDRESSES:
                            // display items
                            case "bip_display_items":
                            case "cor_display_items":
                            case "mi2_display_items":
                            case "miband3_display_items":
                            case HuamiConst.PREF_DISPLAY_ITEMS:
                            // @array/pref_amazfitneo_sounds_values
                            case DeviceSettingsPreferenceConst.PREF_SOUNDS:
                            // TestFeature enum
                            case TestDeviceConst.PREF_TEST_FEATURES:
                            // Ignored due to unsafe values
                            //case GBPrefs.CALENDAR_BLACKLIST: // user-controlled names
                            //case LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_GROUPS: // user-controlled names
                            //case MiBandConst.PREF_MIBAND_ALARMS: // unknown potential values
                            //case "casio_features_current_values": // unknown potential values
                                editor.putStringSet(key, stringToSet(text));
                                break;
                            default:
                                LOG.warn("Unknown hashset preference {}, will not import", key);
                        }
                    } else if (!PREFERENCES.equals(name)) {
                        throw new Exception("Unknown type " + name + " for pref " + key);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = null;
                    break;
            }
            eventType = parser.next();
        }
        return editor.commit();
    }

    private static Set<String> stringToSet(final String text) {
        final Set<String> ret = new HashSet<>();
        final String[] split = text.replace("[", "")
                .replace("]", "")
                .split(",");
        for (final String s : split) {
            ret.add(s.trim());
        }
        return ret;
    }
}
