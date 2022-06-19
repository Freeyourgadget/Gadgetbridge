package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;

public class ResultCode extends Parameter{
    nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ResultCode resultCode;
    public ResultCode(nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ResultCode resultCode) {
        super(ParameterId.PARAMETER_ID_RESULT_CODE, resultCode.getResultCodeByte());
        this.resultCode = resultCode;
    }

    public static ResultCode decode(byte[] data){
        return new ResultCode(
                nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ResultCode.fromResultCodeByte(data[0])
        );
    }
}
