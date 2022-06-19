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
        int dataInt = (data[1] << 8) | data[0];
        byte stateByte = (byte)((dataInt >> 11) & 0x01);
        int count = dataInt & 0b11111111111;
        return new SensorState(
                nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState.fromSensorStateByte(stateByte),
                count
        );
    }
}
