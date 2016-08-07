package nodomain.freeyourgadget.gadgetbridge.entities;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public abstract class AbstractPebbleMorpheuzActivitySample extends AbstractActivitySample {

    @Override
    public int getKind() {
        int rawIntensity = getRawIntensity();
        if (rawIntensity <= 120) {
            return ActivityKind.TYPE_DEEP_SLEEP;
        } else if (rawIntensity <= 1000) {
            return ActivityKind.TYPE_LIGHT_SLEEP;
        }
        return ActivityKind.TYPE_ACTIVITY;
    }
}
