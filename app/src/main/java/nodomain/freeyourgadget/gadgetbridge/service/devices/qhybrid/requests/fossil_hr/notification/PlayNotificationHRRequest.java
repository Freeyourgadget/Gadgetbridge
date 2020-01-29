package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class PlayNotificationHRRequest extends FilePutRequest {

    public PlayNotificationHRRequest(NotificationSpec spec, FossilWatchAdapter adapter) {
        this(spec.sourceAppId, spec.sender, spec.body, adapter);
    }

    public PlayNotificationHRRequest(String packageName, String sender, String message, FossilWatchAdapter adapter){
        super((short) 0x0900, createFile(packageName, sender, message), adapter);
    }

    private static byte[] createFile(String packageName, String sender, String message) {
        byte lengthBufferLength = (byte) 10;
        byte typeId = 3;
        byte flags = getFlags();
        byte uidLength = (byte) 4;
        byte appBundleCRCLength = (byte) 4;
        String nullTerminatedTitle = StringUtils.terminateNull(packageName);

        Charset charsetUTF8 = Charset.forName("UTF-8");
        byte[] titleBytes = nullTerminatedTitle.getBytes(charsetUTF8);
        String nullTerminatedSender = StringUtils.terminateNull(sender);
        byte[] senderBytes = nullTerminatedSender.getBytes(charsetUTF8);
        String nullTerminatedMessage = StringUtils.terminateNull(message);
        byte[] messageBytes = nullTerminatedMessage.getBytes(charsetUTF8);

        short mainBufferLength = (short) (lengthBufferLength + uidLength + appBundleCRCLength + titleBytes.length + senderBytes.length + messageBytes.length);

        ByteBuffer lengthBuffer = ByteBuffer.allocate(lengthBufferLength);
        lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
        lengthBuffer.putShort(mainBufferLength);
        lengthBuffer.put(lengthBufferLength);
        lengthBuffer.put(typeId);
        lengthBuffer.put(flags);
        lengthBuffer.put(uidLength);
        lengthBuffer.put(appBundleCRCLength);
        lengthBuffer.put((byte) titleBytes.length);
        lengthBuffer.put((byte) senderBytes.length);
        lengthBuffer.put((byte) messageBytes.length);

        ByteBuffer mainBuffer = ByteBuffer.allocate(mainBufferLength);
        mainBuffer.order(ByteOrder.LITTLE_ENDIAN);
        mainBuffer.put(lengthBuffer.array());

        lengthBuffer = ByteBuffer.allocate(mainBufferLength - lengthBufferLength);
        lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
        // lengthBuffer.putInt(0);
        lengthBuffer.put((byte) 0x00);
        lengthBuffer.put((byte) 0x00);
        lengthBuffer.put((byte) 0x00);
        lengthBuffer.put((byte) 0x00);

        CRC32 packageNameCrc = new CRC32();
        packageNameCrc.update(packageName.getBytes());
        // lengthBuffer.putInt((int) packageNameCrc.getValue());

        lengthBuffer.putInt((int) 0);

        // lengthBuffer.put((byte) 0x19);
        // lengthBuffer.put((byte) 0x38);
        // lengthBuffer.put((byte) 0xE0);
        // lengthBuffer.put((byte) 0xDA);
        lengthBuffer.put(titleBytes);
        lengthBuffer.put(senderBytes);
        lengthBuffer.put(messageBytes);
        mainBuffer.put(lengthBuffer.array());
        return mainBuffer.array();
    }



    private static byte getFlags(){
        return (byte) 2;
    }
}
