/*  Copyright (C) 2019-2021 Daniel Dakhno

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
    public MoveHandsRequest(MovementConfiguration movement){
        init(movement);
    }

    private void init(MovementConfiguration movement) {
        int count = 0;
        if(movement.isHourSet()) count++;
        if(movement.isMinuteSet()) count++;
        if(movement.isSubSet()) count++;

        ByteBuffer buffer = createBuffer(count * 5 + 5);
        buffer.put(movement.isRelativeMovement() ? 1 : (byte)2);
        buffer.put((byte)count);

        if(movement.isHourSet()){
            buffer.put((byte)1);
            buffer.putShort((short)Math.abs(movement.getHourDegrees()));
            if(movement.isRelativeMovement()){
                buffer.put(movement.getHourDegrees() >= 0 ? (byte) 1 : (byte) 2);
            }else {
                buffer.put((byte)3);
            }
            buffer.put((byte)1);
        }

        if(movement.isMinuteSet()){
            buffer.put((byte)2);
            buffer.putShort((short)Math.abs(movement.getMinuteDegrees()));
            if(movement.isRelativeMovement()){
                buffer.put(movement.getMinuteDegrees() >= 0 ? (byte) 1 : (byte) 2);
            }else {
                buffer.put((byte)3);
            }
            buffer.put((byte)1);
        }

        if(movement.isSubSet()){
            buffer.put((byte)3);
            buffer.putShort((short)Math.abs(movement.getSubDegrees()));
            if(movement.isRelativeMovement()){
                buffer.put(movement.getSubDegrees() >= 0 ? (byte) 1 : (byte) 2);
            }else {
                buffer.put((byte)3);
            }
            buffer.put((byte)1);
        }

        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 21, 3};
    }

    static public class MovementConfiguration{
        private int hourDegrees, minuteDegrees, subDegrees;
        private boolean hourSet = false, minuteSet = false, subSet = false;
        private boolean relativeMovement;

        public MovementConfiguration(boolean relativeMovement) {
            this.relativeMovement = relativeMovement;
        }

        public boolean isRelativeMovement() {
            return relativeMovement;
        }

        public void setRelativeMovement(boolean relativeMovement) {
            this.relativeMovement = relativeMovement;
        }

        public int getHourDegrees() {
            return hourDegrees;
        }

        public void setHourDegrees(int hourDegrees) {
            this.hourDegrees = hourDegrees;
            this.hourSet = true;
        }

        public int getMinuteDegrees() {
            return minuteDegrees;
        }

        public void setMinuteDegrees(int minuteDegrees) {
            this.minuteDegrees = minuteDegrees;
            this.minuteSet = true;
        }

        public int getSubDegrees() {
            return subDegrees;
        }

        public void setSubDegrees(int subDegrees) {
            this.subDegrees = subDegrees;
            this.subSet = true;
        }

        public boolean isHourSet() {
            return hourSet;
        }

        public boolean isMinuteSet() {
            return minuteSet;
        }

        public boolean isSubSet() {
            return subSet;
        }
    }
}
