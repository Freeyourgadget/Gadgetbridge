package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.parser;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.HybridHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class ActivityEntry {
    public int id;
    public int heartRate;
    public int variability, maxVariability;
    public int calories;
    public int stepCount;
    public boolean isActive;

    public int timestamp;

    public int heartRateQuality;

    public WEARING_STATE wearingState;

    public HybridHRActivitySample toDAOActivitySample(long userId, long deviceId) {
        HybridHRActivitySample sample = new HybridHRActivitySample(
                timestamp,
                deviceId,
                userId,
                stepCount,
                calories,
                variability,
                maxVariability,
                heartRateQuality,
                isActive,
                wearingState.value,
                heartRate
        );

        return sample;
    }

    public enum WEARING_STATE{
        WEARING((byte) 0, ActivityKind.TYPE_NOT_MEASURED),
        NOT_WEARING((byte) 1, ActivityKind.TYPE_NOT_WORN),
        UNKNOWN((byte) 2, ActivityKind.TYPE_UNKNOWN);

        byte value;
        int activityKind;

        WEARING_STATE(byte value, int activityKind){
            this.value = value;
            this.activityKind = activityKind;
        }

        public int getActivityKind() {
            return activityKind;
        }

        static public WEARING_STATE fromValue(byte value){
            switch (value){
                case 0: return WEARING_STATE.WEARING;
                case 1: return WEARING_STATE.NOT_WEARING;
                case 2: return WEARING_STATE.UNKNOWN;
                default: throw new RuntimeException("value " + value + " not valid state value");
            }
        }
    }
}
