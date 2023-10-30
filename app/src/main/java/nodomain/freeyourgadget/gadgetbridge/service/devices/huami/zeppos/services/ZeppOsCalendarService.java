/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsCalendarService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsCalendarService.class);

    public static final short ENDPOINT = 0x0007;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_EVENTS_REQUEST = 0x05;
    public static final byte CMD_EVENTS_RESPONSE = 0x06;
    public static final byte CMD_CREATE_EVENT = 0x07;
    public static final byte CMD_CREATE_EVENT_ACK = 0x08;
    public static final byte CMD_DELETE_EVENT = 0x09;
    public static final byte CMD_DELETE_EVENT_ACK = 0x0a;

    public static final String PREF_VERSION = "zepp_os_calendar_version";

    private int version = -1;

    public ZeppOsCalendarService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                version = payload[1];
                getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_VERSION, version));
                if (version != 1 && version != 3) {
                    LOG.warn("Unsupported calendar service version {}", version);
                    return;
                }
                LOG.info("Calendar service version={}", version);
                break;
            case CMD_EVENTS_RESPONSE:
                LOG.info("Got calendar events from band");
                decodeAndUpdateCalendarEvents(payload);
                return;
            case CMD_CREATE_EVENT_ACK:
                LOG.info("Calendar create event ACK, status = {}", payload[1]);
                return;
            case CMD_DELETE_EVENT_ACK:
                LOG.info("Calendar delete event ACK, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected calendar payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public void requestCalendarEvents() {
        LOG.info("Requesting calendar events from band");

        write("request calendar events", new byte[]{CMD_EVENTS_REQUEST, 0x00, 0x00});
    }

    public void addEvent(final CalendarEventSpec calendarEventSpec) {
        if (calendarEventSpec.type != CalendarEventSpec.TYPE_UNKNOWN) {
            LOG.warn("Unsupported calendar event type {}", calendarEventSpec.type);
            return;
        }

        LOG.info("Sending calendar event {} to band", calendarEventSpec.id);

        int length = 34;
        if (calendarEventSpec.title != null) {
            length += calendarEventSpec.title.getBytes(StandardCharsets.UTF_8).length;
        }
        if (calendarEventSpec.description != null) {
            length += calendarEventSpec.description.getBytes(StandardCharsets.UTF_8).length;
        }

        if (version == 3) {
            if (calendarEventSpec.location != null) {
                length += calendarEventSpec.location.getBytes(StandardCharsets.UTF_8).length;
            }
            // Extra null byte at the end
            length++;
        }

        final ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_CREATE_EVENT);
        buf.putInt((int) calendarEventSpec.id);

        if (calendarEventSpec.title != null) {
            buf.put(calendarEventSpec.title.getBytes(StandardCharsets.UTF_8));
        }
        buf.put((byte) 0x00);

        if (calendarEventSpec.description != null) {
            buf.put(calendarEventSpec.description.getBytes(StandardCharsets.UTF_8));
        }
        buf.put((byte) 0x00);

        buf.putInt(calendarEventSpec.timestamp);
        buf.putInt(calendarEventSpec.timestamp + calendarEventSpec.durationInSeconds);

        // Remind
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        // Repeat
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        // ?
        buf.put((byte) 0xff); // ?
        buf.put((byte) 0xff); // ?
        buf.put((byte) 0xff); // ?
        buf.put((byte) 0xff); // ?
        buf.put(bool(calendarEventSpec.allDay));
        buf.put((byte) 0x00); // ?
        buf.put((byte) 130); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        // TODO: Description here

        if (version == 3) {
            if (calendarEventSpec.location != null) {
                buf.put(calendarEventSpec.location.getBytes(StandardCharsets.UTF_8));
            }
            buf.put((byte) 0x00);
        }

        write("add calendar event", buf.array());
    }

    public void deleteEvent(final byte type, final long id) {
        if (type != CalendarEventSpec.TYPE_UNKNOWN) {
            LOG.warn("Unsupported calendar event type {}", type);
            return;
        }

        LOG.info("Deleting calendar event {} from band", id);

        final ByteBuffer buf = ByteBuffer.allocate(5);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_DELETE_EVENT);
        buf.putInt((int) id);

        write("delete calendar event", buf.array());
    }

    private void decodeAndUpdateCalendarEvents(final byte[] payload) {
        final int numEvents = payload[1];
        // FIXME there's a 0 after this, is it actually a 2-byte short?

        if (payload.length < 1 + numEvents * 34) {
            LOG.warn("Unexpected payload length of {} for {} calendar events", payload.length, numEvents);
            return;
        }

        int i = 3;
        while (i < payload.length) {
            if (payload.length - i < 34) {
                LOG.error("Not enough bytes remaining to parse a calendar event ({})", payload.length - i);
                return;
            }

            final int eventId = BLETypeConversions.toUint32(payload, i);
            i += 4;

            final String title = StringUtils.untilNullTerminator(payload, i);
            if (title == null) {
                LOG.error("Failed to decode title");
                return;
            }
            i += title.length() + 1;

            final String description = StringUtils.untilNullTerminator(payload, i);
            if (description == null) {
                LOG.error("Failed to decode description");
                return;
            }
            i += description.length() + 1;

            final int startTime = BLETypeConversions.toUint32(payload, i);
            i += 4;

            final int endTime = BLETypeConversions.toUint32(payload, i);
            i += 4;

            // ? 00 00 00 00 00 00 00 00 ff ff ff ff
            i += 12;

            boolean allDay = (payload[i] == 0x01);
            i++;

            // ? 00 82 00 00 00 00
            i += 6;

            LOG.info("Calendar Event {}: {}", eventId, title);
        }

        if (i != payload.length) {
            LOG.error("Unexpected calendar events payload trailer, {} bytes were not consumed", payload.length - i);
            return;
        }

        // TODO update database?
    }
}
