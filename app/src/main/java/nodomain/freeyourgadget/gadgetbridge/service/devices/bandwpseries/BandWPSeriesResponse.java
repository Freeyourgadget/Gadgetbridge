package nodomain.freeyourgadget.gadgetbridge.service.devices.bandwpseries;

import org.bouncycastle.shaded.util.Arrays;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BandWPSeriesResponse {

    private static final Logger LOG = LoggerFactory.getLogger(BandWPSeriesResponse.class);

    BandWMessageType messageType;
    final byte namespace;
    final byte commandId;
    final int errorCode;
    final int payloadLength;
    final byte[] payload;

    public final MessageUnpacker payloadUnpacker;

    BandWPSeriesResponse(byte[] contents) {
        messageType = BandWMessageType.getByType(getUInt16(Arrays.copyOfRange(contents, 0, 2)));
        commandId = contents[2];
        namespace = contents[3];
        int payloadOffset = 6;
        if (messageType == BandWMessageType.RESPONSE_WITH_PAYLOAD || messageType == BandWMessageType.RESPONSE_WITHOUT_PAYLOAD) {
            errorCode = getUInt16(Arrays.copyOfRange(contents, 4, 6));
        } else {
            errorCode = 0;
            payloadOffset = 4;
        }
        if (messageType == null || !messageType.hasPayload || errorCode != 0) {
            payloadLength = 0;
            payload = null;
            payloadUnpacker = null;
        } else {
            payloadLength = getUInt16(Arrays.copyOfRange(contents, payloadOffset, payloadOffset + 2));
            payload = Arrays.copyOfRange(contents, payloadOffset + 2, contents.length);
            payloadUnpacker = MessagePack.newDefaultUnpacker(payload);
        }
    }

    private int getUInt16(byte[] buffer) {
        return (0xff & buffer[0]) | ((0xff & buffer[1]) << 8);
    }

    public String getPayloadString() {
        String value;
        try {
            value = payloadUnpacker.unpackString();
        } catch (IOException e) {
            LOG.warn("Failed to unpack String from payload {}", payload);
            return null;
        }
        return value;
    }

    public int[] getPayloadFixArray() {
        int length;
        try {
            length = payloadUnpacker.unpackArrayHeader();
        } catch (IOException e) {
            LOG.warn("Failed to unpack ArrayHeader from payload {}", payload);
            return null;
        }
        int[] values = new int[length];
        try {
            for (int i = 0; i < length; i++) {
                values[i] = payloadUnpacker.unpackInt();
            }
        } catch (IOException e) {
            LOG.warn("Failed to unpack byte from fixarray in payload {}", payload);
            return null;
        }
        return values;
    }

    public boolean getPayloadBoolean() throws IOException{
        return payloadUnpacker.unpackBoolean();
    }
}
