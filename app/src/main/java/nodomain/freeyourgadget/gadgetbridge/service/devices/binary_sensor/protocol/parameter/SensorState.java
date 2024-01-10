/*  Copyright (C) 2022-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;

public class SensorState extends Parameter{
    nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState sensorState;
    int count;

    public SensorState(nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState sensorState, int count) {
        super(ParameterId.PARAMETER_ID_SENSOR_STATUS, sensorState.getSensorStateByte());
        this.sensorState = sensorState;
        this.count = count;
    }

    public nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState getSensorState() {
        return sensorState;
    }

    public int getCount() {
        return count;
    }

    public static SensorState decode(byte[] data){
        int dataInt = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);
        byte stateByte = (byte)((dataInt >> 11) & 0x01);
        int count = dataInt & 0b11111111111;
        return new SensorState(
                nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState.fromSensorStateByte(stateByte),
                count
        );
    }
}
