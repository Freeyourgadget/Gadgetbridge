/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyTestUtils {
    public static void assertRequest(final Request request, final String messageHex) {
        final Message message = Message.fromBytes(GB.hexStringToByteArray(messageHex.replace(":", "")));
        assertRequest(request, message.getType(), message.getPayload());
    }

    public static void assertRequest(final Request request, final int messageType, final String payloadHex) {
        assertRequest(
                request,
                MessageType.fromCode((byte) messageType),
                GB.hexStringToByteArray(payloadHex.replace(":", ""))
        );
    }

    public static void assertRequest(final Request request, final MessageType messageType, final byte[] payload) {
        assertEquals("Message types should be the same", messageType, request.messageType());
        assertArrayEquals("Payloads should be the same", payload, request.payload());
    }

    public static List<? extends GBDeviceEvent> handleMessage(final AbstractSonyProtocolImpl protocol, final String messageHex) {
        final Message message = Message.fromBytes(GB.hexStringToByteArray(messageHex.replace(":", "")));

        return protocol.handlePayload(message.getType(), message.getPayload());
    }
}
