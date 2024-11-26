package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.util.SparseArray;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.NotificationControlMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.NotificationDataMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.NotificationUpdateMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationDataStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;

public class NotificationsHandler implements MessageHandler {
    public static final SimpleDateFormat NOTIFICATION_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);
    private static final Logger LOG = LoggerFactory.getLogger(NotificationsHandler.class);
    private final Queue<NotificationSpec> notificationSpecQueue;
    private final Upload upload;
    private boolean enabled = false;
    // Keep track of Notification ID -> action handle, as BangleJSDeviceSupport.
    // TODO: This needs to be simplified.
    private final LimitedQueue<Integer, Long> mNotificationReplyAction = new LimitedQueue<>(16);


    public NotificationsHandler() {
        this.notificationSpecQueue = new LinkedList<>();
        this.upload = new Upload();
    }

    private static void encodeNotificationAttribute(NotificationSpec notificationSpec, Map.Entry<NotificationAttribute, Integer> entry, MessageWriter messageWriter) {
        messageWriter.writeByte(entry.getKey().code);
        final byte[] bytes = entry.getKey().getNotificationSpecAttribute(notificationSpec, entry.getValue());
        messageWriter.writeShort(bytes.length);
        messageWriter.writeBytes(bytes);
//        LOG.info("ATTRIBUTE:{} value:{}/{} length:{}", entry.getKey(), new String(bytes), GB.hexdump(bytes), bytes.length);
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
        final int id = StringUtils.firstNonBlank(callSpec.number, "Gadgetbridge Call").hashCode();
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            NotificationSpec callNotificationSpec = new NotificationSpec(id);
            callNotificationSpec.phoneNumber = callSpec.number;
            callNotificationSpec.sourceAppId = callSpec.sourceAppId;
            callNotificationSpec.title = StringUtils.isEmpty(callSpec.name) ? callSpec.number : callSpec.name;
            callNotificationSpec.type = NotificationType.GENERIC_PHONE;
            callNotificationSpec.body = StringUtils.isEmpty(callSpec.name) ? callSpec.number : callSpec.name;

            // add an empty bogus action to toggle the hasActions boolean. The actions are hardcoded on the watch in case of incoming calls.
            callNotificationSpec.attachedActions = new ArrayList<>();
            callNotificationSpec.attachedActions.add(0, new NotificationSpec.Action());


            return onNotification(callNotificationSpec);
        } else {
            return onDeleteNotification(id);
        }
    }

    public NotificationUpdateMessage onNotification(NotificationSpec notificationSpec) {
        if (!enabled)
            return null;
        final boolean isUpdate = addNotificationToQueue(notificationSpec);

        NotificationUpdateMessage.NotificationUpdateType notificationUpdateType = isUpdate ? NotificationUpdateMessage.NotificationUpdateType.MODIFY : NotificationUpdateMessage.NotificationUpdateType.ADD;

        if (notificationSpecQueue.size() > 10)
            notificationSpecQueue.poll(); //remove the oldest notification TODO: should send a delete notification message to watch!

        final boolean hasActions = (null != notificationSpec.attachedActions && !notificationSpec.attachedActions.isEmpty());
        if (hasActions) {
            for (int i = 0; i < notificationSpec.attachedActions.size(); i++) {
                final NotificationSpec.Action action = notificationSpec.attachedActions.get(i);

                if (action.type == NotificationSpec.Action.TYPE_WEARABLE_REPLY || action.type == NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR) {
                    mNotificationReplyAction.add(notificationSpec.getId(), action.handle);
                }
            }
        }

        final boolean hasPicture = !StringUtils.isEmpty(notificationSpec.picturePath);
        return new NotificationUpdateMessage(notificationUpdateType, notificationSpec.type, getNotificationsCount(notificationSpec.type), notificationSpec.getId(), hasActions, hasPicture);
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
        LOG.warn("Failed to find notificationSpec in queue for {}", id);
        return null;
    }

    public String getNotificationAttachmentPath(int notificationId) {
        NotificationSpec notificationSpec = getNotificationSpecFromQueue(notificationId);
        if (null != notificationSpec)
            return notificationSpec.picturePath;
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
                return new NotificationUpdateMessage(NotificationUpdateMessage.NotificationUpdateType.REMOVE, e.type, getNotificationsCount(e.type), id, false, false);
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
                        return getNotificationDataMessage((NotificationControlMessage) message, notificationSpec);
                    case PERFORM_LEGACY_NOTIFICATION_ACTION:
                        LOG.info("Legacy Notification: {}", ((NotificationControlMessage) message).getLegacyNotificationAction());
                        break;
                    case PERFORM_NOTIFICATION_ACTION:
                        performNotificationAction((NotificationControlMessage) message, notificationSpec);
                        break;

                    default:
                        LOG.error("NOT SUPPORTED: {}", ((NotificationControlMessage) message).getCommand());
                }
            }
        } else if (message instanceof NotificationDataStatusMessage) {
            return upload.processUploadProgress((NotificationDataStatusMessage) message);
        }

        return null;
    }

    private void performNotificationAction(NotificationControlMessage message, NotificationSpec notificationSpec) {
        final GBDeviceEventNotificationControl deviceEvtNotificationControl = new GBDeviceEventNotificationControl();
        deviceEvtNotificationControl.handle = notificationSpec.getId();
        final GBDeviceEventCallControl deviceEvtCallControl = new GBDeviceEventCallControl();
        switch (message.getNotificationAction()) {
            case REPLY_INCOMING_CALL:
                deviceEvtCallControl.event = GBDeviceEventCallControl.Event.REJECT;
                message.addGbDeviceEvent(deviceEvtCallControl);
            case REPLY_MESSAGES:
                deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
                deviceEvtNotificationControl.reply = message.getActionString();
                if (notificationSpec.type.equals(NotificationType.GENERIC_PHONE) || notificationSpec.type.equals(NotificationType.GENERIC_SMS)) {
                    deviceEvtNotificationControl.phoneNumber = notificationSpec.phoneNumber;
                } else {
                    deviceEvtNotificationControl.handle = mNotificationReplyAction.lookup(notificationSpec.getId()); //handle of wearable action is needed
                }
                message.addGbDeviceEvent(deviceEvtNotificationControl);
                break;
            case ACCEPT_INCOMING_CALL:
                deviceEvtCallControl.event = GBDeviceEventCallControl.Event.ACCEPT;
                message.addGbDeviceEvent(deviceEvtCallControl);
                break;
            case REJECT_INCOMING_CALL:
                deviceEvtCallControl.event = GBDeviceEventCallControl.Event.REJECT;
                message.addGbDeviceEvent(deviceEvtCallControl);
                break;
            case DISMISS_NOTIFICATION:
                deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.DISMISS;
                message.addGbDeviceEvent(deviceEvtNotificationControl);
                break;
            case BLOCK_APPLICATION:
                deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.MUTE;
                message.addGbDeviceEvent(deviceEvtNotificationControl);
                break;
        }
    }

    private NotificationDataMessage getNotificationDataMessage(NotificationControlMessage message, NotificationSpec notificationSpec) {
        final MessageWriter messageWriter = new MessageWriter();
        messageWriter.writeByte(NotificationCommand.GET_NOTIFICATION_ATTRIBUTES.code);
        messageWriter.writeInt(message.getNotificationId());
        Map.Entry<NotificationAttribute, Integer> lastEntry = null;
        for (Map.Entry<NotificationAttribute, Integer> entry : message.getNotificationAttributesMap().entrySet()) {
            if (!NotificationAttribute.MESSAGE_SIZE.equals(entry.getKey())) {
                encodeNotificationAttribute(notificationSpec, entry, messageWriter);
            } else {
                lastEntry = entry;
            }
        }
        if (lastEntry != null) {
            encodeNotificationAttribute(notificationSpec, lastEntry, messageWriter);
        }
        NotificationFragment notificationFragment = new NotificationFragment(messageWriter.getBytes());
        return upload.setCurrentlyUploading(notificationFragment);
    }


    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    public enum NotificationCommand { //was AncsCommand
        GET_NOTIFICATION_ATTRIBUTES(0),
        GET_APP_ATTRIBUTES(1), //unknown/untested
        PERFORM_LEGACY_NOTIFICATION_ACTION(2),
        PERFORM_NOTIFICATION_ACTION(128);

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

    public enum LegacyNotificationAction { //was AncsAction
        ACCEPT,
        REFUSE

    }
    public enum NotificationAttribute { //was AncsAttribute
        APP_IDENTIFIER(0),
        TITLE(1, true),
        SUBTITLE(2, true),
        MESSAGE(3, true),
        MESSAGE_SIZE(4),
        DATE(5),
        //        POSITIVE_ACTION_LABEL(6), //needed only for legacy notification actions
        NEGATIVE_ACTION_LABEL(7), //needed only for legacy notification actions
        // Garmin extensions
//        PHONE_NUMBER(126, true),
        ACTIONS(127, false, true),
        ATTACHMENTS(128),
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
                    if (NotificationType.GENERIC_SMS.equals(notificationSpec.type))
                        toReturn = notificationSpec.sender == null ? "" : notificationSpec.sender;
                    else
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
                    toReturn = encodeNotificationActionsString(notificationSpec);
                    break;
                case ATTACHMENTS:
                    LOG.debug("Notification attachments requested for notification id {}", notificationSpec.getId());
                    toReturn = "1"; // the number of attachments
                    break;
            }
            if (maxLength == 0)
                return toReturn.getBytes(StandardCharsets.UTF_8);
            return toReturn.substring(0, Math.min(toReturn.length(), maxLength)).getBytes(StandardCharsets.UTF_8);
        }

        private String encodeNotificationActionsString(NotificationSpec notificationSpec) {

            final List<byte[]> garminActions = new ArrayList<>();
            if (notificationSpec.type.equals(NotificationType.GENERIC_PHONE)) {
                garminActions.add(encodeNotificationAction(NotificationAction.REPLY_INCOMING_CALL, " ")); //text is not shown on watch
                garminActions.add(encodeNotificationAction(NotificationAction.REJECT_INCOMING_CALL, " ")); //text is not shown on watch
                garminActions.add(encodeNotificationAction(NotificationAction.ACCEPT_INCOMING_CALL, " ")); //text is not shown on watch
            }
            if (null != notificationSpec.attachedActions) {
                for (NotificationSpec.Action action : notificationSpec.attachedActions) {
                    switch (action.type) {
                        case NotificationSpec.Action.TYPE_WEARABLE_REPLY:
                        case NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR:
                            garminActions.add(encodeNotificationAction(NotificationAction.REPLY_MESSAGES, action.title));
                            break;
                        case NotificationSpec.Action.TYPE_SYNTECTIC_DISMISS:
                            garminActions.add(encodeNotificationAction(NotificationAction.DISMISS_NOTIFICATION, action.title));
                            break;
                        case NotificationSpec.Action.TYPE_SYNTECTIC_MUTE:
                            garminActions.add(encodeNotificationAction(NotificationAction.BLOCK_APPLICATION, action.title));
                            break;

                    }
//                    LOG.info("Notification has action {} with title {}", action.type, action.title);
                }
            }
            if (garminActions.isEmpty())
                return new String(new byte[]{0x00, 0x00, 0x00, 0x00});

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                byteArrayOutputStream.write(garminActions.size());
                for (byte[] item : garminActions) {
                    byteArrayOutputStream.write(item);
                }
                return byteArrayOutputStream.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        private byte[] encodeNotificationAction(NotificationAction notificationAction, String description) {
            final ByteBuffer action = ByteBuffer.allocate(3 + description.getBytes(StandardCharsets.UTF_8).length);
            action.put((byte) notificationAction.code);
            if (null == notificationAction.notificationActionIconPosition)
                action.put((byte) 0x00);
            else
                action.put((byte) EnumUtils.generateBitVector(NotificationActionIconPosition.class, notificationAction.notificationActionIconPosition));
            action.put((byte) description.getBytes(StandardCharsets.UTF_8).length);
            action.put(description.getBytes());
            return action.array();
        }
    }

    public enum NotificationAction {
        REPLY_INCOMING_CALL(94, NotificationActionIconPosition.BOTTOM),
        REPLY_MESSAGES(95, NotificationActionIconPosition.BOTTOM),
        ACCEPT_INCOMING_CALL(96, NotificationActionIconPosition.RIGHT),
        REJECT_INCOMING_CALL(97, NotificationActionIconPosition.LEFT),
        DISMISS_NOTIFICATION(98, NotificationActionIconPosition.LEFT),
        BLOCK_APPLICATION(99, null),
        ;

        private final int code;
        private final NotificationActionIconPosition notificationActionIconPosition;

        NotificationAction(int code, NotificationActionIconPosition notificationActionIconPosition) {
            this.code = code;
            this.notificationActionIconPosition = notificationActionIconPosition;
        }

        public static NotificationAction fromCode(final int code) {
            for (final NotificationAction notificationAction : NotificationAction.values()) {
                if (notificationAction.code == code) {
                    return notificationAction;
                }
            }
            throw new IllegalArgumentException("Unknown notification action code " + code);
        }

    }

    enum NotificationActionIconPosition { //educated guesses based on the icons' positions on vívomove style
        BOTTOM, //or is it reply?
        RIGHT, //or is it accept?
        LEFT, //or is it dismiss/refuse?
    }
    public static class Upload {

        private NotificationFragment currentlyUploading;

        public NotificationDataMessage setCurrentlyUploading(NotificationFragment currentlyUploading) {
            this.currentlyUploading = currentlyUploading;
            return currentlyUploading.take();
        }

        private GFDIMessage processUploadProgress(NotificationDataStatusMessage notificationDataStatusMessage) {
            if (null == currentlyUploading) {
                LOG.warn("Received Upload Progress but we are not sending any notification");
                return null;
            }
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

    public static class NotificationFragment {
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
