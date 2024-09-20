package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HuaweiReportThreshold {
    private int dataType;
    private int valueType;
    private int value;
    private int action;

    public HuaweiReportThreshold(int dataType, int valueType, int value, int action) {
        this.dataType = dataType;
        this.valueType = valueType;
        this.value = value;
        this.action = action;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte) dataType);
        buffer.put((byte) valueType);
        buffer.putShort((short) value);
        buffer.put((byte) action);
        return buffer.array();
    }

    public static List<HuaweiReportThreshold> getReportThresholds() {
        List<HuaweiReportThreshold> thresholds = new ArrayList<>();
        thresholds.add(new HuaweiReportThreshold(1,3, 500, 2));
        thresholds.add(new HuaweiReportThreshold(3,3, 100, 2));

        int current_hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int value = (current_hour < 6)?21600:3600;
        thresholds.add(new HuaweiReportThreshold(4,3, value, 2));

        return thresholds;
    }
}
