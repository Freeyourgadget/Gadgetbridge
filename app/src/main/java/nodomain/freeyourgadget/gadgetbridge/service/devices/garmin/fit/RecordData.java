package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType.STRING;

public class RecordData {

    private final RecordHeader recordHeader;
    private final GlobalFITMessage globalFITMessage;
    protected ByteBuffer valueHolder;
    private final List<FieldData> fieldDataList;

    public RecordData(RecordDefinition recordDefinition) {
        if (null == recordDefinition.getFieldDefinitions())
            throw new IllegalArgumentException("Cannot create record data without FieldDefinitions " + recordDefinition);

        fieldDataList = new ArrayList<>();

        this.recordHeader = recordDefinition.getRecordHeader();
        this.globalFITMessage = recordDefinition.getGlobalFITMessage();

        int totalSize = 0;

        for (FieldDefinition fieldDef :
                recordDefinition.getFieldDefinitions()) {
            fieldDataList.add(new FieldData(fieldDef, totalSize));
            totalSize += fieldDef.getSize();
        }

        if (recordHeader.isDefinition() && recordDefinition.getDevFieldDefinitions() != null) {
            for (DevFieldDefinition fieldDef :
                    recordDefinition.getDevFieldDefinitions()) {
                FieldDefinition temp = new FieldDefinition(fieldDef.getFieldDefinitionNumber(), fieldDef.getSize(), fieldDef.getBaseType(), fieldDef.getName());
                fieldDataList.add(new FieldData(temp, totalSize));
                totalSize += fieldDef.getSize();
            }
        }

        this.valueHolder = ByteBuffer.allocate(totalSize);
        valueHolder.order(recordDefinition.getByteOrder());

        for (FieldData fieldData :
                fieldDataList) {
            fieldData.invalidate();
        }

    }

    public GlobalFITMessage getGlobalFITMessage() {
        return globalFITMessage;
    }

    public void parseDataMessage(GarminByteBufferReader garminByteBufferReader) {
        garminByteBufferReader.setByteOrder(valueHolder.order());
        for (FieldData fieldData : fieldDataList) {
            fieldData.parseDataMessage(garminByteBufferReader);
        }
    }

    public void generateOutgoingDataPayload(MessageWriter writer) {
        writer.writeByte(recordHeader.generateOutgoingDataPayload());
        writer.writeBytes(valueHolder.array());
    }

    public void setFieldByNumber(int number, Object... value) {
        boolean found = false;
        for (FieldData fieldData :
                fieldDataList) {
            if (fieldData.getNumber() == number) {
                fieldData.encode(value);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown field number " + number);
        }
    }

    public void setFieldByName(String name, Object... value) {
        boolean found = false;
        for (FieldData fieldData :
                fieldDataList) {
            if (fieldData.getName().equals(name)) {
                fieldData.encode(value);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown field name " + name);
        }
    }

    public Object getFieldByNumber(int number) {
        for (FieldData fieldData :
                fieldDataList) {
            if (fieldData.getNumber() == number) {
                return fieldData.decode();
            }
        }
        throw new IllegalArgumentException("Unknown field number " + number);
    }

    public Object getFieldByName(String name) {
        for (FieldData fieldData :
                fieldDataList) {
            if (fieldData.getName().equals(name)) {
                return fieldData.decode();
            }
        }
        throw new IllegalArgumentException("Unknown field name " + name);
    }

    public int[] getFieldsNumbers() {
        int[] arr = new int[fieldDataList.size()];
        int count = 0;
        for (FieldData fieldData : fieldDataList) {
            int number = fieldData.getNumber();
            arr[count++] = number;
        }
        return arr;
    }

    @NonNull
    public String toString() {
        StringBuilder oBuilder = new StringBuilder();
        for (FieldData fieldData :
                fieldDataList) {
            if (fieldData.getName() != null && !fieldData.getName().equals("")) {
                oBuilder.append(fieldData.getName());
            } else {
                oBuilder.append("unknown_" + fieldData.getNumber());
            }
            oBuilder.append(fieldData);
            oBuilder.append(": ");
            Object o = fieldData.decode();
            if (o instanceof Object[]) {
                oBuilder.append("[");
                oBuilder.append(org.apache.commons.lang3.StringUtils.join((Object[]) o, ","));
                oBuilder.append("]");
            } else {
                oBuilder.append(o);
            }
            oBuilder.append(" ");
        }

        return oBuilder.toString();
    }

    public LocalMessage getLocalMessage() {
        return recordHeader.getLocalMessage();
    }

    private class FieldData {
        private final FieldDefinition fieldDefinition;
        private final int position;
        private final int size;
        private final int baseSize;

        public FieldData(FieldDefinition fieldDefinition, int position) {
            this.fieldDefinition = fieldDefinition;
            this.position = position;
            this.size = fieldDefinition.getSize();
            this.baseSize = fieldDefinition.getBaseType().getSize();
        }

        private String getName() {
            return fieldDefinition.getName();
        }

        private int getNumber() {
            return fieldDefinition.getNumber();
        }

        private void invalidate() {
            goToPosition();
            if (STRING.equals(fieldDefinition.getBaseType())) {
                for (int i = 0; i < size; i++) {
                    valueHolder.put((byte) 0);
                }
                return;
            }
            for (int i = 0; i < (size / baseSize); i++) {
                fieldDefinition.invalidate(valueHolder);
            }
        }

        private void goToPosition() {
            valueHolder.position(position);
        }

        private void parseDataMessage(GarminByteBufferReader garminByteBufferReader) {
            goToPosition();
            valueHolder.put(garminByteBufferReader.readBytes(size));
        }

        private void encode(Object... objects) {
            if (objects[0] instanceof boolean[] || objects[0] instanceof short[] || objects[0] instanceof int[] || objects[0] instanceof long[] || objects[0] instanceof float[] || objects[0] instanceof double[]) {
                throw new IllegalArgumentException("Array of primitive types not supported, box them to objects");
            }
            goToPosition();
            final int slots = size / baseSize;
            int i = 0;
            for (Object o : objects) {
                if (i++ >= slots) {
                    throw new IllegalArgumentException("Number of elements in array was too big for the field");
                }
                if (STRING.equals(fieldDefinition.getBaseType())) {
                    final byte[] bytes = ((String) o).getBytes(StandardCharsets.UTF_8);
                    valueHolder.put(Arrays.copyOf(bytes, Math.min(this.size - 1, bytes.length)));
                    valueHolder.put((byte) 0);
                    return;
                }
                fieldDefinition.encode(valueHolder, o);
            }
        }

        private Object decode() {
            goToPosition();
            if (STRING.equals(fieldDefinition.getBaseType())) {
                final byte[] bytes = new byte[size];
                valueHolder.get(bytes);
                final int zero = ArrayUtils.indexOf((byte) 0, bytes);
                if (zero < 0) {
                    return new String(bytes, StandardCharsets.UTF_8);
                }
                return new String(bytes, 0, zero, StandardCharsets.UTF_8);
            }
            if (size > baseSize) {
                Object[] arr = new Object[size / baseSize];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = fieldDefinition.decode(valueHolder);
                }
                return arr;
            }
            return fieldDefinition.decode(valueHolder);
        }

        public String toString() {
            return "(" + fieldDefinition.getBaseType().name() + "/" + size + ")";
        }
    }
}
