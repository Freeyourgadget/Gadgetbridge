package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;


import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

class AppMessageHandler {
    final PebbleProtocol mPebbleProtocol;
    final UUID mUUID;
    protected Map<String, Integer> messageKeys;

    AppMessageHandler(UUID uuid, PebbleProtocol pebbleProtocol) {
        mUUID = uuid;
        mPebbleProtocol = pebbleProtocol;
    }

    public boolean isEnabled() {
        return true;
    }

    public UUID getUUID() {
        return mUUID;
    }

    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        // Just ACK
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);
        return new GBDeviceEvent[]{sendBytesAck};
    }

    public GBDeviceEvent[] onAppStart() {
        return null;
    }

    public byte[] encodeUpdateWeather(WeatherSpec weatherSpec) {
        return null;
    }

    protected GBDevice getDevice() {
        return mPebbleProtocol.getDevice();
    }

    protected JSONObject getAppKeys() throws IOException, JSONException {
        File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
        File configurationFile = new File(destDir, mUUID.toString() + ".json");
        if (configurationFile.exists()) {
            String jsonstring = FileUtils.getStringFromFile(configurationFile);
            JSONObject json = new JSONObject(jsonstring);
            return json.getJSONObject("appKeys");
        }
        throw new IOException();
    }
}