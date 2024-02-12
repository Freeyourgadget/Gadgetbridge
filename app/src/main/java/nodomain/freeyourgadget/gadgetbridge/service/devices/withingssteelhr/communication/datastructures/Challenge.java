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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Challenge extends WithingsStructure {

    private String macAddress;

    private byte[] challenge;

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public byte[] getChallenge() {
        return challenge;
    }

    @Override
    public short getLength() {
        int challengeLength = 0;
        int macAddressLength = (macAddress != null ? macAddress.getBytes().length : 0) + 1;
        if (challenge != null) {
            challengeLength = challenge.length;
        }

        return (short) (macAddressLength + challengeLength + 5);
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
        addStringAsBytesWithLengthByte(buffer, macAddress);

        if (challenge != null) {
            buffer.put((byte) challenge.length);
            buffer.put(challenge);
        } else {
            buffer.put((byte)0);
        }
    }

    @Override
    public void fillFromRawDataAsBuffer(ByteBuffer rawDataBuffer) {
        macAddress = getNextString(rawDataBuffer);
        challenge = getNextByteArray(rawDataBuffer);
    }

    @Override
    public short getType() {
        return WithingsStructureType.CHALLENGE;
    }


}
