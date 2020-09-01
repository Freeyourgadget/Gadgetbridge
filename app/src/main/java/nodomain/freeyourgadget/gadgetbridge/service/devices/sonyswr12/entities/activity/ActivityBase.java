package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitWriter;

public abstract class ActivityBase {
    protected final ActivityType type;
    protected final int timeOffsetMin;
    private final long timeStampSec;

    public ActivityBase(ActivityType type, int timeOffsetMin, long timeStampSec) {
        if (timeOffsetMin < 0 || timeOffsetMin > 1440) {
            throw new IllegalArgumentException("activity time offset out of range: " + timeOffsetMin);
        }
        this.type = type;
        this.timeOffsetMin = timeOffsetMin;
        this.timeStampSec = timeStampSec + this.timeOffsetMin * 60;
    }

    public final int getTimeStampSec() {
        return (int) (timeStampSec);
    }

    public final ActivityType getType() {
        return this.type;
    }

    protected final UIntBitWriter getWriterWithTypeAndOffset() {
        UIntBitWriter uIntBitWriter = new UIntBitWriter(32);
        uIntBitWriter.append(4, this.type.value);
        uIntBitWriter.append(12, this.timeOffsetMin);
        return uIntBitWriter;
    }

    public abstract long toLong();
}