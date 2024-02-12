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

public enum ResultCode {
    RESULT_CODE_SUCCESS,
    RESULT_CODE_FAILURE;

    public byte getResultCodeByte(){
        return (byte) ordinal();
    }

    public static ResultCode fromResultCodeByte(byte resultCode){
        for(ResultCode value:ResultCode.values()){
            if(value.getResultCodeByte() == resultCode){
                return value;
            }
        }
        return null;
    }
}
