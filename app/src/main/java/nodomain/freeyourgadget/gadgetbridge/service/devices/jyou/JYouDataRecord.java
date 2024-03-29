/*  Copyright (C) 2018-2024 Pavel Elagin

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
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
