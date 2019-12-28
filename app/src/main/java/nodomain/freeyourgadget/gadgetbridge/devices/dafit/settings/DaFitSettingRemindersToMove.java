/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings;

import java.nio.ByteBuffer;

public class DaFitSettingRemindersToMove extends DaFitSetting<DaFitSettingRemindersToMove.RemindersToMove> {
    public static class RemindersToMove {
        public byte period;
        public byte steps;
        public byte start_h;
        public byte end_h;

        public RemindersToMove() {
        }

        public RemindersToMove(byte period, byte steps, byte start_h, byte end_h) {
            this.period = period;
            this.steps = steps;
            this.start_h = start_h;
            this.end_h = end_h;
        }

        @Override
        public String toString() {
            return "RemindersToMove{" +
                "period=" + period +
                ", steps=" + steps +
                ", start_h=" + start_h +
                ", end_h=" + end_h +
                '}';
        }
    }

    public DaFitSettingRemindersToMove(String name, byte cmdQuery, byte cmdSet) {
        super(name, cmdQuery, cmdSet);
    }

    @Override
    public byte[] encode(RemindersToMove value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(value.period);
        buffer.put(value.steps);
        buffer.put(value.start_h);
        buffer.put(value.end_h);
        return buffer.array();
    }

    @Override
    public RemindersToMove decode(byte[] data) {
        if (data.length != 4)
            throw new IllegalArgumentException("Wrong data length, should be 4, was " + data.length);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte period = buffer.get();
        byte steps = buffer.get();
        byte start_h = buffer.get();
        byte end_h = buffer.get();
        return new RemindersToMove(period, steps, start_h, end_h);
    }
}
