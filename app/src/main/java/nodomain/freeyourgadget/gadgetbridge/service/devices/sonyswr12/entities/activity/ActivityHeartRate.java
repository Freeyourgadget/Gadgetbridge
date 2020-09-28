package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public class ActivityHeartRate extends ActivityBase {
    public final int bpm;

    public ActivityHeartRate(int timeOffsetMin, int bpm, Long timeStampSec) {
        super(ActivityType.HEART_RATE, timeOffsetMin, timeStampSec);
        if (bpm < 0 || bpm > 65535) {
            throw new IllegalArgumentException("bpm out of range: " + bpm);
        }
        this.bpm = bpm;
    }
}