package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;


import android.util.Pair;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

class AppMessageHandler {
    final PebbleProtocol mPebbleProtocol;
    final UUID mUUID;

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
}