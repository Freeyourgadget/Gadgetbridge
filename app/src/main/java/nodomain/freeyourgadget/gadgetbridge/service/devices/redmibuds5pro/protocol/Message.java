/*  Copyright (C) 2024 Jonathan Gobbo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Message {

    private static final byte[] MESSAGE_HEADER = {(byte) 0xfe, (byte) 0xdc, (byte) 0xba};
    private static final byte MESSAGE_TRAILER = (byte) 0xef;

    private static final int MESSAGE_OFFSET = MESSAGE_HEADER.length;

    private final MessageType type;
    private final Opcode opcode;
    private final byte sequenceNumber;
    private final byte[] payload;

    public Message(final MessageType type, final Opcode opcode, final byte sequenceNumber, final byte[] payload) {
        this.type = type;
        this.opcode = opcode;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    public static Message fromBytes(byte[] message) {
        MessageType type = MessageType.fromCode(message[MESSAGE_OFFSET]);
        Opcode opcode = Opcode.fromCode(message[MESSAGE_OFFSET + 1]);

        int payloadOffset = MESSAGE_OFFSET + ((!type.isRequest()) ? 6 : 5);
        byte sequenceNumber = message[payloadOffset - 1];

        int actualPayloadLength = message.length - payloadOffset - 1;
        byte[] payload = new byte[actualPayloadLength];
        System.arraycopy(message, payloadOffset, payload, 0, actualPayloadLength);
        return new Message(type, opcode, sequenceNumber, payload);
    }

    public static List<Message> splitPiggybackedMessages(byte[] input) {
        List<Message> messages = new ArrayList<>();

        List<Integer> startHeader = new ArrayList<>();
        for (int i = 0; i < input.length - 3; i++) {
            if (input[i] == MESSAGE_HEADER[0] && input[i + 1] == MESSAGE_HEADER[1] && input[i + 2] == MESSAGE_HEADER[2]) {
                startHeader.add(i);
            }
        }

        for (int i = 0; i < startHeader.size(); i++) {
            if (i == startHeader.size() - 1) {
                messages.add(fromBytes(Arrays.copyOfRange(input, startHeader.get(i), input.length)));
            } else {
                messages.add(fromBytes(Arrays.copyOfRange(input, startHeader.get(i), startHeader.get(i + 1))));
            }
        }
        return messages;
    }

    public byte[] encode() {

        int size = (!type.isRequest()) ? 2 : 1;
        final ByteBuffer buf = ByteBuffer.allocate(payload.length + 8 + size);
        int payloadLength = payload.length + size;

        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put(MESSAGE_HEADER);
        buf.put(type.getCode());
        buf.put(opcode.getOpcode());
        buf.putShort((short) payloadLength);
        if (!type.isRequest()) {
            buf.put((byte) 0x00);
        }
        buf.put(sequenceNumber);
        buf.put(payload);
        buf.put(MESSAGE_TRAILER);

        return buf.array();
    }

    @NonNull
    @Override
    public String toString() {
        return "Message{" + "type=" + type + ", opcode=" + opcode + ", sequenceNumber=" + sequenceNumber + ", payload=" + hexdump(payload) + '}';
    }

    public MessageType getType() {
        return type;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public byte getSequenceNumber() {
        return sequenceNumber;
    }

    public byte[] getPayload() {
        return payload;
    }
}
