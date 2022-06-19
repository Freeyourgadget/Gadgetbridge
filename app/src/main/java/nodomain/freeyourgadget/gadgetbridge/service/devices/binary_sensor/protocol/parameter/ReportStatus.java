package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ParameterId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState;

public class ReportStatus extends Parameter{
    ReportState reportState;
    public ReportStatus(nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState reportState) {
        super(ParameterId.PARAMETER_ID_REPORT_STATUS, reportState.getReportStateByte());
        this.reportState = reportState;
    }

    public static ReportStatus decode(byte[] data){
        return new ReportStatus(
                nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.ReportState.fromReportStateByte(data[0])
        );
    }
}
