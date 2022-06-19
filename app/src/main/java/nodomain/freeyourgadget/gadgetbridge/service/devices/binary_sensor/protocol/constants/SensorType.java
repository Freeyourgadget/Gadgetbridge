package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants;

public enum SensorType {
    SENSOR_TYPE_OPENING_CLOSING,
    SENSOR_TYPE_VIBRATION_DETECTION,
    SENSOR_TYPE_HUMAN_DETECTION;

    public byte getSensorTypeByte(){
        return (byte) ordinal();
    }

    public static SensorType fromSensorTypeByte(byte sensorType){
        for(SensorType value:SensorType.values()){
            if(value.getSensorTypeByte() == sensorType){
                return value;
            }
        }
        return null;
    }
}
