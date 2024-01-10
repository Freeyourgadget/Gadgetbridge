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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ChallengeResponse extends WithingsStructure {

    private byte[] response = new byte[0];

    public byte[] getResponse() {
        return response;
    }

    public void setResponse(byte[] response) {
        this.response = response;
    }

    @Override
    public short getLength() {
        return (short) ((response != null ? response.length : 0) + 5);
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
        addByteArrayWithLengthByte(buffer, response);
    }

    @Override
    public void fillFromRawDataAsBuffer(ByteBuffer rawDataBuffer) {
        response = getNextByteArray(rawDataBuffer);
    }

    @Override
    public short getType() {
        return WithingsStructureType.CHALLENGE_RESPONSE;
    }

}
