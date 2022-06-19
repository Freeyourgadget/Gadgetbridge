package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.MessageId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.Parameter;

public class SetSensorRequest extends Message{
    public SetSensorRequest(SensorType sensorType, ReportState reportState) {
        super(
                MessageId.MESSAGE_ID_SET_SENSOR_REQUEST,
                new Parameter[]{
                        new nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.SensorType(sensorType),
                        new nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.ReportStatus(reportState)
                }
        );
    }
}
