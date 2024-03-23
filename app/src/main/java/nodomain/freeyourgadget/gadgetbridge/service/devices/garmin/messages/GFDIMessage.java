package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.GFDIStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.GenericStatusMessage;

public abstract class GFDIMessage {
    public static final int MESSAGE_RESPONSE = 5000; //TODO: MESSAGE_STATUS is a better name?
    public static final int MESSAGE_REQUEST = 5001;
    public static final int MESSAGE_DOWNLOAD_REQUEST = 5002;
    public static final int MESSAGE_UPLOAD_REQUEST = 5003;
    public static final int MESSAGE_FILE_TRANSFER_DATA = 5004;
    public static final int MESSAGE_CREATE_FILE_REQUEST = 5005;
    public static final int MESSAGE_DIRECTORY_FILE_FILTER_REQUEST = 5007;
    public static final int MESSAGE_FILE_READY = 5009;
    public static final int MESSAGE_FIT_DEFINITION = 5011;
    public static final int MESSAGE_FIT_DATA = 5012;
    public static final int MESSAGE_WEATHER_REQUEST = 5014;
    public static final int MESSAGE_BATTERY_STATUS = 5023;
    public static final int MESSAGE_DEVICE_INFORMATION = 5024;
    public static final int MESSAGE_DEVICE_SETTINGS = 5026;
    public static final int MESSAGE_SYSTEM_EVENT = 5030;
    public static final int MESSAGE_SUPPORTED_FILE_TYPES_REQUEST = 5031;
    public static final int MESSAGE_NOTIFICATION_SOURCE = 5033;
    public static final int MESSAGE_GNCS_CONTROL_POINT_REQUEST = 5034;
    public static final int MESSAGE_GNCS_DATA_SOURCE = 5035;
    public static final int MESSAGE_NOTIFICATION_SERVICE_SUBSCRIPTION = 5036;
    public static final int MESSAGE_SYNC_REQUEST = 5037;
    public static final int MESSAGE_FIND_MY_PHONE = 5039;
    public static final int MESSAGE_CANCEL_FIND_MY_PHONE = 5040;
    public static final int MESSAGE_MUSIC_CONTROL = 5041;
    public static final int MESSAGE_MUSIC_CONTROL_CAPABILITIES = 5042;
    public static final int MESSAGE_PROTOBUF_REQUEST = 5043;
    public static final int MESSAGE_PROTOBUF_RESPONSE = 5044;
    public static final int MESSAGE_MUSIC_CONTROL_ENTITY_UPDATE = 5049;
    public static final int MESSAGE_CONFIGURATION = 5050;
    public static final int MESSAGE_CURRENT_TIME_REQUEST = 5052;
    public static final int MESSAGE_AUTH_NEGOTIATION = 5101;
    protected static final Logger LOG = LoggerFactory.getLogger(GFDIMessage.class);
    protected final ByteBuffer response = ByteBuffer.allocate(1000);
    protected GFDIStatusMessage statusMessage;

    public static GFDIMessage parseIncoming(byte[] message) {
        final MessageReader messageReader = new MessageReader(message);

        final int messageType = messageReader.readShort();
        try {
//            Class<? extends GFDIMessage> objectClass = GarminMessage.getClassFromId(messageType);
            Method m = GarminMessage.getClassFromId(messageType).getMethod("parseIncoming", MessageReader.class, int.class);
            return GarminMessage.getClassFromId(messageType).cast(m.invoke(null, messageReader, messageType));
        } catch (Exception e) {
            LOG.error("UNHANDLED GFDI MESSAGE TYPE {}, MESSAGE {}", messageType, message);
            return new UnhandledMessage(messageType);
        }
    }

    protected abstract boolean generateOutgoing();

    public byte[] getOutgoingMessage() {
        response.clear();
        boolean toSend = generateOutgoing();
        response.order(ByteOrder.LITTLE_ENDIAN);
        if (!toSend)
            return null;
        addLengthAndChecksum();
        response.flip();

        final byte[] packet = new byte[response.limit()];
        response.get(packet);
        return packet;
    }

    protected GFDIStatusMessage getStatusMessage(int messageType) {
        return new GenericStatusMessage(messageType, Status.ACK);
    }

    public GBDeviceEvent getGBDeviceEvent() {
        return null;
    }

    public byte[] getAckBytestream() {
        if (null == this.statusMessage) {
            return null;
        }
        return this.statusMessage.getOutgoingMessage();
    }

    private void addLengthAndChecksum() {
        response.putShort(0, (short) (response.position() + 2));
        response.putShort((short) ChecksumCalculator.computeCrc(response.asReadOnlyBuffer(), 0, response.position()));
    }

    public enum GarminMessage {
        RESPONSE(5000, GFDIStatusMessage.class), //TODO: STATUS is a better name?
        SYSTEM_EVENT(5030, SystemEventMessage.class),
        DEVICE_INFORMATION(5024, DeviceInformationMessage.class),
        DEVICE_SETTINGS(5026, SetDeviceSettingsMessage.class),
        FIND_MY_PHONE(5039, FindMyPhoneRequestMessage.class),
        CANCEL_FIND_MY_PHONE(5040, FindMyPhoneRequestMessage.class),
        MUSIC_CONTROL(5041, MusicControlMessage.class),
        MUSIC_CONTROL_CAPABILITIES(5042, MusicControlCapabilitiesMessage.class),
        PROTOBUF_REQUEST(5043, ProtobufMessage.class),
        PROTOBUF_RESPONSE(5044, ProtobufMessage.class),
        MUSIC_CONTROL_ENTITY_UPDATE(5049, MusicControlEntityUpdateMessage.class),
        CONFIGURATION(5050, ConfigurationMessage.class),
        CURRENT_TIME_REQUEST(5052, CurrentTimeRequestMessage.class),
        ;
        private final Class<? extends GFDIMessage> objectClass;
        private final int id;

        GarminMessage(int id, Class<? extends GFDIMessage> objectClass) {
            this.id = id;
            this.objectClass = objectClass;
        }

        public static Class<? extends GFDIMessage> getClassFromId(final int id) {
            for (final GarminMessage garminMessage : GarminMessage.values()) {
                if (garminMessage.getId() == id) {
                    return garminMessage.getObjectClass();
                }
            }
            return null;
        }

        public static GarminMessage fromId(final int id) {
            for (final GarminMessage garminMessage : GarminMessage.values()) {
                if (garminMessage.getId() == id) {
                    return garminMessage;
                }
            }
            return null;
        }
        public int getId() {
            return id;
        }

        private Class<? extends GFDIMessage> getObjectClass() {
            return objectClass;
        }
    }

    public enum Status {
        ACK,
        NAK,
        UNSUPPORTED,
        DECODE_ERROR,
        CRC_ERROR,
        LENGTH_ERROR;

        @Nullable
        public static Status fromCode(final int code) {
            for (final Status status : Status.values()) {
                if (status.ordinal() == code) {
                    return status;
                }
            }

            return null;
        }

    }

}
