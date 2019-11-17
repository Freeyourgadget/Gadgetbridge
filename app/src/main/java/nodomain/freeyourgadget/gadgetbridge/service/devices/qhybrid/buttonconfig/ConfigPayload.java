package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public enum ConfigPayload {
    FORWARD_TO_PHONE(
            "forward to phone", 
            new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x00},
            new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x0C, (byte) 0x2E, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x01, (byte) 0x08, (byte) 0x01, (byte) 0x14, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0xFE, (byte) 0x08, (byte) 0x00, (byte) 0x93, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xBF, (byte) 0xD5, (byte) 0x54, (byte) 0xD1,}
            ),
    STOPWATCH(
            "stopwatch",
            new byte[]{(byte) 0x02, (byte) 0x01, (byte) 0x20, (byte) 0x01},
            new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x92, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x0F, (byte) 0xC0, (byte) 0x5F, (byte) 0x2A}
    ),
    DATE(
            "show date",
            new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x14, (byte) 0x00},
            new byte[]{(byte) 0x01 , (byte) 0x00 , (byte) 0x01 , (byte) 0x01 , (byte) 0x14 , (byte) 0x2D , (byte) 0x00 , (byte) 0x00 , (byte) 0x00 , (byte) 0x01 , (byte) 0x00 , (byte) 0x06 , (byte) 0x00 , (byte) 0x02 , (byte) 0x00 , (byte) 0x00 , (byte) 0x07 , (byte) 0x00 , (byte) 0x01 , (byte) 0x01 , (byte) 0x16 , (byte) 0x00 , (byte) 0x89 , (byte) 0x05 , (byte) 0x01 , (byte) 0x07 , (byte) 0xB0 , (byte) 0x00 , (byte) 0x00 , (byte) 0xB0 , (byte) 0x00 , (byte) 0x00 , (byte) 0xB0 , (byte) 0x00 , (byte) 0x00 , (byte) 0x08 , (byte) 0x01 , (byte) 0x50 , (byte) 0x00 , (byte) 0x01 , (byte) 0x00 , (byte) 0xD0 , (byte) 0x89 , (byte) 0xDE , (byte) 0x6E}
    ),
    LAST_NOTIFICATION(
            "show last notification",
            new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x18, (byte) 0x00},
            new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x18, (byte) 0x2F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x16, (byte) 0x00, (byte) 0x89, (byte) 0x05, (byte) 0x01, (byte) 0x07, (byte) 0xB0, (byte) 0x02, (byte) 0x00, (byte) 0xB0, (byte) 0x02, (byte) 0x00, (byte) 0xB0, (byte) 0x02, (byte) 0x00, (byte) 0x08, (byte) 0x01, (byte) 0x50, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x6B, (byte) 0x9D, (byte) 0x55, (byte) 0x3A}
    );
    private byte[] header, data;

    static public ConfigPayload fromId(short id) throws RuntimeException{
        for(ConfigPayload payload : ConfigPayload.values()){
            ByteBuffer buffer = ByteBuffer.wrap(payload.header);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            if(id == buffer.getShort(1)) return payload;
        }

        throw new RuntimeException("app " + id + " not found");
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getData() {
        return data;
    }

    public String getDescription() {
        return description;
    }

    public boolean equals(ConfigPayload p1, ConfigPayload p2){
        return Arrays.equals(p1.getData(), p2.getData());
    }

    private String description;

    ConfigPayload(String description, byte[] header, byte[] data) {
        this.description = description;
        this.header = header;
        this.data = data;
    }
}
