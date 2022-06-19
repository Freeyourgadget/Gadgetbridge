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
