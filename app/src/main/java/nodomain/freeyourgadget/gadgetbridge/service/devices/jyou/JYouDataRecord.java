package nodomain.freeyourgadget.gadgetbridge.service.devices.jyou;

/*
 * @author Pavel Elagin &lt;elagin.pasha@gmail.com&gt;
 */

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class JYouDataRecord {
    public final static int TYPE_UNKNOWN = 0;
    public final static int TYPE_SLEEP = 100;
    public final static int TYPE_DAY_SUMMARY = 101;
    public final static int TYPE_DAY_SLOT = 102;
    public final static int TYPE_REALTIME = 103;

    public int type = TYPE_UNKNOWN;
    public int activityKind = ActivityKind.TYPE_UNKNOWN;

    /**
     * Time of this record in seconds
     */
    public int timestamp;

    /**
     * Raw data as sent from the device
     */
    public byte[] rawData;

    protected JYouDataRecord(){

    }

    protected JYouDataRecord(byte[] data, int type){
        this.rawData = data;
        this.type = type;
    }

    public byte[] getRawData() {

        return rawData;
    }

    public class RecordInterval {
        /**
         * Start time of this interval in seconds
         */
        public int timestampFrom;

        /**
         * End time of this interval in seconds
         */
        public int timestampTo;

        /**
         * Type of activity {@link ActivityKind}
         */
        public int activityKind;

        RecordInterval(int timestampFrom, int timestampTo, int activityKind) {
            this.timestampFrom = timestampFrom;
            this.timestampTo = timestampTo;
            this.activityKind = activityKind;
        }
    }
}
