package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.MessageId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.Parameter;

public class GetSensorRequest extends Message{
    public GetSensorRequest(SensorType sensorType) {
        super(
                MessageId.MESSAGE_ID_GET_SENSOR_REQUEST,
                new Parameter[]{
                        new nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.SensorType(sensorType)
                }
        );
    }
}
