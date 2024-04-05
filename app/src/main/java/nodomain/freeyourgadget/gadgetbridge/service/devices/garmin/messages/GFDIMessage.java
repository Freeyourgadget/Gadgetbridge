package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.GFDIStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.GenericStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class GFDIMessage {
    public static final int MESSAGE_REQUEST = 5001;
    public static final int MESSAGE_DOWNLOAD_REQUEST = 5002;
    public static final int MESSAGE_UPLOAD_REQUEST = 5003;
    public static final int MESSAGE_FILE_TRANSFER_DATA = 5004;
    public static final int MESSAGE_CREATE_FILE_REQUEST = 5005;
    public static final int MESSAGE_DIRECTORY_FILE_FILTER_REQUEST = 5007;
    public static final int MESSAGE_FILE_READY = 5009;
    public static final int MESSAGE_BATTERY_STATUS = 5023;
    public static final int MESSAGE_SUPPORTED_FILE_TYPES_REQUEST = 5031;
    public static final int MESSAGE_NOTIFICATION_SOURCE = 5033;
    public static final int MESSAGE_GNCS_CONTROL_POINT_REQUEST = 5034;
    public static final int MESSAGE_GNCS_DATA_SOURCE = 5035;
    public static final int MESSAGE_NOTIFICATION_SERVICE_SUBSCRIPTION = 5036;
    public static final int MESSAGE_SYNC_REQUEST = 5037;
    public static final int MESSAGE_AUTH_NEGOTIATION = 5101;
    protected static final Logger LOG = LoggerFactory.getLogger(GFDIMessage.class);
    protected final ByteBuffer response = ByteBuffer.allocate(1000);
    protected GFDIStatusMessage statusMessage;
    protected GarminMessage garminMessage;

    public static GFDIMessage parseIncoming(byte[] message) {
        final MessageReader messageReader = new MessageReader(message);

        final int messageType = messageReader.readShort();
        try {
            GarminMessage garminMessage = GarminMessage.fromId(messageType);
            Method m = garminMessage.objectClass.getMethod("parseIncoming", MessageReader.class, GarminMessage.class);
            return garminMessage.objectClass.cast(m.invoke(null, messageReader, garminMessage));
        } catch (Exception e) {
            LOG.error("UNHANDLED GFDI MESSAGE TYPE {}, MESSAGE {}", messageType, message);
            return new UnhandledMessage(messageType);
        } finally {
            messageReader.warnIfLeftover();
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

    protected GFDIStatusMessage getStatusMessage() {
        return new GenericStatusMessage(garminMessage, Status.ACK);
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
        FIT_DEFINITION(5011, FitDefinitionMessage.class),
        FIT_DATA(5012, FitDataMessage.class),
        WEATHER_REQUEST(5014, WeatherMessage.class),
        DEVICE_INFORMATION(5024, DeviceInformationMessage.class),
        DEVICE_SETTINGS(5026, SetDeviceSettingsMessage.class),
        SYSTEM_EVENT(5030, SystemEventMessage.class),
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

        @Nullable
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

        public static Status fromCode(final int code) {
            for (final Status status : Status.values()) {
                if (status.ordinal() == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status code " + code);
        }

    }

    protected static class MessageReader extends GarminByteBufferReader {

        private final int payloadSize;

        public MessageReader(byte[] data) {
            super(data);
            this.byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            this.payloadSize = readShort(); //includes CRC
            checkSize();
            checkCRC();
            this.byteBuffer.limit(payloadSize - 2); //remove CRC
        }

        public void skip(int offset) {
            if (remaining() < offset) throw new IllegalStateException();
            byteBuffer.position(byteBuffer.position() + offset);
        }

        private void checkSize() {
            if (payloadSize != byteBuffer.capacity()) {
                LOG.error("Received GFDI packet with invalid length: {} vs {}", payloadSize, byteBuffer.capacity());
                throw new IllegalArgumentException("Received GFDI packet with invalid length");
            }
        }

        private void checkCRC() {
            final int crc = Short.toUnsignedInt(byteBuffer.getShort(payloadSize - 2));
            final int correctCrc = ChecksumCalculator.computeCrc(byteBuffer.asReadOnlyBuffer(), 0, payloadSize - 2);
            if (crc != correctCrc) {
                LOG.error("Received GFDI packet with invalid CRC: {} vs {}", crc, correctCrc);
                throw new IllegalArgumentException("Received GFDI packet with invalid CRC");
            }
        }

        public void warnIfLeftover() {
            if (byteBuffer.hasRemaining() && byteBuffer.position() < (byteBuffer.limit())) {
                int pos = byteBuffer.position();
                int numBytes = (byteBuffer.limit()) - byteBuffer.position();
                byte[] leftover = new byte[numBytes];
                byteBuffer.get(leftover);
                byteBuffer.position(pos);
                LOG.warn("Leftover bytes when parsing message. Bytes: {}, complete message: {}", GB.hexdump(leftover), GB.hexdump(byteBuffer.array()));
            }
        }
    }
}
