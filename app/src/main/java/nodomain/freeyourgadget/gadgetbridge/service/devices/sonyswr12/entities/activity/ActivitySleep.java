package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public class ActivitySleep extends ActivityBase {
    public final SleepLevel sleepLevel;
    public final int durationMin;

    public ActivitySleep(int timeOffsetMin, int durationMin, SleepLevel sleepLevel, Long timeStampSec) {
        super(ActivityType.SLEEP, timeOffsetMin, timeStampSec);
        this.durationMin = durationMin;
        this.sleepLevel = sleepLevel;
    }
}