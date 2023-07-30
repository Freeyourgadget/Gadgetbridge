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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;

public class ZeppOsAppsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAppsService.class);

    private static final short ENDPOINT = 0x00a0;

    private static final byte CMD_JS = 0x01;
    private static final byte CMD_APPS = 0x02;
    private static final byte CMD_SCREENSHOT = 0x03;

    private static final byte CMD_INCOMING = 0x00;
    private static final byte CMD_OUTGOING = 0x01;

    private static final byte CMD_APPS_LIST = 0x01;
    private static final byte CMD_APPS_DELETE = 0x03;
    private static final byte CMD_APPS_DELETING = 0x04;
    private static final byte CMD_APPS_API_LEVEL = 0x05;
    private static final byte CMD_SCREENSHOT_REQUEST = 0x01;

    private final List<GBDeviceApp> apps = new ArrayList<>();

    public ZeppOsAppsService(final Huami2021Support support) {
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
            case CMD_JS:
                handleJsPayload(payload);
                return;
            case CMD_APPS:
                handleAppsPayload(payload);
                return;
            case CMD_SCREENSHOT:
                handleScreenshotPayload(payload);
                return;
            default:
                LOG.warn("Unexpected apps byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public void initialize(final TransactionBuilder builder) {
        requestApps(builder);
    }

    public List<GBDeviceApp> getApps() {
        return apps;
    }

    private void handleJsPayload(final byte[] payload) {
        LOG.warn("Handling js payloads not implemented");
    }

    private void handleAppsPayload(final byte[] payload) {
        if (payload[1] != CMD_INCOMING) {
            LOG.warn("Unexpected non-incoming payload ({})", String.format("0x%02x", payload[1]));
            return;
        }

        switch (payload[2]) {
            case CMD_APPS_LIST:
                parseAppList(payload);
                return;
            case CMD_APPS_DELETE:
                LOG.info("Got app delete");
                return;
            case CMD_APPS_DELETING:
                LOG.info("Got app deleting");
                return;
            case CMD_APPS_API_LEVEL:
                final int apiLevel = payload[17] & 0xff;
                LOG.info("Got API level: {}", apiLevel); // 200 = 2.0
                return;
            default:
                LOG.warn("Unexpected apps payload byte {}", payload[2]);
        }
    }

    private void handleScreenshotPayload(final byte[] payload) {
        if (payload[1] != CMD_INCOMING) {
            LOG.warn("Unexpected non-incoming payload ({})", String.format("0x%02x", payload[1]));
            return;
        }

        switch (payload[2]) {
            case CMD_SCREENSHOT_REQUEST:
                LOG.info("Got screenshot request ack, status={}", payload[16]);  // 0 for success
                return;
            default:
                LOG.warn("Unexpected screenshot payload byte {}", payload[2]);
        }
    }

    private void parseAppList(final byte[] payload) {
        apps.clear();

        final byte[] appListStringBytes = ArrayUtils.subarray(payload, 16, payload.length);
        final String appListString = new String(appListStringBytes);
        final String[] appListSplit = appListString.split(";");
        for (final String appString : appListSplit) {
            if (StringUtils.isBlank(appString)) {
                continue;
            }

            final String[] appSplit = appString.split("-", 2);
            if (appSplit.length != 2) {
                LOG.warn("Failed to parse {}", appString);
                continue;
            }

            final int appId = Integer.parseInt(appSplit[0], 16);
            final String appVersion = appSplit[1];

            LOG.debug("Got app: '{}'", appString);

            apps.add(new GBDeviceApp(
                    UUID.fromString(String.format("%08x-0000-0000-0000-000000000000", appId)),
                    "",
                    "",
                    appVersion,
                    GBDeviceApp.Type.UNKNOWN // it might be an app or watchface
            ));
        }

        // TODO broadcast something to update app manager
    }

    public void requestApps(final TransactionBuilder builder) {
        LOG.info("Request apps");

        final ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_APPS);
        buf.put(CMD_OUTGOING);
        buf.put(CMD_APPS_LIST);
        buf.put((byte) 0x00);

        write(builder, buf.array());
    }

    public void requestApilevel() {
        LOG.info("Request api level");

        final ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_APPS);
        buf.put(CMD_OUTGOING);
        buf.put(CMD_APPS_API_LEVEL);

        write("request api level", buf.array());
    }

    public void deleteApp(final UUID uuid) {
        deleteApp(Integer.parseInt(uuid.toString().split("-")[0], 16));
    }

    public void deleteApp(final int appId) {
        LOG.info("Delete app {}", String.format("0x%08x", appId));

        final ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_APPS);
        buf.put(CMD_OUTGOING);
        buf.put(CMD_APPS_DELETE);
        buf.put((byte) 0x00);
        buf.putInt(0x00);
        buf.putInt(0x00);
        buf.putInt(0x00);
        buf.putInt(appId);

        write("delete app", buf.array());
    }

    public void requestScreenshot() {
        LOG.info("Requesting screenshot");

        final ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_SCREENSHOT);
        buf.put(CMD_OUTGOING);
        buf.put(CMD_SCREENSHOT_REQUEST);
        buf.put((byte) 0x00);

        write("request screenshot", buf.array());
    }
}
