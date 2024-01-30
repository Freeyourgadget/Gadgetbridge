package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class GpsAndTime {
    public static final byte id = 0x18;

    public static class CurrentGPSRequest extends HuaweiPacket {
        public static final byte id = 0x07;
        public CurrentGPSRequest (
                ParamsProvider paramsProvider
        ) {
            super(paramsProvider);

            this.serviceId = GpsAndTime.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV();
            this.tlv.put(0x01, (int) 0);
            this.tlv.put(0x02, (long) 0);
            this.tlv.put(0x03, (long) 0);
            this.isEncrypted = true;
            this.complete = true;

        }
    }
}
