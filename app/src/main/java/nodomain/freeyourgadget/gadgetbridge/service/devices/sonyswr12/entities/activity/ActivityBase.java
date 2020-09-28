package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public abstract class ActivityBase {
    protected final ActivityType type;
    private final long timeStampSec;

    public ActivityBase(ActivityType type, int timeOffsetMin, long timeStampSec) {
        if (timeOffsetMin < 0 || timeOffsetMin > 1440) {
            throw new IllegalArgumentException("activity time offset out of range: " + timeOffsetMin);
        }
        this.type = type;
        this.timeStampSec = timeStampSec + timeOffsetMin * 60;
    }

    public final int getTimeStampSec() {
        return (int) (timeStampSec);
    }

    public final ActivityType getType() {
        return this.type;
    }
}