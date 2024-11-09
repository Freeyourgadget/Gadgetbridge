package nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BandWPSeriesRequest {

    private static final Logger LOG = LoggerFactory.getLogger(BandWPSeriesRequest.class);

    BandWMessageType messageType;
    final byte namespace;
    final byte commandId;

    private final MessageBufferPacker payloadPacker = MessagePack.newDefaultBufferPacker();

    public BandWPSeriesRequest(byte mNamespace, byte mCommandId) throws IOException {
        messageType = BandWMessageType.REQUEST_WITHOUT_PAYLOAD;
        namespace = mNamespace;
        commandId = mCommandId;
    }

    public BandWPSeriesRequest addToPayload(int value) throws IOException {
        payloadPacker.packInt(value);
        messageType = BandWMessageType.REQUEST_WITH_PAYLOAD;
        return this;
    }

    public BandWPSeriesRequest addToPayload(byte value) throws IOException {
        payloadPacker.packByte(value);
        messageType = BandWMessageType.REQUEST_WITH_PAYLOAD;
        return this;
    }

    public BandWPSeriesRequest addToPayload(boolean value) throws IOException {
        payloadPacker.packBoolean(value);
        messageType = BandWMessageType.REQUEST_WITH_PAYLOAD;
        return this;
    }

    public BandWPSeriesRequest addToPayload(String value) throws IOException {
        payloadPacker.packString(value);
        messageType = BandWMessageType.REQUEST_WITH_PAYLOAD;
        return this;
    }

    public byte[] finishAndGetBytes() {
        byte[] payload = payloadPacker.toByteArray();
        byte len = (byte) ((this.messageType == BandWMessageType.REQUEST_WITHOUT_PAYLOAD) ? 4 : 6 + payload.length);
        byte[] out = addMessageType(new byte[len+1], messageType.value);
        out[0] = len;
        out[3] = commandId;
        out[4] = namespace;
        if (messageType == BandWMessageType.REQUEST_WITH_PAYLOAD) {
            addShort(out, 5, payload.length);
            System.arraycopy(payload, 0, out, 7, payload.length);
        }
        try {
            payloadPacker.close();
        } catch (IOException e) {
            LOG.warn("Failed to close payloadPacker");
        }
        return out;
    }

    private byte[] addMessageType(byte[] target, int value) {
        return addShort(target, 1, value);
    }

    private byte[] addShort(byte[] target, int position, int value) {
        byte valueLo = (byte) (value & 0xff);
        byte valueHi = (byte) (value >> 8);
        target[position] = valueLo;
        target[position+1] = valueHi;
        return target;
    }
}
