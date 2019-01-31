/*  Copyright (C) 2017-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview;

import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.UUID;

import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;

public class JSInterface {

    private UUID mUuid;
    private GBDevice device;
    private Integer lastTransaction;

    private static final Logger LOG = LoggerFactory.getLogger(JSInterface.class);

    public JSInterface(@NonNull GBDevice device, @NonNull UUID mUuid) {
        LOG.debug("Creating JS interface for UUID: " + mUuid.toString());
        this.device = device;
        this.mUuid = mUuid;
        this.lastTransaction = 0;
    }


    private boolean isLocationEnabledForWatchApp() {
        return true; //FIXME: as long as we don't give watchapp internet access it's not a problem
    }

    @JavascriptInterface
    public void gbLog(String msg) {
        LOG.debug("WEBVIEW webpage log: " + msg);
    }

    @JavascriptInterface
    public String sendAppMessage(String msg, String needsTransactionMsg) {
        boolean needsTransaction = "true".equals(needsTransactionMsg);
        LOG.debug("from WEBVIEW: " + msg + " needs a transaction: " + needsTransaction);
        JSONObject knownKeys = PebbleUtils.getAppConfigurationKeys(this.mUuid);
        if (knownKeys == null) {
            LOG.warn("No app configuration keys for: " + mUuid);
            return null;
        }

        try {
            JSONObject in = new JSONObject(msg);
            JSONObject out = new JSONObject();
            String inKey, outKey;
            boolean passKey;
            for (Iterator<String> key = in.keys(); key.hasNext(); ) {
                passKey = false;
                inKey = key.next();
                outKey = null;
                int pebbleAppIndex = knownKeys.optInt(inKey, -1);
                if (pebbleAppIndex != -1) {
                    passKey = true;
                    outKey = String.valueOf(pebbleAppIndex);
                } else {
                    //do not discard integer keys (see https://developer.pebble.com/guides/communication/using-pebblekit-js/ )
                    Scanner scanner = new Scanner(inKey);
                    if (scanner.hasNextInt() && inKey.equals("" + scanner.nextInt())) {
                        passKey = true;
                        outKey = inKey;
                    }
                }

                if (passKey) {
                    Object obj = in.get(inKey);
                    out.put(outKey, obj);
                } else {
                    GB.toast("Discarded key " + inKey + ", not found in the local configuration and is not an integer key.", Toast.LENGTH_SHORT, GB.WARN);
                }

            }
            LOG.info("WEBVIEW message to pebble: " + out.toString());
            if (needsTransaction) {
                this.lastTransaction++;
                GBApplication.deviceService().onAppConfiguration(this.mUuid, out.toString(), this.lastTransaction);
                return this.lastTransaction.toString();
            } else {
                GBApplication.deviceService().onAppConfiguration(this.mUuid, out.toString(), null);
            }

        } catch (JSONException e) {
            LOG.warn("Error building the appmessage JSON object", e);
        }
        return null;
    }

    @JavascriptInterface
    public String getActiveWatchInfo() {
        JSONObject wi = new JSONObject();
        try {
            wi.put("firmware", device.getFirmwareVersion());
            wi.put("platform", PebbleUtils.getPlatformName(device.getModel()));
            wi.put("model", PebbleUtils.getModel(device.getModel()));
            //TODO: use real info
            wi.put("language", "en");
        } catch (JSONException e) {
            LOG.warn("Error building the ActiveWathcInfo JSON object", e);
        }
        //Json not supported apparently, we need to cast back and forth
        return wi.toString();
    }

    @JavascriptInterface
    public String getAppConfigurationFile() {
        LOG.debug("WEBVIEW loading config file of " + this.mUuid.toString());
        try {
            File destDir = PebbleUtils.getPbwCacheDir();
            File configurationFile = new File(destDir, this.mUuid.toString() + "_config.js");
            if (configurationFile.exists()) {
                return "file:///" + configurationFile.getAbsolutePath();
            }
        } catch (IOException e) {
            LOG.warn("Error loading config file", e);
        }
        return null;
    }

    @JavascriptInterface
    public String getAppStoredPreset() {
        try {
            File destDir = PebbleUtils.getPbwCacheDir();
            File configurationFile = new File(destDir, this.mUuid.toString() + "_preset.json");
            if (configurationFile.exists()) {
                return FileUtils.getStringFromFile(configurationFile);
            }
        } catch (IOException e) {
            GB.toast("Error reading presets", Toast.LENGTH_LONG, GB.ERROR);
            LOG.warn("Error reading presets", e);
        }
        return null;
    }

    @JavascriptInterface
    public void saveAppStoredPreset(String msg) {
        Writer writer;

        try {
            File destDir = PebbleUtils.getPbwCacheDir();
            File presetsFile = new File(destDir, this.mUuid.toString() + "_preset.json");
            writer = new BufferedWriter(new FileWriter(presetsFile));
            writer.write(msg);
            writer.close();
            GB.toast("Presets stored", Toast.LENGTH_SHORT, GB.INFO);
        } catch (IOException e) {
            GB.toast("Error storing presets", Toast.LENGTH_LONG, GB.ERROR);
            LOG.warn("Error storing presets", e);
        }
    }

    @JavascriptInterface
    public String getAppUUID() {
        return this.mUuid.toString();
    }

    @JavascriptInterface
    public String getAppLocalstoragePrefix() {
        String prefix = device.getAddress() + this.mUuid.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = prefix.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();
            final StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(String.format("%02X", aByte));
            }
            return sb.toString().toLowerCase();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            LOG.warn("Error definining local storage prefix", e);
            return prefix;
        }
    }

    @JavascriptInterface
    public String getWatchToken() {
        //specification says: A string that is guaranteed to be identical for each Pebble device for the same app across different mobile devices. The token is unique to your app and cannot be used to track Pebble devices across applications. see https://developer.pebble.com/docs/js/Pebble/
        return "gb" + this.mUuid.toString();
    }


    @JavascriptInterface
    public String getCurrentPosition() {
        if (!isLocationEnabledForWatchApp()) {
            return "";
        }
        //we need to override this because the coarse location is not enough for the android webview, we should add the permission for fine location.
        JSONObject geoPosition = new JSONObject();
        JSONObject coords = new JSONObject();
        try {

            CurrentPosition currentPosition = new CurrentPosition();

            geoPosition.put("timestamp", currentPosition.timestamp);

            coords.put("latitude", currentPosition.getLatitude());
            coords.put("longitude", currentPosition.getLongitude());
            coords.put("accuracy", currentPosition.accuracy);
            coords.put("altitude", currentPosition.altitude);
            coords.put("speed", currentPosition.speed);

            geoPosition.put("coords", coords);

        } catch (JSONException e) {
            LOG.warn(e.getMessage());
        }
        LOG.info("WEBVIEW - geo position" + geoPosition.toString());
        return geoPosition.toString();
    }

    @JavascriptInterface
    public void eventFinished(String event) {
        LOG.debug("WEBVIEW event finished: " + event);
    }
}
