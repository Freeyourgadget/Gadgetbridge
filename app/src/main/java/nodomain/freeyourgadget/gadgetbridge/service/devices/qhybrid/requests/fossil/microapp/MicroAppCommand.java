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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.microapp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface MicroAppCommand {
    byte[] getData();
}

class StartCriticalCommand implements MicroAppCommand{
    @Override
    public byte[] getData() {
        return new byte[]{(byte) 0x03, (byte) 0x00};
    }
}

class CloseCommand implements MicroAppCommand{
    @Override
    public byte[] getData() {
        return new byte[]{(byte) 0x01, (byte) 0x00};
    }
}

class DelayCommand implements MicroAppCommand{
    private double delayInSeconds;

    public DelayCommand(double delayInSeconds) {
        this.delayInSeconds = delayInSeconds;
    }

    @Override
    public byte[] getData() {
        return ByteBuffer.wrap(new byte[]{0x08, 0x01, 0x00, 0x00})
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(2, (short)(delayInSeconds * 10f))
                .array();
    }
}

enum VibrationType{
    NORMAL((byte) 0x04);

    private byte value;

    VibrationType(byte value){
        this.value = value;
    }

    public byte getValue(){ return this.value; }
}

class VibrateCommand implements MicroAppCommand{
    private VibrationType vibrationType;

    public VibrateCommand(VibrationType vibrationType) {
        this.vibrationType = vibrationType;
    }

    @Override
    public byte[] getData() {
        return ByteBuffer.wrap(new byte[]{(byte) 0x93, 0x00, 0x00})
                .put(2, vibrationType.getValue())
                .array();
    }
}

enum MovingDirection{
    CLOCKWISE((byte) 0x00),
    COUNTER_CLOCKWISE((byte) 0x01),
    SHORTEST((byte) 0x02),
    ;
    private byte value;

    MovingDirection(byte value){ this.value = value; }

    public byte getValue() {
        return value;
    }
}

enum MovingSpeed{
    MAX((byte) 0x00),
    HALF((byte) 0x01),
    QUARTER((byte) 0x02),
    EIGHTH((byte) 0x03),
    SIXTEENTH((byte) 0x04),
    ;
    private byte value;

    MovingSpeed(byte value){ this.value = value; }

    public byte getValue() {
        return value;
    }
}

class StreamCommand implements MicroAppCommand{
    private byte type;

    public StreamCommand(byte type) {
        this.type = type;
    }


    @Override
    public byte[] getData() {
        return new byte[]{(byte) 0x8B, (byte) 0x00, this.type};
    }
}

class RepeatStartCommand implements MicroAppCommand{
    private byte count;

    public RepeatStartCommand(byte count) {
        this.count = count;
    }


    @Override
    public byte[] getData() {
        return new byte[]{(byte) 0x86, (byte) 0x00, this.count};
    }
}

class RepeatStopCommand implements MicroAppCommand{
    @Override
    public byte[] getData() {
        return new byte[]{(byte) 0x07, (byte) 0x00};
    }
}

class AnimationCommand implements MicroAppCommand{
    private short hour, minute;
    private MovingDirection direction;
    private MovingSpeed speed;
    private byte absoluteMovementFlag;

    public AnimationCommand(short hour, short minute) {
        this.hour = hour;
        this.minute = minute;
        this.speed = MovingSpeed.MAX;
        this.direction = MovingDirection.SHORTEST;
        this.absoluteMovementFlag = 1;
    }

    @Override
    public byte[] getData() {
        return ByteBuffer.allocate(10)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0x09)
                .put((byte) 0x04)
                .put((byte) 0x01)
                .put((byte) 0x03)
                .put((byte) ((direction.getValue() << 6) | (byte)(absoluteMovementFlag << 5) | this.speed.getValue()))
                .putShort(this.hour)
                .put((byte) ((direction.getValue() << 6) | (byte)(absoluteMovementFlag << 5) | this.speed.getValue()))
                .putShort(this.minute)
                .array();
    }
}
