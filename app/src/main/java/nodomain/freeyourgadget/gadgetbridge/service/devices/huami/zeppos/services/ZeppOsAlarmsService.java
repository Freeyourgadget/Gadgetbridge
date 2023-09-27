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

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class ZeppOsAlarmsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAlarmsService.class);

    public static final short ENDPOINT = 0x000f;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_CREATE = 0x03;
    public static final byte CMD_CREATE_ACK = 0x04;
    public static final byte CMD_DELETE = 0x05;
    public static final byte CMD_DELETE_ACK = 0x06;
    public static final byte CMD_UPDATE = 0x07;
    public static final byte CMD_UPDATE_ACK = 0x08;
    public static final byte CMD_REQUEST = 0x09;
    public static final byte CMD_RESPONSE = 0x0a;
    public static final byte CMD_NOTIFY_CHANGE = 0x0f;

    public static final int IDX_FLAGS = 0;
    public static final int IDX_POSITION = 1;
    public static final int IDX_HOUR = 2;
    public static final int IDX_MINUTE = 3;
    public static final int IDX_REPETITION = 4;

    public static final int FLAG_SMART = 0x01;
    public static final int FLAG_UNKNOWN_2 = 0x02;
    public static final int FLAG_ENABLED = 0x04;

    public ZeppOsAlarmsService(final Huami2021Support support) {
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
            case CMD_CREATE_ACK:
                LOG.info("Alarm create ACK, status = {}", payload[1]);
                return;
            case CMD_DELETE_ACK:
                LOG.info("Alarm delete ACK, status = {}", payload[1]);
                return;
            case CMD_UPDATE_ACK:
                LOG.info("Alarm update ACK, status = {}", payload[1]);
                return;
            case CMD_NOTIFY_CHANGE:
                LOG.info("Alarms changed on band");
                requestAlarms();
                return;
            case CMD_RESPONSE:
                LOG.info("Got alarms from band");
                decodeAndUpdateAlarms(payload);
                return;
            default:
                LOG.warn("Unexpected alarms payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    private void requestAlarms() {
        try {
            final TransactionBuilder builder = new TransactionBuilder("request alarms");
            requestAlarms(builder);
            builder.queue(getSupport().getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to request alarms", e);
        }
    }

    public void requestAlarms(final TransactionBuilder builder) {
        LOG.info("Requesting alarms");

        write(builder, new byte[]{CMD_REQUEST});
    }

    public void sendAlarm(final Alarm alarm, final TransactionBuilder builder) {
        final DeviceCoordinator coordinator = getSupport().getDevice().getDeviceCoordinator();

        final Calendar calendar = AlarmUtils.toCalendar(alarm);

        final byte[] alarmMessage;
        if (!alarm.getUnused()) {
            int alarmFlags = 0;
            if (alarm.getEnabled()) {
                alarmFlags = FLAG_ENABLED;
            }
            if (coordinator.supportsSmartWakeup(getSupport().getDevice()) && alarm.getSmartWakeup()) {
                alarmFlags |= FLAG_SMART;
            }
            alarmMessage = new byte[]{
                    CMD_CREATE,
                    (byte) 0x01, // ?
                    (byte) alarmFlags,
                    (byte) alarm.getPosition(),
                    (byte) calendar.get(Calendar.HOUR_OF_DAY),
                    (byte) calendar.get(Calendar.MINUTE),
                    (byte) alarm.getRepetition(),
                    (byte) 0x00, // ?
                    (byte) 0x00, // ?
                    (byte) 0x00, // ?
                    (byte) 0x00, // ?, this is usually 0 in the create command, 1 in the watch response
                    (byte) 0x00, // ?
            };
        } else {
            // Delete it from the band
            alarmMessage = new byte[]{
                    CMD_DELETE,
                    (byte) 0x01, // ?
                    (byte) alarm.getPosition()
            };
        }

        write(builder, alarmMessage);
    }

    private void decodeAndUpdateAlarms(final byte[] payload) {
        final int numAlarms = payload[1];

        if (payload.length != 2 + numAlarms * 10) {
            LOG.warn("Unexpected payload length of {} for {} alarms", payload.length, numAlarms);
            return;
        }

        // Map of alarm position to Alarm, as returned by the band
        final Map<Integer, Alarm> payloadAlarms = new HashMap<>();
        for (int i = 0; i < numAlarms; i++) {
            final Alarm alarm = parseAlarm(payload, 2 + i * 10);
            payloadAlarms.put(alarm.getPosition(), alarm);
        }

        final List<nodomain.freeyourgadget.gadgetbridge.entities.Alarm> dbAlarms = DBHelper.getAlarms(getSupport().getDevice());
        int numUpdatedAlarms = 0;

        for (nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm : dbAlarms) {
            final int pos = alarm.getPosition();
            final Alarm updatedAlarm = payloadAlarms.get(pos);
            final boolean alarmNeedsUpdate = updatedAlarm == null ||
                    alarm.getUnused() != updatedAlarm.getUnused() ||
                    alarm.getEnabled() != updatedAlarm.getEnabled() ||
                    alarm.getSmartWakeup() != updatedAlarm.getSmartWakeup() ||
                    alarm.getHour() != updatedAlarm.getHour() ||
                    alarm.getMinute() != updatedAlarm.getMinute() ||
                    alarm.getRepetition() != updatedAlarm.getRepetition();

            if (alarmNeedsUpdate) {
                numUpdatedAlarms++;
                LOG.info("Updating alarm index={}, unused={}", pos, updatedAlarm == null);
                alarm.setUnused(updatedAlarm == null);
                if (updatedAlarm != null) {
                    alarm.setEnabled(updatedAlarm.getEnabled());
                    alarm.setSmartWakeup(updatedAlarm.getSmartWakeup());
                    alarm.setHour(updatedAlarm.getHour());
                    alarm.setMinute(updatedAlarm.getMinute());
                    alarm.setRepetition(updatedAlarm.getRepetition());
                }
                DBHelper.store(alarm);
            }
        }

        if (numUpdatedAlarms > 0) {
            final Intent intent = new Intent(DeviceService.ACTION_SAVE_ALARMS);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    private Alarm parseAlarm(final byte[] payload, final int offset) {
        final nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm = new nodomain.freeyourgadget.gadgetbridge.entities.Alarm();

        alarm.setUnused(false); // If the band sent it, it's not unused
        alarm.setPosition(payload[offset + IDX_POSITION]);
        alarm.setEnabled((payload[offset + IDX_FLAGS] & FLAG_ENABLED) > 0);
        alarm.setSmartWakeup((payload[offset + IDX_FLAGS] & FLAG_SMART) > 0);
        alarm.setHour(payload[offset + IDX_HOUR]);
        alarm.setMinute(payload[offset + IDX_MINUTE]);
        alarm.setRepetition(payload[offset + IDX_REPETITION]);

        return alarm;
    }
}
