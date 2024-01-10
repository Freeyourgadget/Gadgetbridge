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

public enum MessageId {
    MESSAGE_ID_GET_SENSOR_REQUEST,
    MESSAGE_ID_GET_SENSOR_RESPONSE,
    MESSAGE_ID_SET_SENSOR_REQUEST,
    MESSAGE_ID_SET_SENSOR_RESPONSE,
    MESSAGE_ID_SENSOR_STATUS_EVENT;

    public byte getMessageIdByte(){
        return (byte) ordinal();
    }

    public static MessageId fromMessageIdByte(byte messageIdByte){
        for(MessageId value:MessageId.values()){
            if(value.getMessageIdByte() == messageIdByte){
                return value;
            }
        }
        return null;
    }
}
