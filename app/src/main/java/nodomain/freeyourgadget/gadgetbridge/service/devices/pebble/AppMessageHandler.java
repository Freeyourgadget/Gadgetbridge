package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;


import android.util.Pair;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AppMessageHandler {
    protected final PebbleProtocol mPebbleProtocol;
    protected final UUID mUUID;

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
        return null;
    }

    public GBDeviceEvent[] pushMessage() {
        return null;
    }

    protected PebbleActivitySample createSample(int timestamp, int intensity, int steps, int type, User user, Device device) {
        return new PebbleActivitySample(null, timestamp, intensity, steps, type, user.getId(), device.getId());
    }

    protected GBDevice getDevice() {
        return mPebbleProtocol.getDevice();
    }
}