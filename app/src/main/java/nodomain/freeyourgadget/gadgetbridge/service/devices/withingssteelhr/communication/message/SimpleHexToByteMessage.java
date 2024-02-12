/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message;

import java.nio.ByteBuffer;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsStructure;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SimpleHexToByteMessage implements Message {
    private String hexString;

    public SimpleHexToByteMessage(String hexString) {
        this.hexString = hexString;
    }

    @Override
    public List<WithingsStructure> getDataStructures() {
        return null;
    }

    @Override
    public void addDataStructure(WithingsStructure data) {

    }

    @Override
    public short getType() {
        return 0;
    }

    @Override
    public byte[] getRawData() {
        return GB.hexStringToByteArray(hexString);
    }

    @Override
    public boolean needsResponse() {
        return false;
    }

    @Override
    public boolean needsEOT() {
        return false;
    }

    @Override
    public boolean isIncomingMessage() {
        return false;
    }

    @Override
    public <T extends WithingsStructure> T getStructureByType(Class<T> type) {
        return null;
    }
}
