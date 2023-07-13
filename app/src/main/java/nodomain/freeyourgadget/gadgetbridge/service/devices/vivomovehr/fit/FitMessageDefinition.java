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
