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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class AuthNegotiationResponseMessage {
    public final int status;
    public final int response;
    public final int longTermKeyAvailability;
    public final int supportedEncryptionAlgorithms;

    public AuthNegotiationResponseMessage(int status, int response, int longTermKeyAvailability, int supportedEncryptionAlgorithms) {
        this.status = status;
        this.response = response;
        this.longTermKeyAvailability = longTermKeyAvailability;
        this.supportedEncryptionAlgorithms = supportedEncryptionAlgorithms;
    }

    public static AuthNegotiationResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();
        final int response = reader.readByte();
        final int longTermKeyAvailability = reader.readByte();
        final int supportedEncryptionAlgorithms = reader.readInt();

        return new AuthNegotiationResponseMessage(status, response, longTermKeyAvailability, supportedEncryptionAlgorithms);
    }
}
