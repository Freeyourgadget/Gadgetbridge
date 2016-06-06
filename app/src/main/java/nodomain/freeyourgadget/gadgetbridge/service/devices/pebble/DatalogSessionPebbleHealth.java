package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

abstract class DatalogSessionPebbleHealth extends DatalogSession {

    DatalogSessionPebbleHealth(byte id, UUID uuid, int tag, byte itemType, short itemSize) {
        super(id, uuid, tag, itemType, itemSize);
    }

    protected boolean isPebbleHealthEnabled() {
        Prefs prefs = GBApplication.getPrefs();
        int activityTracker = prefs.getInt("pebble_activitytracker", SampleProvider.PROVIDER_PEBBLE_HEALTH);
        return (activityTracker == SampleProvider.PROVIDER_PEBBLE_HEALTH);
    }
}