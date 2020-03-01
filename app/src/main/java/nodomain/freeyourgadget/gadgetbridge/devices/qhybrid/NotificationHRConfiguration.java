package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class NotificationHRConfiguration implements Serializable {
    private String packageName;
    private long id = -1;
    private byte[] packageCrc;

    public NotificationHRConfiguration(String packageName, long id) {
        this.packageName = packageName;
        this.id = id;

        CRC32 crc = new CRC32();
        crc.update(packageName.getBytes());

        this.packageCrc = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) crc.getValue())
                .array();
    }

    public NotificationHRConfiguration(String packageName, byte[] packageCrc, long id) {
        this.id = id;
        this.packageCrc = packageCrc;
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public long getId() {
        return id;
    }

    public byte[] getPackageCrc() {
        return packageCrc;
    }
}
