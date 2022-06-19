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
