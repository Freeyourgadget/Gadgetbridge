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
import java.security.InvalidParameterException;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class PlayNotificationRequest extends Request {

    public enum VibrationType{
        SINGLE_SHORT(3),
        DOUBLE_SHORT(2),
        TRIPLE_SHORT(1),
        SINGLE_NORMAL(5),
        DOUBLE_NORMAL(6),
        TRIPLE_NORMAL(7),
        SINGLE_LONG(8),
        NO_VIBE(9);

        private byte value;

        VibrationType(int value) {
            this.value = (byte)value;
        }

        public static VibrationType fromValue(byte value){
            for(VibrationType type : values()){
                if(type.getValue() == value) return type;
            }
            throw new InvalidParameterException("vibration Type not supported");
        }

        public byte getValue() {
            return value;
        }
    }

    public PlayNotificationRequest(VibrationType vibrationType, int degreesHour, int degreesMins, int degreesActivityHand){
        int length = 0;
        if(degreesHour > -1) length++;
        if(degreesMins > -1) length++;
        if(degreesActivityHand > -1) length++;
        ByteBuffer buffer = createBuffer(length * 2 + 10);
        buffer.put(vibrationType.getValue());
        buffer.put((byte)5);
        buffer.put((byte)(length * 2 + 2));
        buffer.putShort((short)0);
        if(degreesHour > -1){
            buffer.putShort((short) ((degreesHour % 360) | (1 << 12)));
        }
        if(degreesMins > -1){
            buffer.putShort((short)((degreesMins % 360) | (2 << 12)));
        }
        if(degreesActivityHand > -1) {
            buffer.putShort((short)((degreesActivityHand % 360) | (3 << 12)));
        }
        this.data = buffer.array();
    }



    public PlayNotificationRequest(VibrationType vibrationType, int degreesHour, int degreesMins){
        this(vibrationType, degreesHour, degreesMins, -1);
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 7, 15, 10, 1};
    }
}
