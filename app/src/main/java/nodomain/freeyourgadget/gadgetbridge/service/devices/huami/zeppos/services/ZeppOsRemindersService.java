/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
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

    private final Map<ZeppOsReminder, Integer> deviceReminders = new HashMap<>();

    public ZeppOsRemindersService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
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
                final TransactionBuilder builder = getSupport().createTransactionBuilder("send reminders");
                sendReminders(builder);
                builder.queue(getSupport().getQueue());
                return;
            default:
                LOG.warn("Unexpected reminders payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
        requestReminders(builder);
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

        final Date currentDate = new Date();
        final Set<ZeppOsReminder> allReminders = new HashSet<>();
        final LinkedList<ZeppOsReminder> toDelete = new LinkedList<>();
        final LinkedList<ZeppOsReminder> toSend = new LinkedList<>();

        for (final Reminder reminder : reminders) {
            if (currentDate.after(reminder.getDate())) {
                // Disregard reminders from the past, device ignores them anyway (does not even send
                // them back when requesting).
                continue;
            }

            final ZeppOsReminder newReminder = new ZeppOsReminder(reminder);
            allReminders.add(newReminder);

            if (deviceReminders.containsKey(newReminder)) {
                // Reminder exists and is up-to-date
                continue;
            }
            toSend.push(newReminder);
        }

        for (final ZeppOsReminder reminder : deviceReminders.keySet()) {
            if (!allReminders.contains(reminder)) {
                toDelete.add(reminder);
            }
        }

        for (final ZeppOsReminder reminder : toSend) {
            if (!toDelete.isEmpty()) {
                // If we have reminders to delete, replace them with the ones we want to send
                final ZeppOsReminder reminderToReplace = toDelete.pop();
                final Integer position = deviceReminders.get(reminderToReplace);
                if (position == null) {
                    LOG.error("Failed to find position for {} - this should never happen", reminderToReplace);
                    // We somehow got out of sync - request all reminders again
                    requestReminders(builder);
                    return;
                }
                LOG.debug("Updating reminder at position {}", position);
                sendReminderToDevice(builder, position, true, reminder);
                deviceReminders.remove(reminderToReplace);
                deviceReminders.put(reminder, position);
            } else {
                // Find the next available position
                for (int position = 0; position < reminderSlotCount; position++) {
                    if (!deviceReminders.containsValue(position)) {
                        LOG.debug("Creating reminder at position {}", position);
                        sendReminderToDevice(builder, position, false, reminder);
                        deviceReminders.put(reminder, position);
                        break;
                    }
                }
            }
        }

        for (final ZeppOsReminder reminder : toDelete) {
            final Integer position = deviceReminders.remove(reminder);
            if (position != null) {
                LOG.debug("Deleting reminder at position {}", position);
                sendReminderToDevice(builder, position, true, null);
            }
        }
    }

    private void sendReminderToDevice(final TransactionBuilder builder, int position, final boolean update, final ZeppOsReminder reminder) {
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

        final ByteBuffer buf = ByteBuffer.allocate(1 + 10 + reminder.getText().getBytes(StandardCharsets.UTF_8).length + 1);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Update does an upsert, so let's use it. If we call create twice on the same ID, it becomes weird
        buf.put(update ? CMD_UPDATE : CMD_CREATE);
        buf.put((byte) (position & 0xFF));

        buf.putInt(reminder.getFlags());
        buf.putInt(reminder.getTimestamp());
        buf.put((byte) 0x00);

        buf.put(reminder.getText().getBytes(StandardCharsets.UTF_8));
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

        deviceReminders.clear();

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

            final ZeppOsReminder zeppOsReminder = new ZeppOsReminder(
                    reminderFlags,
                    reminderTimestamp,
                    reminderText
            );

            deviceReminders.put(zeppOsReminder, reminderPosition);

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

    private class ZeppOsReminder {
        final int flags;
        final int timestamp;
        final String text;

        private ZeppOsReminder(final int flags, final int timestamp, final String text) {
            this.flags = flags;
            this.timestamp = timestamp;
            this.text = text;
        }

        private ZeppOsReminder(final Reminder reminder) {
            this.flags = getReminderFlags(reminder);
            this.timestamp = (int) (reminder.getDate().getTime() / 1000L);
            if (reminder.getMessage().length() > getCoordinator().getMaximumReminderMessageLength()) {
                LOG.warn("The reminder message length {} is longer than {}, will be truncated",
                        reminder.getMessage().length(),
                        getCoordinator().getMaximumReminderMessageLength()
                );
                text = StringUtils.truncate(reminder.getMessage(), getCoordinator().getMaximumReminderMessageLength());
            } else {
                text = reminder.getMessage();
            }
        }

        public int getFlags() {
            return flags;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public String getText() {
            return text;
        }

        private int getReminderFlags(final Reminder reminder) {
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
                    LOG.warn("Unknown repetition for reminder {}, defaulting to once", reminder.getReminderId());
            }

            return reminderFlags;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof ZeppOsReminder)) return false;
            final ZeppOsReminder that = (ZeppOsReminder) o;
            return flags == that.flags && timestamp == that.timestamp && Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(flags, timestamp, text);
        }
    }
}
