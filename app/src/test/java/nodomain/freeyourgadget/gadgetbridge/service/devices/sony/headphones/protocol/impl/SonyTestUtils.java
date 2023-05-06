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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyTestUtils {
    public static void assertRequest(final Request request, final String messageHex) {
        final Message message = Message.fromBytes(GB.hexStringToByteArray(messageHex.replace(":", "")));
        assertNotNull(message);
        assertRequest(request, message.getType(), message.getPayload());
    }

    public static void assertRequest(final Request request, final int messageType, final String payloadHex) {
        if (payloadHex == null) {
            assertNull(request);
            return;
        }

        assertRequest(
                request,
                MessageType.fromCode((byte) messageType),
                GB.hexStringToByteArray(payloadHex.replace(":", ""))
        );
    }

    public static void assertRequest(final Request request, final MessageType messageType, final byte[] payload) {
        assertEquals("Message types should be the same", messageType, request.messageType());
        assertEquals("Payloads should be the same", hexdump(payload), hexdump(request.payload()));
    }

    public static <A> void assertRequests(Function<A, Request> requestFunction, final int messageType, final Map<A, String> expectedRequests) {
        for (Map.Entry<A, String> entry : expectedRequests.entrySet()) {
            final Request request = requestFunction.apply(entry.getKey());
            assertRequest(request, messageType, entry.getValue());
        }
    }

    public static <A> void assertRequests(Function<A, Request> requestFunction, final Map<A, String> expectedRequests) {
        assertRequests(requestFunction, 0x0c, expectedRequests);
    }

    public static void assertPrefs(final List<? extends GBDeviceEvent> events, final Map<String, Object> expectedPrefs) {
        assertEquals("Expect 1 events", 1, events.size());
        final GBDeviceEventUpdatePreferences event = (GBDeviceEventUpdatePreferences) events.get(0);
        assertEquals("Expect number of prefs", expectedPrefs.size(), event.preferences.size());

        for (Map.Entry<String, Object> pref : expectedPrefs.entrySet()) {
            assertEquals("Expect " + pref.getKey() + " value", pref.getValue(), event.preferences.get(pref.getKey()));
        }
    }
    
    public static <A> void printRequests(Function<A, Request> requestFunction, final Map<A, String> expectedRequests) {
        for (Map.Entry<A, String> entry : expectedRequests.entrySet()) {
            final Request request = requestFunction.apply(entry.getKey());
            printRequest(request);
        }
    }

    public static List<? extends GBDeviceEvent> handleMessage(final AbstractSonyProtocolImpl protocol, final String messageHex) {
        final Message message = Message.fromBytes(GB.hexStringToByteArray(messageHex.replace(":", "")));
        assertNotNull("Failed to deserialize message from bytes", message);
        return protocol.handlePayload(message.getType(), message.getPayload());
    }

    public static void printRequest(final Request request) {
        System.out.printf(Locale.ROOT, "0x%02x - %s%n", request.messageType().getCode(), hexdump(request.payload()));
    }

    public static String hexdump(byte[] buffer, int offset, int length) {
        if (length == -1) {
            length = buffer.length - offset;
        }

        char[] hexChars = new char[length * 2 + length - 1];
        for (int i = 0; i < length; i++) {
            int v = buffer[i + offset] & 0xFF;
            hexChars[i * 3] = GB.HEX_CHARS[v >>> 4];
            hexChars[i * 3 + 1] = GB.HEX_CHARS[v & 0x0F];
            if (i + 1 < length) {
                hexChars[i * 3 + 2] = ':';
            }
        }
        return new String(hexChars).toLowerCase(Locale.ROOT);
    }

    public static String hexdump(byte[] buffer) {
        return hexdump(buffer, 0, buffer.length);
    }
}
