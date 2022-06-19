package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants;

public enum SensorState {
    SENSOR_STATE_CLOSED,
    SENSOR_STATE_OPEN;

    public byte getSensorStateByte(){
        return (byte) ordinal();
    }

    public static SensorState fromSensorStateByte(byte sensorState){
        for(SensorState value:SensorState.values()){
            if(value.getSensorStateByte() == sensorState){
                return value;
            }
        }
        return null;
    }
}
