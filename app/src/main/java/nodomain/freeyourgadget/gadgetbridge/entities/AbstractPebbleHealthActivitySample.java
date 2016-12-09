package nodomain.freeyourgadget.gadgetbridge.entities;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public abstract class AbstractPebbleHealthActivitySample extends AbstractActivitySample {
    abstract public byte[] getRawPebbleHealthData();

    private transient int rawActivityKind = ActivityKind.TYPE_UNKNOWN;

    @Override
    public int getRawKind() {
        return rawActivityKind;
    }

    @Override
    public void setRawKind(int kind) {
        this.rawActivityKind = kind;
    }
}