package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType.STRING;

public class RecordData {

    private final RecordHeader recordHeader;
    protected ByteBuffer valueHolder;
    private List<FieldData> fieldDataList;

    public RecordData(RecordDefinition recordDefinition) {
        fieldDataList = new ArrayList<>();

        this.recordHeader = recordDefinition.getRecordHeader();

        int totalSize = 0;


        for (FieldDefinition fieldDef :
                recordDefinition.getFieldDefinitions()) {
            fieldDataList.add(new FieldData(fieldDef, totalSize));
            totalSize += fieldDef.getSize();

        }

        this.valueHolder = ByteBuffer.allocate(totalSize);
        valueHolder.order(recordDefinition.getByteOrder());

        for (FieldData fieldData :
                fieldDataList) {
            fieldData.invalidate();
        }

    }

    public void parseDataMessage(MessageReader reader) {
        reader.setByteOrder(valueHolder.order());
        for (FieldData fieldData : fieldDataList) {
            fieldData.parseDataMessage(reader);
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

    public String toString() {
        StringBuilder oBuilder = new StringBuilder();
        for (FieldData fieldData :
                fieldDataList) {
            if (fieldData.getName() != null && !fieldData.getName().equals("")) {
                oBuilder.append(fieldData.getName());
            } else {
                oBuilder.append("unknown_" + fieldData.getNumber());
            }
            oBuilder.append(": ");
            oBuilder.append(fieldData.decode());
            oBuilder.append(" ");
        }

        return oBuilder.toString();
    }
    private class FieldData {
        private FieldDefinition fieldDefinition;
        private final int position;
        private final int size;

        public FieldData(FieldDefinition fieldDefinition, int position) {
            this.fieldDefinition = fieldDefinition;
            this.position = position;
            this.size = fieldDefinition.getSize();
        }


        public String getName() {
            return fieldDefinition.getName();
        }

        public int getNumber() {
            return fieldDefinition.getLocalNumber();
        }

        public void invalidate() {
            goToPosition();
            if (STRING.equals(fieldDefinition.getBaseType())) {
                for (int i = 0; i < size; i++) {
                    valueHolder.put((byte) 0);
                }
                return;
            }
            fieldDefinition.invalidate(valueHolder);
        }

        private void goToPosition() {
            valueHolder.position(position);
        }

        public void parseDataMessage(MessageReader reader) {
            goToPosition();
            valueHolder.put(reader.readBytes(size));
        }

        public void encode(Object... objects) {
            if (objects.length > 1)
                throw new IllegalArgumentException("Array of values not supported yet"); //TODO: handle arrays
            Object o = objects[0];
            goToPosition();
            if (STRING.equals(fieldDefinition.getBaseType())) {
                final byte[] bytes = ((String) o).getBytes(StandardCharsets.UTF_8);
                valueHolder.put(Arrays.copyOf(bytes, Math.min(this.size - 1, bytes.length)));
                valueHolder.put((byte) 0);
                return;
            }
            fieldDefinition.encode(valueHolder, o);
        }

        public Object decode() {
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
            //TODO: handle arrays
            return fieldDefinition.decode(valueHolder);

        }
    }
}
