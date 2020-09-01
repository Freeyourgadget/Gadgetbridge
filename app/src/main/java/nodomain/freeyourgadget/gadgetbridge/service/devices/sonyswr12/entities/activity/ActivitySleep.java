package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitWriter;

public class ActivitySleep extends ActivityBase {
    public final SleepLevel sleepLevel;
    public final int durationMin;

    public ActivitySleep(int timeOffsetMin, int durationMin, SleepLevel sleepLevel, Long timeStampSec) {
        super(ActivityType.SLEEP, timeOffsetMin, timeStampSec);
        this.durationMin = durationMin;
        this.sleepLevel = sleepLevel;
    }

    @Override
    public long toLong() {
        UIntBitWriter writerWithTypeAndOffset = this.getWriterWithTypeAndOffset();
        writerWithTypeAndOffset.append(14, this.durationMin);
        writerWithTypeAndOffset.append(2, this.sleepLevel.value);
        return writerWithTypeAndOffset.getValue();
    }
}