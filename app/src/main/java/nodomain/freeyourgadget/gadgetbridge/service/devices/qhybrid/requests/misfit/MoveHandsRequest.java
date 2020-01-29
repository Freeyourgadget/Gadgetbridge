/*  Copyright (C) 2019-2020 Daniel Dakhno

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;


import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class MoveHandsRequest extends Request {
    public MoveHandsRequest(boolean moveRelative, short degreesMin, short degreesHour, short degreesSub){
        init(moveRelative, degreesMin, degreesHour, degreesSub);
    }

    private void init(boolean moveRelative, short degreesMin, short degreesHour, short degreesSub) {
        int count = 0;
        if(degreesHour != -1) count++;
        if(degreesMin != -1) count++;
        if(degreesSub != -1) count++;

        ByteBuffer buffer = createBuffer(count * 5 + 5);
        buffer.put(moveRelative ? 1 : (byte)2);
        buffer.put((byte)count);

        if(degreesHour > -1){
            buffer.put((byte)1);
            buffer.putShort(degreesHour);
            buffer.put((byte)3);
            buffer.put((byte)1);
        }
        if(degreesMin > -1){
            buffer.put((byte)2);
            buffer.putShort(degreesMin);
            buffer.put((byte)3);
            buffer.put((byte)1);
        }
        if(degreesSub > -1){
            buffer.put((byte)3);
            buffer.putShort(degreesSub);
            buffer.put((byte)3);
            buffer.put((byte)1);
        }

        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 21, 3};
    }
}
