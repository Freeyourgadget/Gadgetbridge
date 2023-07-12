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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsRemindersService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsRemindersService.class);

    private static final short ENDPOINT = 0x0038;

    private static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    private static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    private static final byte CMD_REQUEST = 0x03;
    private static final byte CMD_RESPONSE = 0x04;
    private static final byte CMD_CREATE = 0x05;
    private static final byte CMD_CREATE_ACK = 0x06;
    private static final byte CMD_UPDATE = 0x07;
    private static final byte CMD_UPDATE_ACK = 0x08;
    private static final byte CMD_DELETE = 0x09;
    private static final byte CMD_DELETE_ACK = 0x0a;

    private static final int FLAG_ENABLED = 0x0001;
    private static final int FLAG_TEXT = 0x0008;
    private static final int FLAG_REPEAT_MONTH = 0x1000;
    private static final int FLAG_REPEAT_YEAR = 0x2000;

    private static final String PREF_CAPABILITY = "huami_2021_capability_reminders";

    public ZeppOsRemindersService(final Huami2021Support support) {
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
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                LOG.info("Reminder capability, version = {}", payload[1]);
                if (payload[1] != 1) {
                    LOG.warn("Reminder unsupported version {}", payload[1]);
                    return;
                }
                final int numReminders = payload[2] & 0xff;
                final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences(
                        PREF_CAPABILITY,
                        numReminders
                );
                evaluateGBDeviceEvent(eventUpdatePreferences);
                return;
            case CMD_CREATE_ACK:
                LOG.info("Reminder create ACK, status = {}", payload[1]);
                return;
            case CMD_DELETE_ACK:
                LOG.info("Reminder delete ACK, status = {}", payload[1]);
                // status 1 = success
                // status 2 = reminder not found
                return;
            case CMD_UPDATE_ACK:
                LOG.info("Reminder update ACK, status = {}", payload[1]);
                return;
            case CMD_RESPONSE:
                LOG.info("Got reminders from band");
                decodeAndUpdateReminders(payload);
                return;
            default:
                LOG.warn("Unexpected reminders payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
        //requestReminders(builder);
        sendReminders(builder);
    }

    private void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    private void requestReminders(final TransactionBuilder builder) {
        write(builder, CMD_REQUEST);
    }

    public void sendReminders(final TransactionBuilder builder) {
        final List<? extends Reminder> reminders = DBHelper.getReminders(getSupport().getDevice());
        sendReminders(builder, reminders);
    }

    public void sendReminders(final TransactionBuilder builder, final List<? extends Reminder> reminders) {
        LOG.info("On Set Reminders: {}", reminders.size());

        final int reminderSlotCount = getCoordinator().getReminderSlotCount(getSupport().getDevice());
        if (reminderSlotCount <= 0) {
            LOG.warn("Reminders not yet initialized");
            return;
        }

        // Send the reminders
        for (int i = 0; i < reminders.size(); i++) {
            LOG.debug("Sending reminder at position {}", i);

            sendReminderToDevice(builder, i, reminders.get(i));
        }

        // Delete the remaining slots, skipping the sent reminders
        for (int i = reminders.size(); i < reminderSlotCount; i++) {
            LOG.debug("Deleting reminder at position {}", i);

            sendReminderToDevice(builder, i, null);
        }
    }

    protected void sendReminderToDevice(final TransactionBuilder builder, int position, final Reminder reminder) {
        final DeviceCoordinator coordinator = getCoordinator();
        final int reminderSlotCount = coordinator.getReminderSlotCount(getSupport().getDevice());
        if (position + 1 > reminderSlotCount) {
            LOG.error("Reminder for position {} is over the limit of {} reminders", position, reminderSlotCount);
            return;
        }

        if (reminder == null) {
            // Delete reminder
            write(builder, new byte[]{CMD_DELETE, (byte) (position & 0xFF)});

            return;
        }

        final String message;
        if (reminder.getMessage().length() > coordinator.getMaximumReminderMessageLength()) {
            LOG.warn("The reminder message length {} is longer than {}, will be truncated",
                    reminder.getMessage().length(),
                    coordinator.getMaximumReminderMessageLength()
            );
            message = StringUtils.truncate(reminder.getMessage(), coordinator.getMaximumReminderMessageLength());
        } else {
            message = reminder.getMessage();
        }

        final ByteBuffer buf = ByteBuffer.allocate(1 + 10 + message.getBytes(StandardCharsets.UTF_8).length + 1);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Update does an upsert, so let's use it. If we call create twice on the same ID, it becomes weird
        buf.put(CMD_UPDATE);
        buf.put((byte) (position & 0xFF));

        final Calendar cal = BLETypeConversions.createCalendar();
        cal.setTime(reminder.getDate());

        int reminderFlags = FLAG_ENABLED | FLAG_TEXT;

        switch (reminder.getRepetition()) {
            case Reminder.ONCE:
                // Default is once, nothing to do
                break;
            case Reminder.EVERY_DAY:
                reminderFlags |= 0x0fe0; // all week day bits set
                break;
            case Reminder.EVERY_WEEK:
                int dayOfWeek = BLETypeConversions.dayOfWeekToRawBytes(cal) - 1; // Monday = 0
                reminderFlags |= 0x20 << dayOfWeek;
                break;
            case Reminder.EVERY_MONTH:
                reminderFlags |= FLAG_REPEAT_MONTH;
                break;
            case Reminder.EVERY_YEAR:
                reminderFlags |= FLAG_REPEAT_YEAR;
                break;
            default:
                LOG.warn("Unknown repetition for reminder in position {}, defaulting to once", position);
        }

        buf.putInt(reminderFlags);

        buf.putInt((int) (cal.getTimeInMillis() / 1000L));
        buf.put((byte) 0x00);

        buf.put(message.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);

        write(builder, buf.array());
    }

    private void decodeAndUpdateReminders(final byte[] payload) {
        final int numReminders = payload[1] & 0xff;

        if (payload.length < 3 + numReminders * 11) {
            LOG.warn("Unexpected payload length of {} for {} reminders", payload.length, numReminders);
            return;
        }

        LOG.debug("Got {} reminders from band", numReminders);

        int i = 3;
        while (i < payload.length) {
            if (payload.length - i < 11) {
                LOG.error("Not enough bytes remaining to parse a reminder ({})", payload.length - i);
                return;
            }

            final int reminderPosition = payload[i++] & 0xff;
            final int reminderFlags = BLETypeConversions.toUint32(payload, i);
            i += 4;
            final int reminderTimestamp = BLETypeConversions.toUint32(payload, i);
            i += 4;
            i++; // 0 ?
            final Date reminderDate = new Date(reminderTimestamp * 1000L);
            final String reminderText = StringUtils.untilNullTerminator(payload, i);
            if (reminderText == null) {
                LOG.error("Failed to parse reminder text at pos {}", i);
                return;
            }

            i += reminderText.length() + 1;

            LOG.info("Reminder[{}]: {}, {}, {}", reminderPosition, String.format("0x%04x", reminderFlags), reminderDate, reminderText);
        }
        if (i != payload.length) {
            LOG.error("Unexpected reminders payload trailer, {} bytes were not consumed", payload.length - i);
        }

        // TODO persist in database. Probably not trivial, because reminderPosition != reminderId
    }

    public static int getSlotCount(final Prefs devicePrefs) {
        return devicePrefs.getInt(PREF_CAPABILITY, 0);
    }
}
