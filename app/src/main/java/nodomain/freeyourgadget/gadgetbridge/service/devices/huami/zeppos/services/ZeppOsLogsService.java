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

import android.annotation.SuppressLint;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsLogsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsLogsService.class);

    private static final short ENDPOINT = 0x003a;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_LOGS_START = 0x03;
    public static final byte CMD_LOGS_START_ACK = 0x04;
    public static final byte CMD_LOGS_STOP = 0x05;
    public static final byte CMD_LOGS_STOP_ACK = 0x06;
    public static final byte CMD_LOGS_DATA = 0x07;
    public static final byte CMD_UNKNOWN_8 = 0x08;
    public static final byte CMD_UNKNOWN_9 = 0x09;

    public static final String PREF_VERSION = "zepp_os_logs_version";

    private String logsType;
    private final Set<Byte> sessions = new HashSet<>();

    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public ZeppOsLogsService(final Huami2021Support support) {
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
                handleCapabilitiesResponse(payload);
                break;
            case CMD_LOGS_START_ACK:
                if (payload[1] != 1) {
                    LOG.warn("Failed to start logs, status = {}", payload[1]);
                    GB.toast(getContext(), "Failed to start logs", Toast.LENGTH_SHORT, GB.WARN);
                    return;
                }
                final byte sessionId = payload[2];
                LOG.info("Got logs start ack, sessionId = {}", sessionId);
                GB.toast(getContext(), "App logs started", Toast.LENGTH_SHORT, GB.INFO);
                sessions.add(sessionId);
                break;
            case CMD_LOGS_STOP_ACK:
                LOG.info("Got logs stop ack, status = {}", payload[1]);
                GB.toast(getContext(), "App logs stopped", Toast.LENGTH_SHORT, GB.INFO);
                break;
            case CMD_LOGS_DATA:
                handleLogsData(payload);
                break;
            case CMD_UNKNOWN_8:
                LOG.info("Got unknown 8, replying with unknown 9");
                write("reply logs unknown 9", CMD_UNKNOWN_9);
                break;
            default:
                LOG.warn("Unexpected logs payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_APP_LOGS_START:
                start();
                return true;
            case DeviceSettingsPreferenceConst.PREF_APP_LOGS_STOP:
                stop();
                return true;
        }

        return false;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void start() {
        if (logsType == null) {
            LOG.error("logsType is null");
            return;
        }

        LOG.info("Starting logs");

        final byte[] logsTypeBytes = logsType.getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buf = ByteBuffer.allocate(1 + logsTypeBytes.length + 1)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_LOGS_START);
        buf.put(logsTypeBytes);
        buf.put((byte) 0);

        write("start logs", buf.array());
    }

    public void stop() {
        LOG.info("Stopping {} log sessions", sessions.size());

        for (final Byte session : sessions) {
            stop(session);
        }
    }

    private void stop(final byte sessionId) {
        final ByteBuffer buf = ByteBuffer.allocate(3)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_LOGS_STOP);
        buf.put((byte) 0);
        buf.put(sessionId);

        write("stop logs session " + sessionId, buf.array());
    }

    private void handleCapabilitiesResponse(final byte[] payload) {
        final int version = payload[1] & 0xFF;
        if (version != 1) {
            LOG.warn("Unsupported logs service version {}", version);
            return;
        }
        final byte var1 = payload[2];
        if (var1 != 1) {
            LOG.warn("Unexpected value for var1 '{}'", var1);
        }
        final byte var2 = payload[3];
        if (var2 != 0) {
            LOG.warn("Unexpected value for var2 '{}'", var2);
        }

        logsType = StringUtils.untilNullTerminator(payload, 4);

        LOG.info("Logs version={}, var1={}, var2={}, logsType={}", version, var1, var2, logsType);

        getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_VERSION, version));
    }

    private void handleLogsData(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        buf.get(); // discard first byte
        final byte index = buf.get();
        final byte sessionId = buf.get();
        if (!sessions.contains(sessionId)) {
            LOG.warn("Got log data for unknown session {}", sessionId);
        }
        final String appIdDecimal = StringUtils.untilNullTerminator(buf);
        final byte unknown1 = buf.get();
        if (unknown1 != 0) {
            LOG.warn("Unexpected value for unknown1 = {}", unknown1);
        }
        final long timestampMillis = buf.getLong();
        final byte unknown2 = buf.get();
        if (unknown2 != 2) {
            LOG.warn("Unexpected value for unknown2 = {}", unknown2);
        }
        final String message = StringUtils.untilNullTerminator(buf);
        if (buf.position() < buf.limit()) {
            LOG.warn("There are {} log data bytes still in the buffer", (buf.limit() - buf.position()));
        }

        LOG.info(
                "Log entry - {} [{}] - {}",
                sdf.format(new Date(timestampMillis)),
                appIdDecimal,
                message
        );
    }

    public static boolean isSupported(final Prefs devicePrefs) {
        return devicePrefs.getInt(PREF_VERSION, 0) == 1;
    }
}
