package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


public  class HPlusDataRecord {
    public final static int TYPE_SLEEP = 1;
    public int activityKind = ActivityKind.TYPE_UNKNOWN;

    /**
     * Time of this record in seconds
     */
    public int timestamp;

    /**
     * Raw data as sent from the device
     */
    public byte[] rawData;

    public HPlusDataRecord(byte[] data){
        rawData = data;
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
