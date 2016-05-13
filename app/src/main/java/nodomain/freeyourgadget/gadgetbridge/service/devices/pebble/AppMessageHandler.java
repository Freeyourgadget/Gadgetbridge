package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;


import android.util.Pair;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySample;

public class AppMessageHandler {
    protected final PebbleProtocol mPebbleProtocol;
    protected final UUID mUUID;

    AppMessageHandler(UUID uuid, PebbleProtocol pebbleProtocol) {
        mUUID = uuid;
        mPebbleProtocol = pebbleProtocol;
    }

    public UUID getUUID() {
        return mUUID;
    }

    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        return null;
    }

    public GBDeviceEvent[] pushMessage() {
        return null;
    }

    protected PebbleActivitySample createSample(int timestamp, int intensity, int steps, int type) {
        // TODO: user and device id
        Long userId = null;
        Long deviceId = null;
        return new PebbleActivitySample(null, timestamp, intensity, steps, type, userId, deviceId);
    }
}