/*  Copyright (C) 2020-2023 Petr Kadlec

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FitSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(FitSerializer.class);

    private final SparseBooleanArray knownMessageIDs = new SparseBooleanArray(16);
    private final SparseIntArray localMessageIDs = new SparseIntArray(16);
    private final SparseArray<FitLocalMessageDefinition> localMessageDefinitions;

    // “.FIT” – magic value indicating a .FIT file
    private static final int FIT_MAGIC = 0x5449462E;

    private static final int FLAG_NORMAL_HEADER = 0x80;
    private static final int FLAG_DEFINITION_MESSAGE = 0x40;
    private static final int FLAG_DEVELOPER_FIELDS = 0x20;
    private static final int MASK_LOCAL_MESSAGE_TYPE = 0x0F;
    private static final int MASK_TIME_OFFSET = 0x1F;
    private static final int MASK_COMPRESSED_LOCAL_MESSAGE_TYPE = 0x60;

    public FitSerializer() {
        this(new SparseArray<FitLocalMessageDefinition>(16));
    }

    public FitSerializer(SparseArray<FitLocalMessageDefinition> initialDefinitions) {
        this.localMessageDefinitions = initialDefinitions;
        for (int i = 0; i < initialDefinitions.size(); ++i) {
            final int localId = initialDefinitions.keyAt(i);
            final FitLocalMessageDefinition definition = initialDefinitions.valueAt(i);
            knownMessageIDs.put(definition.globalDefinition.globalMessageID, true);
            localMessageIDs.put(definition.globalDefinition.globalMessageID, localId);
        }
    }

    public byte[] serializeFitFile(List<FitMessage> messages) {
        final MessageWriter writer = new MessageWriter();
        writer.writeByte(14);
        writer.writeByte(0x10);
        writer.writeShort(2057);
        // dataSize will be rewritten later
        writer.writeInt(0);
        writer.writeInt(FIT_MAGIC);
        // CRC will be rewritten later
        writer.writeShort(0);

        // first, gather additional needed definitions (if any)
        for (final FitMessage message : messages) {
            final FitMessageDefinition messageDefinition = message.definition;
            final int globalMessageID = messageDefinition.globalMessageID;
            if (!knownMessageIDs.get(globalMessageID)) {
                LOG.debug("FitSerializer needs to add definition for {}", globalMessageID);
                final int localMessageID = localMessageIDs.size() == 0 ? 0 : localMessageIDs.keyAt(localMessageIDs.size() - 1) + 1;
                localMessageIDs.put(globalMessageID, localMessageID);
                knownMessageIDs.put(globalMessageID, true);
                final List<FitMessageFieldDefinition> fieldDefinitions = messageDefinition.fieldDefinitions;
                final List<FitLocalFieldDefinition> localFieldDefinitions = new ArrayList<>(fieldDefinitions.size());
                for (FitMessageFieldDefinition definition : fieldDefinitions) {
                    localFieldDefinitions.add(new FitLocalFieldDefinition(definition, definition.fieldSize, definition.fieldType));
                }
                localMessageDefinitions.put(localMessageID, new FitLocalMessageDefinition(messageDefinition, localFieldDefinitions));
            }
        }
        // now, write definition messages for all used message types
        final SparseBooleanArray definedMessages = new SparseBooleanArray();
        for (final FitMessage message : messages) {
            int localMessageID = localMessageIDs.get(message.definition.globalMessageID);
            if (!definedMessages.get(localMessageID)) {
                definedMessages.put(localMessageID, true);

                writeDefinitionMessage(writer, localMessageID, localMessageDefinitions.get(localMessageID));
            }
        }

        // and now, write the data messages
        for (final FitMessage message : messages) {
            int localMessageID = localMessageIDs.get(message.definition.globalMessageID);
            final FitLocalMessageDefinition localMessageDefinition = localMessageDefinitions.get(localMessageID);
            writeDataMessage(writer, message, localMessageID, localMessageDefinition);
        }

        writer.writeShort(ChecksumCalculator.computeCrc(writer.peekBytes(), 14, writer.getSize() - 14));

        final byte[] bytes = writer.getBytes();
        // rewrite size
        BLETypeConversions.writeUint32(bytes, 4, bytes.length - 14 - 2);
        // rewrite header CRC
        BLETypeConversions.writeUint16(bytes, 12, ChecksumCalculator.computeCrc(bytes, 0, 12));
        return bytes;
    }

    private void writeDefinitionMessage(MessageWriter writer, int localMessageID, FitLocalMessageDefinition localMessageDefinition) {
        writer.writeByte(FLAG_DEFINITION_MESSAGE | localMessageID);
        writer.writeByte(0);
        writer.writeByte(0);
        writer.writeShort(localMessageDefinition.globalDefinition.globalMessageID);
        writer.writeByte(localMessageDefinition.fieldDefinitions.size());
        for (FitLocalFieldDefinition localFieldDefinition : localMessageDefinition.fieldDefinitions) {
            writer.writeByte(localFieldDefinition.globalDefinition.fieldNumber);
            writer.writeByte(localFieldDefinition.size);
            writer.writeByte(localFieldDefinition.baseType.typeID);
        }
    }

    private void writeDataMessage(MessageWriter writer, FitMessage message, int localMessageID, FitLocalMessageDefinition localMessageDefinition) {
        writer.writeByte(localMessageID);

        for (FitLocalFieldDefinition localFieldDefinition : localMessageDefinition.fieldDefinitions) {
            Object value = message.getField(localFieldDefinition.globalDefinition.fieldNumber);
            if (value == null) {
                value = localFieldDefinition.baseType.invalidValue;
            }
            writeValue(writer, localFieldDefinition, value);
        }
    }

    private void writeValue(MessageWriter writer, FitLocalFieldDefinition fieldDefinition, Object value) {
        switch (fieldDefinition.globalDefinition.fieldType) {
            case ENUM:
            case SINT8:
            case UINT8:
            case SINT16:
            case UINT16:
            case SINT32:
            case UINT32:
            case UINT8Z:
            case UINT16Z:
            case UINT32Z:
            case SINT64:
            case UINT64:
            case UINT64Z:
                writeFitNumber(writer, value, fieldDefinition.size, fieldDefinition.globalDefinition.scale, fieldDefinition.globalDefinition.offset);
                break;
            case BYTE:
                if (fieldDefinition.size == 1) {
                    writer.writeByte((int) value);
                } else {
                    writer.writeBytes((byte[]) value);
                }
                break;
            case STRING:
                writeFitString(writer, (String) value, fieldDefinition.size);
                break;
            case FLOAT32:
                writeFloat32(writer, (float) value);
                break;
            case FLOAT64:
                writeFloat64(writer, (double) value);
                break;
            default:
                throw new IllegalArgumentException("Unable to write value of type " + fieldDefinition.baseType);
        }
    }

    private void writeFitString(MessageWriter writer, String value, int size) {
        if (value.length() >= size) throw new IllegalArgumentException("Too long string");
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writer.writeBytes(bytes);
        final byte[] zeroes = new byte[size - value.length()];
        writer.writeBytes(zeroes);
    }

    private void writeFloat32(MessageWriter writer, float value) {
        writer.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array(), 0, 4);
    }

    private void writeFloat64(MessageWriter writer, double value) {
        writer.writeBytes(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array(), 0, 4);
    }

    private void writeFitNumber(MessageWriter writer, Object value, int size, double scale, double offset) {
        if (scale == 0) {
            writeRawFitNumber(writer, value, size);
        } else {
            final long rawValue = Math.round((double) value * scale - offset);
            switch (size) {
                case 1:
                    writer.writeByte((int) rawValue);
                    break;
                case 2:
                    writer.writeShort((int) rawValue);
                    break;
                case 4:
                    writer.writeInt((int) rawValue);
                    break;
                case 8:
                    writer.writeLong(rawValue);
                    break;
                default:
                    throw new IllegalArgumentException("Unable to write number of size " + size);
            }
        }
    }

    private void writeRawFitNumber(MessageWriter writer, Object value, int size) {
        switch (size) {
            case 1:
                writer.writeByte((int) value);
                break;
            case 2:
                writer.writeShort((int) value);
                break;
            case 3: {
                // this is strange?
                byte[] bytes = new byte[4];
                BLETypeConversions.writeUint32(bytes, 0, (int) value);
                writer.writeBytes(bytes, 0, 3);
                break;
            }
            case 4:
                writer.writeInt((int) value);
                break;
            case 7: {
                // this is strange?
                byte[] bytes = new byte[8];
                BLETypeConversions.writeUint64(bytes, 0, (long) value);
                writer.writeBytes(bytes, 0, 7);
                break;
            }
            case 8:
                writer.writeLong((long) value);
                break;
            case 12: {
                // this is strange? (and probably losing precision anyway)
                final double val = (double) value;
                final long upper = Math.round(val / Long.MAX_VALUE);
                final long lower = Math.round(val - upper);
                writer.writeLong(lower);
                writer.writeInt((int) upper);
                break;
            }
            case 16: {
                // this is strange? (and probably losing precision anyway)
                final double val = (double) value;
                final long upper = Math.round(val / Long.MAX_VALUE);
                final long lower = Math.round(val - upper);
                writer.writeLong(lower);
                writer.writeLong(upper);
                break;
            }
            default:
                throw new IllegalArgumentException("Unable to read number of size " + size);
        }
    }
}
