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
import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageWriter;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FitMessage {
    public final FitMessageDefinition definition;
    private final SparseArray<Object> fieldValues = new SparseArray<>();
    private final Map<String, Object> fieldValuesPerName = new HashMap<>();

    public FitMessage(FitMessageDefinition definition) {
        this.definition = definition;
    }

    public void setField(int fieldNumber, Object value) {
        // TODO: Support arrays
        fieldValues.append(fieldNumber, value);
        final FitMessageFieldDefinition fieldDefinition = definition.getField(fieldNumber);
        fieldValuesPerName.put(fieldDefinition.fieldName, value);
    }

    public void setField(String fieldName, Object value) {
        final FitMessageFieldDefinition fieldDefinition = definition.findField(fieldName);
        if (fieldDefinition == null) throw new IllegalArgumentException("Unknown field name " + fieldName);
        setField(fieldDefinition.fieldNumber, value);
    }

    public Object getField(int fieldNumber) {
        return fieldValues.get(fieldNumber);
    }

    public Object getField(String fieldName) {
        return fieldValuesPerName.get(fieldName);
    }

    public String getStringField(String fieldName) {
        return (String) getField(fieldName);
    }

    public Integer getIntegerField(String fieldName) {
        return (Integer) getField(fieldName);
    }

    public Double getNumericField(String fieldName) {
        return (Double) getField(fieldName);
    }

    public Boolean getBooleanField(String fieldName) {
        final Integer value = (Integer) getField(fieldName);
        if (value == null) return null;
        int v = value;
        return v == FitBool.INVALID ? null : (v != 0);
    }

    public boolean isBooleanFieldTrue(String fieldName) {
        final Boolean value = getBooleanField(fieldName);
        return value != null && value;
    }

    public void writeToMessage(MessageWriter writer) {
        writer.writeByte(definition.localMessageID);
        for (FitMessageFieldDefinition fieldDefinition : definition.fieldDefinitions) {
            final Object value = fieldValues.get(fieldDefinition.fieldNumber, fieldDefinition.defaultValue);
            writeFitValueToMessage(writer, value, fieldDefinition.fieldType, fieldDefinition.fieldSize);
        }
    }

    private static void writeFitValueToMessage(MessageWriter writer, Object value, FitFieldBaseType type, int size) {
        switch (type) {
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
            case BYTE:
                writeFitNumberToMessage(writer, ((Number) value).intValue(), size);
                break;
            case SINT64:
            case UINT64:
            case UINT64Z:
                writeFitNumberToMessage(writer, ((Number) value).longValue(), size);
                break;
            case STRING:
                writeFitStringToMessage(writer, (String) value, size);
                break;
            // TODO: Float data types
            default:
                throw new IllegalArgumentException("Unable to write value of type " + type);
        }
    }

    private static void writeFitStringToMessage(MessageWriter writer, String value, int size) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int valueSize = Math.min(bytes.length, size - 1);
        writer.writeBytes(bytes, 0, valueSize);
        for (int i = valueSize; i < size; ++i) {
            writer.writeByte(0);
        }
    }

    private static void writeFitNumberToMessage(MessageWriter writer, long value, int size) {
        switch (size) {
            case 1:
                writer.writeByte((int) value);
                break;
            case 2:
                writer.writeShort((int) value);
                break;
            case 4:
                writer.writeInt((int) value);
                break;
            case 8:
                writer.writeLong(value);
                break;
            default:
                throw new IllegalArgumentException("Unable to write number of size " + size);
        }
    }

    @Override
    @NonNull
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(this.definition.messageName);
        result.append(System.lineSeparator());
        for (Map.Entry<String, Object> field : fieldValuesPerName.entrySet()) {
            result.append('\t');
            result.append(field.getKey());
            result.append(": ");
            result.append(valueToString(field.getValue()));
            result.append(System.lineSeparator());
        }
        return result.toString();
    }

    @NonNull
    private static String valueToString(Object value) {
        if (value == null) return "null";
        final Class<?> clazz = value.getClass();
        if (clazz.isArray()) {
            final StringBuilder result = new StringBuilder();
            result.append('[');
            final int length = Array.getLength(value);
            for (int i = 0; i < length; ++i) {
                if (i > 0) result.append(", ");
                result.append(valueToString(Array.get(value, i)));
            }
            result.append(']');
            return result.toString();
        } else {
            return String.valueOf(value);
        }
    }
}
