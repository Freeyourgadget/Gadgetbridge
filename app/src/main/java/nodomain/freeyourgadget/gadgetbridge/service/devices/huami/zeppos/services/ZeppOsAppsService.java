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
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ZeppOsAppsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAppsService.class);

    private static final short ENDPOINT = 0x00a0;

    private static final byte CMD_BYTE = 0x02;

    private static final byte CMD_INCOMING = 0x00;
    private static final byte CMD_OUTGOING = 0x01;

    private static final byte CMD_APP_LIST = 0x01;
    private static final byte CMD_APP_DELETE = 0x03;
    private static final byte CMD_APP_DELETING = 0x04;

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
        if (payload[0] != CMD_BYTE) {
            LOG.warn("Unexpected apps byte {}", String.format("0x%02x", payload[0]));
            return;
        }

        if (payload[1] != CMD_INCOMING) {
            LOG.warn("Unexpected apps 2nd byte {}", String.format("0x%02x", payload[1]));
            return;
        }

        switch (payload[2]) {
            case CMD_APP_LIST:
                parseAppList(payload);
                return;
            case CMD_APP_DELETE:
                LOG.info("Got app delete");
                return;
            case CMD_APP_DELETING:
                LOG.info("Got app deleting");
                return;
            default:
                LOG.warn("Unexpected apps payload {}", GB.hexdump(payload));
        }
    }

    private void parseAppList(final byte[] payload) {
        final List<GBDeviceApp> apps = new ArrayList<>();

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
                    "", //String.format("0x%08x", appId),
                    "",
                    appVersion,
                    GBDeviceApp.Type.APP_GENERIC // it might actually be a watchface
            ));
        }

        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        appInfoCmd.apps = apps.toArray(new GBDeviceApp[0]);

        getSupport().evaluateGBDeviceEvent(appInfoCmd);
    }

    public void requestApps() {
        LOG.info("Request apps");

        final ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_BYTE);
        buf.put(CMD_OUTGOING);
        buf.put(CMD_APP_LIST);
        buf.put((byte) 0x00);

        write("request apps", buf.array());
    }

    public void deleteApp(final int appId) {
        LOG.info("Delete app {}", String.format("0x%08x", appId));

        final ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);

        buf.put(CMD_BYTE);
        buf.put(CMD_OUTGOING);
        buf.put(CMD_APP_DELETE);
        buf.put((byte) 0x00);
        buf.putInt(0x00);
        buf.putInt(0x00);
        buf.putInt(0x00);
        buf.putInt(appId);

        write("delete app", buf.array());
    }
}
