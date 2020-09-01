package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitWriter;

public class ActivityHeartRate extends ActivityBase {
    public final int bpm;

    public ActivityHeartRate(int timeOffsetMin, int bpm, Long timeStampSec) {
        super(ActivityType.HEART_RATE, timeOffsetMin, timeStampSec);
        if (bpm < 0 || bpm > 65535) {
            throw new IllegalArgumentException("bpm out of range: " + bpm);
        }
        this.bpm = bpm;
    }

    @Override
    public long toLong() {
        UIntBitWriter writerWithTypeAndOffset = this.getWriterWithTypeAndOffset();
        writerWithTypeAndOffset.append(16, this.bpm);
        return writerWithTypeAndOffset.getValue();
    }
}