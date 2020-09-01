package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.SonySWR12Util;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.IntFormat;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public class EventWithActivity extends EventBase {
    public final long timeStampSec;
    public final List<ActivityBase> activityList;

    private EventWithActivity(long timeStampSec, List<ActivityBase> activityList) {
        super(EventCode.ACTIVITY_DATA);
        this.timeStampSec = timeStampSec;
        this.activityList = activityList;
    }

    public static EventWithActivity fromByteArray(ByteArrayReader byteArrayReader) {
        long timeOffset = byteArrayReader.readInt(IntFormat.UINT32);
        long timeStampSec = SonySWR12Util.secSince2013() + timeOffset;
        ArrayList<ActivityBase> activities = new ArrayList<>();
        while (byteArrayReader.getBytesLeft() > 0) {
            UIntBitReader uIntBitReader = new UIntBitReader(byteArrayReader.readInt(IntFormat.UINT32), 32);
            ActivityType activityType = ActivityType.fromInt(uIntBitReader.read(4));
            int offsetMin = uIntBitReader.read(12);
            ActivityBase activityPayload;
            switch (activityType) {
                case SLEEP: {
                    SleepLevel sleepLevel = SleepLevel.fromInt(uIntBitReader.read(2));
                    int duration = uIntBitReader.read(14);
                    activityPayload = new ActivitySleep(offsetMin, duration, sleepLevel, timeStampSec);
                    break;
                }
                case HEART_RATE: {
                    int bpm = uIntBitReader.read(16);
                    activityPayload = new ActivityHeartRate(offsetMin, bpm, timeStampSec);
                    break;
                }
                default: {
                    int data = uIntBitReader.read(16);
                    activityPayload = new ActivityWithData(activityType, offsetMin, data, timeStampSec);
                    break;
                }
            }
            activities.add(activityPayload);
        }
        return new EventWithActivity(timeStampSec, activities);
    }

    public byte[] toByteArray() {
        ByteArrayWriter byteArrayWriter = this.getValueWriter();
        byteArrayWriter.appendUint32(this.timeStampSec);
        for (ActivityBase activity : activityList){
            byteArrayWriter.appendUint32(activity.toLong());
        }
        return byteArrayWriter.getByteArray();
    }
}
