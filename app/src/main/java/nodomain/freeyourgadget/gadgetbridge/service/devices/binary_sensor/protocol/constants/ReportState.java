package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants;

public enum ReportState {
    REPORT_STATUS_DISABLED,
    REPORT_STATUS_ENABLED;

    public byte getReportStateByte(){
        return (byte) ordinal();
    }

    public static ReportState fromReportStateByte(byte reportState){
        for(ReportState value:ReportState.values()){
            if(value.getReportStateByte() == reportState){
                return value;
            }
        }
        return null;
    }
}
