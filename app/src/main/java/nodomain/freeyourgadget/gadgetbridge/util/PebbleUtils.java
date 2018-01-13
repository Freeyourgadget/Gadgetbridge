/*  Copyright (C) 2016-2017 Andreas Shimokawa, Daniele Gobbetti, Frank Slezak

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.graphics.Color;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

public class PebbleUtils {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleUtils.class);

    public static String getPlatformName(String hwRev) {
        String platformName;
        if (hwRev.startsWith("snowy")) {
            platformName = "basalt";
        } else if (hwRev.startsWith("spalding")) {
            platformName = "chalk";
        } else if (hwRev.startsWith("silk")) {
            platformName = "diorite";
        } else if (hwRev.startsWith("robert")) {
            platformName = "emery";
        } else {
            platformName = "aplite";
        }
        return platformName;
    }

    public static String getModel(String hwRev) {
        //TODO: get real data?
        String model;
        if (hwRev.startsWith("snowy")) {
            model = "pebble_time_black";
        } else if (hwRev.startsWith("spalding")) {
            model = "pebble_time_round_black_20mm";
        } else if (hwRev.startsWith("silk")) {
            model = "pebble2_black";
        } else if (hwRev.startsWith("robert")) {
            model = "pebble_time2_black";
        } else {
            model = "pebble_black";
        }
        return model;
    }

    public static int getFwMajor(String fwString) {
        return fwString.charAt(1) - 48;
    }

    public static boolean hasHRM(String hwRev) {
        String platformName = getPlatformName(hwRev);
        return "diorite".equals(platformName) || "emery".equals(platformName);
    }

    public static boolean hasHealth(String hwRev) {
        String platformName = getPlatformName(hwRev);
        return !"aplite".equals(platformName);
    }

    /**
     * Get the closest Pebble-compatible color from the associated Android Color Integer.
     * @param color An Android Color Integer to convert
     * @return A byte representing the closest Pebble color.
     */
    public static byte getPebbleColor(int color) {
        // 85 here is determined by dividing 255 by 3, or reducing an 8-bit color to a 2-bit color. (2^3 = 8)

        int colorRed = ((color >> 16) & 0xFF) / 85;
        int colorGreen = ((color >> 8) & 0xFF) / 85;
        int colorBlue = (color & 0xFF) / 85;

        // Bit shifting, woo!
        return (byte) ((0b11 << 6) | ((colorRed & 0b11) << 4) | ((colorGreen & 0b11) << 2) | (colorBlue & 0b11));
    }

    /**
     * Get the closest Pebble-compatible color from the associated Hex string.
     * @param colorHex A Hex-formatted string (#FFDD00) to convert.
     * @return A byte representing the closest Pebble color.
     */
    public static byte getPebbleColor(String colorHex) {
        return getPebbleColor(Color.parseColor(colorHex));
    }


    /**
     * Returns the directory containing the .pbw cache.
     * @throws IOException when the external files directory cannot be accessed
     */
    public static File getPbwCacheDir() throws IOException {
        return new File(FileUtils.getExternalFilesDir(), "pbw-cache");
    }

    public static JSONObject getAppConfigurationKeys(UUID uuid) {
        try {
            File destDir = getPbwCacheDir();
            File configurationFile = new File(destDir, uuid.toString() + ".json");
            if (configurationFile.exists()) {
                String jsonString = FileUtils.getStringFromFile(configurationFile);
                JSONObject json = new JSONObject(jsonString);
                return json.getJSONObject("appKeys");
            }
        } catch (IOException | JSONException e) {
            LOG.warn("Unable to parse configuration JSON file", e);
        }
        return null;
    }

    public static String parseIncomingAppMessage(String msg, UUID uuid) {
        JSONObject jsAppMessage = new JSONObject();

        JSONObject knownKeys = PebbleUtils.getAppConfigurationKeys(uuid);
        SparseArray<String> appKeysMap = new SparseArray<>();

        if (knownKeys == null || msg == null) {
            return "{}";
        }

        String inKey, outKey;
        //knownKeys contains "name"->"index", we need to reverse that
        for (Iterator<String> key = knownKeys.keys(); key.hasNext(); ) {
            inKey = key.next();
            appKeysMap.put(knownKeys.optInt(inKey), inKey);
        }

        try {
            JSONArray incoming = new JSONArray(msg);
            JSONObject outgoing = new JSONObject();
            for (int i = 0; i < incoming.length(); i++) {
                JSONObject in = incoming.getJSONObject(i);
                outKey = null;
                Object outValue = null;
                for (Iterator<String> key = in.keys(); key.hasNext(); ) {
                    inKey = key.next();
                    switch (inKey) {
                        case "key":
                            outKey = appKeysMap.get(in.optInt(inKey));
                            break;
                        case "value":
                            outValue = in.get(inKey);
                    }
                }
                if (outKey != null && outValue != null) {
                    outgoing.put(outKey, outValue);
                }
            }
            jsAppMessage.put("payload", outgoing);

        } catch (Exception e) {
            LOG.warn("Unable to parse incoming app message", e);
        }
        return jsAppMessage.toString();
    }
}
