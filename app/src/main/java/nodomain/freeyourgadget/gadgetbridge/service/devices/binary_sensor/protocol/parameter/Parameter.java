package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;

public class Parameter {
    ParameterId parameterId;
    byte[] payload;

    public Parameter(ParameterId parameterId, byte... payload) {
        this.parameterId = parameterId;
        this.payload = payload;
    }

    public ParameterId getParameterId() {
        return parameterId;
    }

    public int getPayloadLength(){
        return payload.length;
    }

    public byte[] encode(){
        ByteBuffer buffer = ByteBuffer.allocate(payload.length + 4);
        buffer
                .put(parameterId.getParameterIdByte())
                .put((byte) payload.length)
                .put((byte) 0x00) // RFU
                .put((byte) 0x00) // RFU
                .put(payload);

        return buffer.array();
    }

    public static Parameter decode(ParameterId parameterId, byte[] payload){
        if(parameterId == ParameterId.PARAMETER_ID_RESULT_CODE){
            return ResultCode.decode(payload);
        }else if(parameterId == ParameterId.PARAMETER_ID_REPORT_STATUS){
            return ReportStatus.decode(payload);
        }else if(parameterId == ParameterId.PARAMETER_ID_SENSOR_STATUS){
            return SensorState.decode(payload);
        }else if(parameterId == ParameterId.PARAMETER_ID_SENSOR_TYPE){
            return SensorType.decode(payload);
        }

        return null;
    }
}
