/*  Copyright (C) 2021-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class Request {
    private final MessageType messageType;
    private final byte[] payload;

    public Request(final MessageType messageType, final byte[] payload) {
        this.messageType = messageType;
        this.payload = payload;
    }

    public Request(final MessageType messageType, final int... payload) {
        this.messageType = messageType;
        this.payload = new byte[payload.length];

        for (int i = 0; i < payload.length; i++) {
            this.payload[i] = (byte) payload[i];
        }
    }

    public MessageType messageType() {
        return this.messageType;
    }

    public byte[] payload() {
        return this.payload;
    }

    public final Message toMessage(final byte sequenceNumber) {
        return new Message(messageType(), sequenceNumber, payload());
    }

    public final byte[] encode(final byte sequenceNumber) {
        return toMessage(sequenceNumber).encode();
    }

    public static Request fromHex(final MessageType messageType, final String payload) {
        return new Request(messageType, GB.hexStringToByteArray(payload));
    }
}
