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
package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants;

public enum ParameterId {
    PARAMETER_ID_RESULT_CODE((byte) 0x00),
    PARAMETER_ID_CANCEL((byte) 0x01),
    PARAMETER_ID_SENSOR_TYPE((byte) 0x02),
    PARAMETER_ID_REPORT_STATUS((byte) 0x03),
    PARAMETER_ID_SENSOR_STATUS((byte) 0x0A),
    PARAMETER_ID_MULTIPLE_SENSOR_STATUS((byte) 0x0B),
    PARAMETER_ID_NAME((byte) 0x0C);

    private byte id;

    ParameterId(byte id){
        this.id = id;
    }

    public byte getParameterIdByte(){
        return this.id;
    }

    public static ParameterId fromParameterIdByte(byte parameterId){
        for(ParameterId id:ParameterId.values()){
            if(id.getParameterIdByte() == parameterId){
                return id;
            }
        }
        return null;
    }
}
