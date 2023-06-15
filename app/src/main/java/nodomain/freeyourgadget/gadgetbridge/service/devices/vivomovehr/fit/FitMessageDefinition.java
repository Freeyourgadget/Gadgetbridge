package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import android.util.SparseArray;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageWriter;

import java.util.Arrays;
import java.util.List;

public class FitMessageDefinition {
    public final String messageName;
    public final int globalMessageID;
    public final int localMessageID;
    public final List<FitMessageFieldDefinition> fieldDefinitions;
    public final SparseArray<FitMessageFieldDefinition> fieldsPerNumber;

    public FitMessageDefinition(String messageName, int globalMessageID, int localMessageID, FitMessageFieldDefinition... fieldDefinitions) {
        this.messageName = messageName;
        this.globalMessageID = globalMessageID;
        this.localMessageID = localMessageID;
        this.fieldDefinitions = Arrays.asList(fieldDefinitions);
        fieldsPerNumber = new SparseArray<>(fieldDefinitions.length);
        for (FitMessageFieldDefinition fieldDefinition : fieldDefinitions) {
            addField(fieldDefinition);
        }
    }

    public FitMessageFieldDefinition getField(int fieldNumber) {
        return fieldsPerNumber.get(fieldNumber);
    }

    public void writeToMessage(MessageWriter writer) {
        writer.writeByte(localMessageID | 0x40);
        writer.writeByte(0);
        writer.writeByte(0);
        writer.writeShort(globalMessageID);
        writer.writeByte(fieldDefinitions.size());
        for (FitMessageFieldDefinition fieldDefinition : fieldDefinitions) {
            fieldDefinition.writeToMessage(writer);
        }
    }

    public void addField(FitMessageFieldDefinition fieldDefinition) {
        if (fieldsPerNumber.get(fieldDefinition.fieldNumber) != null) {
            throw new IllegalArgumentException("Duplicate field number " + fieldDefinition.fieldNumber + " in message " + globalMessageID);
        }
        fieldsPerNumber.append(fieldDefinition.fieldNumber, fieldDefinition);
    }

    public FitMessageFieldDefinition findField(String fieldName) {
        for (final FitMessageFieldDefinition fieldDefinition : fieldDefinitions) {
            if (fieldName.equals(fieldDefinition.fieldName)) return fieldDefinition;
        }
        return null;
    }
}
