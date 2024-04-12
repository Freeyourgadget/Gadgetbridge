package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.util.SparseArray;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.NotificationControlMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.NotificationDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.NotificationUpdateMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationDataStatusMessage;

public class NotificationsHandler implements MessageHandler {
    public static final SimpleDateFormat NOTIFICATION_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);
    private static final Logger LOG = LoggerFactory.getLogger(NotificationsHandler.class);
    private final Queue<NotificationSpec> notificationSpecQueue;
    private final Upload upload;
    private boolean enabled = false;


    public NotificationsHandler() {
        this.notificationSpecQueue = new LinkedList<>();
        this.upload = new Upload();
    }

    public NotificationUpdateMessage onNotification(NotificationSpec notificationSpec) {
        if (!enabled)
            return null;
        final boolean isUpdate = addNotificationToQueue(notificationSpec);

        NotificationUpdateMessage.NotificationUpdateType notificationUpdateType = isUpdate ? NotificationUpdateMessage.NotificationUpdateType.MODIFY : NotificationUpdateMessage.NotificationUpdateType.ADD;

        if (notificationSpecQueue.size() > 10)
            notificationSpecQueue.poll(); //remove the oldest notification TODO: should send a delete notification message to watch!

        return new NotificationUpdateMessage(notificationUpdateType, notificationSpec.type, getNotificationsCount(notificationSpec.type), notificationSpec.getId());
    }


    private boolean addNotificationToQueue(NotificationSpec notificationSpec) {
        boolean found = false;
        Iterator<NotificationSpec> iterator = notificationSpecQueue.iterator();
        while (iterator.hasNext()) {
            NotificationSpec e = iterator.next();
            if (e.getId() == notificationSpec.getId()) {
                found = true;
                iterator.remove();
            }
        }
        notificationSpecQueue.offer(notificationSpec); // Add the notificationSpec to the front of the queue
        return found;
    }

    public NotificationUpdateMessage onSetCallState(CallSpec callSpec) {
        if (!enabled)
            return null;
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            NotificationSpec callNotificationSpec = new NotificationSpec(callSpec.number.hashCode());
            callNotificationSpec.phoneNumber = callSpec.number;
            callNotificationSpec.sourceAppId = callSpec.sourceAppId;
            callNotificationSpec.title = StringUtils.isEmpty(callSpec.name) ? callSpec.number : callSpec.name;
            callNotificationSpec.type = NotificationType.GENERIC_PHONE;
            callNotificationSpec.body = StringUtils.isEmpty(callSpec.name) ? callSpec.number : callSpec.name;

            return onNotification(callNotificationSpec);
        } else {
            if (callSpec.number != null) // this happens in debug screen
                return onDeleteNotification(callSpec.number.hashCode());
        }
        return null;
    }


    public NotificationUpdateMessage onDeleteNotification(int id) {
        if (!enabled)
            return null;

        Iterator<NotificationSpec> iterator = notificationSpecQueue.iterator();
        while (iterator.hasNext()) {
            NotificationSpec e = iterator.next();
            if (e.getId() == id) {
                iterator.remove();
                return new NotificationUpdateMessage(NotificationUpdateMessage.NotificationUpdateType.REMOVE, e.type, getNotificationsCount(e.type), id);
            }
        }
        return null;
    }

    private int getNotificationsCount(NotificationType notificationType) {
        int count = 0;
        for (NotificationSpec e : notificationSpecQueue) {
            count += e.type == notificationType ? 1 : 0;
        }
        return count;
    }

    private NotificationSpec getNotificationSpecFromQueue(int id) {
        for (NotificationSpec e : notificationSpecQueue) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    public GFDIMessage handle(GFDIMessage message) {
        if (!enabled)
            return null;
        if (message instanceof NotificationControlMessage) {
            final NotificationSpec notificationSpec = getNotificationSpecFromQueue(((NotificationControlMessage) message).getNotificationId());
            if (notificationSpec != null) {
                switch (((NotificationControlMessage) message).getCommand()) {
                    case GET_NOTIFICATION_ATTRIBUTES:
                        final MessageWriter messageWriter = new MessageWriter();
                        messageWriter.writeByte(NotificationCommand.GET_NOTIFICATION_ATTRIBUTES.code);
                        messageWriter.writeInt(((NotificationControlMessage) message).getNotificationId());
                        for (Map.Entry<NotificationAttribute, Integer> attribute : ((NotificationControlMessage) message).getNotificationAttributesMap().entrySet()) {
                            if (!attribute.getKey().equals(NotificationAttribute.MESSAGE_SIZE)) { //should be last
                                messageWriter.writeByte(attribute.getKey().code);
                                final byte[] bytes = attribute.getKey().getNotificationSpecAttribute(notificationSpec, attribute.getValue());
                                messageWriter.writeShort(bytes.length);
                                messageWriter.writeBytes(bytes);
                                LOG.info("ATTRIBUTE:{} value:{} length:{}", attribute.getKey(), new String(bytes), bytes.length);
                            }
                        }
                        if (((NotificationControlMessage) message).getNotificationAttributesMap().containsKey(NotificationAttribute.MESSAGE_SIZE)) {
                            messageWriter.writeByte(NotificationAttribute.MESSAGE_SIZE.code);
                            final byte[] bytes = NotificationAttribute.MESSAGE_SIZE.getNotificationSpecAttribute(notificationSpec, 0);
                            messageWriter.writeShort(bytes.length);
                            messageWriter.writeBytes(bytes);
                            LOG.info("ATTRIBUTE:{} value:{} length:{}", NotificationAttribute.MESSAGE_SIZE, new String(bytes), bytes.length);

                        }
                        NotificationFragment notificationFragment = new NotificationFragment(messageWriter.getBytes());
                        return upload.setCurrentlyUploading(notificationFragment);
                    default:
                        LOG.error("NOT SUPPORTED");
                }
            }
        } else if (message instanceof NotificationDataStatusMessage) {
            return upload.processUploadProgress((NotificationDataStatusMessage) message);
        }

        return null;
    }


    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    public enum NotificationCommand { //was AncsCommand
        GET_NOTIFICATION_ATTRIBUTES(0),
        GET_APP_ATTRIBUTES(1),
        PERFORM_NOTIFICATION_ACTION(2),
        // Garmin extensions
        PERFORM_ANDROID_ACTION(128);

        public final int code;

        NotificationCommand(int code) {
            this.code = code;
        }

        public static NotificationCommand fromCode(int code) {
            for (NotificationCommand value : values()) {
                if (value.code == code)
                    return value;
            }
            throw new IllegalArgumentException("Unknown notification command " + code);
        }
    }

    public enum NotificationAttribute { //was AncsAttribute
        APP_IDENTIFIER(0),
        TITLE(1, true),
        SUBTITLE(2, true),
        MESSAGE(3, true),
        MESSAGE_SIZE(4),
        DATE(5),
        //        POSITIVE_ACTION_LABEL(6),
//        NEGATIVE_ACTION_LABEL(7),
        // Garmin extensions
//        PHONE_NUMBER(126, true),
        ACTIONS(127, false, true),
        ;
        private static final SparseArray<NotificationAttribute> valueByCode;

        static {
            final NotificationAttribute[] values = values();
            valueByCode = new SparseArray<>(values.length);
            for (NotificationAttribute value : values) {
                valueByCode.append(value.code, value);
            }
        }

        public final int code;
        public final boolean hasLengthParam;
        public final boolean hasAdditionalParams;

        NotificationAttribute(int code) {
            this(code, false, false);
        }

        NotificationAttribute(int code, boolean hasLengthParam) {
            this(code, hasLengthParam, false);
        }

        NotificationAttribute(int code, boolean hasLengthParam, boolean hasAdditionalParams) {
            this.code = code;
            this.hasLengthParam = hasLengthParam;
            this.hasAdditionalParams = hasAdditionalParams;
        }

        public static NotificationAttribute getByCode(int code) {
            return valueByCode.get(code);
        }

        public byte[] getNotificationSpecAttribute(NotificationSpec notificationSpec, int maxLength) {
            String toReturn = "";
            switch (this) {
                case DATE:
                    final long notificationTimestamp = notificationSpec.when == 0 ? System.currentTimeMillis() : notificationSpec.when;
                    toReturn = NOTIFICATION_DATE_FORMAT.format(new Date(notificationTimestamp));
                    break;
                case TITLE:
                    toReturn = notificationSpec.title == null ? "" : notificationSpec.title;
                    break;
                case SUBTITLE:
                    toReturn = notificationSpec.subject == null ? "" : notificationSpec.subject;
                    break;
                case APP_IDENTIFIER:
                    toReturn = notificationSpec.sourceAppId == null ? "" : notificationSpec.sourceAppId;
                    break;
                case MESSAGE:
                    toReturn = notificationSpec.body == null ? "" : notificationSpec.body;
                    break;
                case MESSAGE_SIZE:
                    toReturn = Integer.toString(notificationSpec.body == null ? "".length() : notificationSpec.body.length());
                    break;
                case ACTIONS:
                    toReturn = new String(new byte[]{0x00, 0x00, 0x00, 0x00});
                    break;
            }
            return toReturn.substring(0, Math.min(toReturn.length(), maxLength)).getBytes(StandardCharsets.UTF_8);
        }
    }

    class Upload {

        private NotificationFragment currentlyUploading;

        public NotificationDataMessage setCurrentlyUploading(NotificationFragment currentlyUploading) {
            this.currentlyUploading = currentlyUploading;
            return currentlyUploading.take();
        }

        private GFDIMessage processUploadProgress(NotificationDataStatusMessage notificationDataStatusMessage) {
            if (!currentlyUploading.dataHolder.hasRemaining()) {
                this.currentlyUploading = null;
                LOG.info("SENT ALL");

                return new NotificationDataStatusMessage(GFDIMessage.GarminMessage.NOTIFICATION_DATA, GFDIMessage.Status.ACK, NotificationDataStatusMessage.TransferStatus.OK);
            } else {
                if (notificationDataStatusMessage.canProceed()) {
                    LOG.info("SENDING NEXT CHUNK!!!");
                    return currentlyUploading.take();
                } else {
                    LOG.warn("Cannot proceed with upload"); //TODO: send the correct status message
                    this.currentlyUploading = null;
                }

            }
            return null;
        }

    }

    class NotificationFragment {
        private final int dataSize;
        private final ByteBuffer dataHolder;
        private final int maxBlockSize = 300;
        private int runningCrc;

        NotificationFragment(byte[] contents) {
            this.dataHolder = ByteBuffer.wrap(contents);
            this.dataSize = contents.length;
            this.dataHolder.flip();
            this.dataHolder.compact();
            this.setRunningCrc(0);
        }

        public int getDataSize() {
            return dataSize;
        }

        private int getMaxBlockSize() {
            return maxBlockSize;
        }

        private NotificationDataMessage take() {
            final int currentOffset = this.dataHolder.position();
            final byte[] chunk = new byte[Math.min(this.dataHolder.remaining(), getMaxBlockSize())];
            this.dataHolder.get(chunk);
            setRunningCrc(ChecksumCalculator.computeCrc(getRunningCrc(), chunk, 0, chunk.length));
            return new NotificationDataMessage(chunk, getDataSize(), currentOffset, getRunningCrc());
        }

        private int getRunningCrc() {
            return runningCrc;
        }

        private void setRunningCrc(int runningCrc) {
            this.runningCrc = runningCrc;
        }
    }

}
