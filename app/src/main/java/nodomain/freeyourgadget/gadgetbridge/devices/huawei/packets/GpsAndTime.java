package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class GpsAndTime {
    public static final byte id = 0x18;

    public static class CurrentGPSRequest extends HuaweiPacket {
        public static final byte id = 0x07;
        public CurrentGPSRequest (
                ParamsProvider paramsProvider,
                int timestamp,
                double lat,
                double lon
        ) {
            super(paramsProvider);

            this.serviceId = GpsAndTime.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV()
                    .put(0x01, timestamp)
                    .put(0x02, lon)
                    .put(0x03, lat);
            this.isEncrypted = true;
            this.complete = true;
        }
    }
}
