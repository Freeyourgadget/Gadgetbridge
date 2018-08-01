package nodomain.freeyourgadget.gadgetbridge.service.devices.id115;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115Constants;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SendNotificationOperation extends AbstractID115Operation {
    private static final Logger LOG = LoggerFactory.getLogger(SendNotificationOperation.class);

    byte[] currentNotificationBuffer;
    int currentNotificationSize;
    int currentNotificationIndex;
    byte currentNotificationType;

    SendNotificationOperation(ID115Support support, NotificationSpec notificationSpec)
    {
        super(support);

        String phone = "";
        if (notificationSpec.phoneNumber != null) {
            phone = notificationSpec.phoneNumber;
        }

        String title = "";
        if (notificationSpec.sender != null) {
            title = notificationSpec.sender;
        } else if (notificationSpec.title != null) {
            title = notificationSpec.title;
        } else if (notificationSpec.subject != null) {
            title = notificationSpec.subject;
        }

        String text = "";
        if (notificationSpec.body != null) {
            text = notificationSpec.body;
        }

        currentNotificationBuffer = encodeMessageNotification(notificationSpec.type, title, phone, text);
        currentNotificationSize = (currentNotificationBuffer.length + 15) / 16;
        currentNotificationType = ID115Constants.CMD_KEY_NOTIFY_MSG;
    }

    SendNotificationOperation(ID115Support support, CallSpec callSpec)
    {
        super(support);

        String number = "";
        if (callSpec.number != null) {
            number = callSpec.number;
        }

        String name = "";
        if (callSpec.name != null) {
            name = callSpec.name;
        }

        currentNotificationBuffer = encodeCallNotification(name, number);
        currentNotificationSize = (currentNotificationBuffer.length + 15) / 16;
        currentNotificationType = ID115Constants.CMD_KEY_NOTIFY_CALL;
    }

    @Override
    boolean isHealthOperation() {
        return false;
    }

    @Override
    protected void doPerform() throws IOException {
        sendNotificationChunk(1);
    }

    void sendNotificationChunk(int chunkIndex) throws IOException {
        currentNotificationIndex = chunkIndex;

        int offset = (chunkIndex - 1) * 16;
        int tailSize = currentNotificationBuffer.length - offset;
        int chunkSize = (tailSize > 16)? 16 : tailSize;

        byte raw[] = new byte[16];
        System.arraycopy(currentNotificationBuffer, offset, raw, 0, chunkSize);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ID115Constants.CMD_ID_NOTIFY);
        outputStream.write(currentNotificationType);
        outputStream.write((byte)currentNotificationSize);
        outputStream.write((byte)currentNotificationIndex);
        outputStream.write(raw);
        byte cmd[] = outputStream.toByteArray();

        TransactionBuilder builder = performInitialized("send notification chunk");
        builder.write(controlCharacteristic, cmd);
        builder.queue(getQueue());
    }

    void handleResponse(byte[] data) {
        if (!isOperationRunning()) {
            LOG.error("ignoring notification because operation is not running. Data length: " + data.length);
            getSupport().logMessageContent(data);
            return;
        }

        if (data.length < 2) {
            LOG.warn("short GATT response");
            return;
        }
        if (data[0] == ID115Constants.CMD_ID_NOTIFY) {
            if (data.length < 4) {
                LOG.warn("short GATT response for NOTIFY");
                return;
            }
            if (data[1] == currentNotificationType) {
                if (data[3] == currentNotificationIndex) {
                    if (currentNotificationIndex != currentNotificationSize) {
                        try {
                            sendNotificationChunk(currentNotificationIndex + 1);
                        } catch (IOException ex) {
                            GB.toast(getContext(), "Error sending ID115 notification, you may need to connect and disconnect", Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    } else {
                        LOG.info("Notification transfer has finished.");
                        operationFinished();
                    }
                }
            }
        }
    }

    byte[] encodeCallNotification(String name, String phone) {
        if (name.length() > 20) {
            name = name.substring(0, 20);
        }
        if (phone.length() > 20) {
            phone = phone.substring(0, 20);
        }

        byte[] name_bytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] phone_bytes = phone.getBytes(StandardCharsets.UTF_8);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write((byte) phone_bytes.length);
            outputStream.write((byte) name_bytes.length);
            outputStream.write(phone_bytes);
            outputStream.write(name_bytes);
            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    byte[] encodeMessageNotification(NotificationType type, String title, String phone, String text) {
        if (title.length() > 20) {
            title = title.substring(0, 20);
        }
        if (phone.length() > 20) {
            phone = phone.substring(0, 20);
        }
        if (text.length() > 20) {
            text = text.substring(0, 20);
        }
        byte[] title_bytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] phone_bytes = phone.getBytes(StandardCharsets.UTF_8);
        byte[] text_bytes = text.getBytes(StandardCharsets.UTF_8);

        byte nativeType = ID115Constants.getNotificationType(type);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(nativeType);
            outputStream.write((byte) text_bytes.length);
            outputStream.write((byte) phone_bytes.length);
            outputStream.write((byte) title_bytes.length);
            outputStream.write(phone_bytes);
            outputStream.write(title_bytes);
            outputStream.write(text_bytes);
            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
