/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HuaweiTruSleepParser {

    public static class TruSleepStatus {
        public final int startTime;
        public final int endTime;

        public TruSleepStatus(int startTime, int endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TruSleepStatus that = (TruSleepStatus) o;
            return startTime == that.startTime && endTime == that.endTime;
        }

        @Override
        public String toString() {
            return "TruSleepStatus{" +
                    "endTime=" + endTime +
                    ", startTime=" + startTime +
                    '}';
        }
    }

    public static TruSleepStatus[] parseState(byte[] stateData) {
        /*
            Format:
             - Start time (int)
             - End time (int)
             - Unknown (short)
             - Unknown (byte)
             - Padding (5 bytes)
            Could be multiple available
         */
        ByteBuffer buffer = ByteBuffer.wrap(stateData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        TruSleepStatus[] retv = new TruSleepStatus[buffer.remaining() / 0x10];
        int c = 0;
        while (stateData.length - buffer.position() >= 0x10) {
            int startTime = buffer.getInt();
            int endTime = buffer.getInt();
            // Throw away for now because we don't know what it means, and we don't think we can implement this soon
            buffer.get(); buffer.get(); buffer.get();
            buffer.get(); buffer.get(); buffer.get(); buffer.get(); buffer.get();

            retv[c++] = new TruSleepStatus(startTime, endTime);
        }
        return retv;
    }
}
