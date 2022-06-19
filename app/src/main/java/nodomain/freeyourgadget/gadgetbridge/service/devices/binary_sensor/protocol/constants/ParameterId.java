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
