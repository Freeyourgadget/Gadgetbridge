package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

public class ActivityWithData extends ActivityBase {
    public final int data;

    public ActivityWithData(ActivityType activityType, int timeOffsetMin, int data, Long timeStampSec) {
        super(activityType, timeOffsetMin, timeStampSec);
        if (data < 0 || data > 65535) {
            throw new IllegalArgumentException("data out of range: " + data);
        }
        this.data = data;
    }
}
