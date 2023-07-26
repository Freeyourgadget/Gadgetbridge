/*  Copyright (C) 2021 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsStructure;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class AbstractMessage implements Message {

    /**
     * The header consist of the first byte 0x01 (probably the message format identifier),
     * two bytes for the message type and 2 bytes for the actual datalength.
     */
    private static final int HEADER_SIZE = 5;
    protected final static short EOM_SIZE = 4;

    private List<WithingsStructure> dataStructures = new ArrayList<WithingsStructure>();

    public List<WithingsStructure> getDataStructures() {
        return Collections.unmodifiableList(dataStructures);
    }

    @Override
    public void addDataStructure(WithingsStructure data) {
        dataStructures.add(data);
    }

    @Override
    public byte[] getRawData() {
        short structureLength = 0;
        boolean setEndOfMessage = false;
        for (WithingsStructure structure : dataStructures) {
            if (structure.withEndOfMessage()) {
                setEndOfMessage = true;
            }
            structureLength += (short)(structure.getLength());
        }

        if (setEndOfMessage) {
            structureLength += EOM_SIZE;
        }

        ByteBuffer rawDataBuffer = ByteBuffer.allocate(HEADER_SIZE + structureLength);
        rawDataBuffer.put((byte)0x01); // <= This seems to be always 0x01 for all commands
        rawDataBuffer.putShort(getType());
        rawDataBuffer.putShort(structureLength);

        for (WithingsStructure structure : dataStructures) {
            rawDataBuffer.put(structure.getRawData());
        }

        if (setEndOfMessage) {
            addEndOfMessageBytes(rawDataBuffer);
        }

        return rawDataBuffer.array();
    }

    @Override
    public <T extends WithingsStructure> T getStructureByType(Class<T> type) {
        for (WithingsStructure structure : this.getDataStructures()) {
            if (type.isInstance(structure)) {
                return (T)structure;
            }
        }

        return null;
    }

    private void addEndOfMessageBytes(ByteBuffer buffer) {
        buffer.putShort((short)256);
        buffer.putShort((short)0);
    }

    public String toString() {
        return GB.hexdump(this.getRawData()).toLowerCase(Locale.ROOT);
    }
}
