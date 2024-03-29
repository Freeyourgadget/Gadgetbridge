/*  Copyright (C) 2023-2024 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class FitDefinitionResponseMessage {
    public final int requestID;
    public final int status;
    public final int fitResponse;

    public FitDefinitionResponseMessage(int requestID, int status, int fitResponse) {
        this.requestID = requestID;
        this.status = status;
        this.fitResponse = fitResponse;
    }

    public static FitDefinitionResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();
        final int fitResponse = reader.readByte();

        return new FitDefinitionResponseMessage(requestID, status, fitResponse);
    }

    public static final int RESPONSE_APPLIED = 0;
    public static final int RESPONSE_NOT_UNIQUE = 1;
    public static final int RESPONSE_OUT_OF_RANGE = 2;
    public static final int RESPONSE_NOT_READY = 3;
}
