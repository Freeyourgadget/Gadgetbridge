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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiWatchfaceService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWatchfaceService.class);

    public static final int COMMAND_TYPE = 4;

    public static final int CMD_WATCHFACE_LIST = 0;
    public static final int CMD_WATCHFACE_SET = 1;
    public static final int CMD_WATCHFACE_INSTALL = 4;


    public XiaomiWatchfaceService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        //getSupport().sendCommand(builder, COMMAND_TYPE, CMD_WATCHFACE_LIST);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        // TODO
        switch (cmd.getSubtype()) {
            case CMD_WATCHFACE_LIST:
                handleWatchfaceList(cmd.getWatchface().getWatchfaceList());
                // TODO handle
                return;
            case CMD_WATCHFACE_SET:
                // watchface set ack
                return;
            case CMD_WATCHFACE_INSTALL:
                return;
        }

        LOG.warn("Unknown watchface command {}", cmd.getSubtype());
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        // TODO set watchface
        return super.onSendConfiguration(config, prefs);
    }

    public void requestWatchfaceList() {
        getSupport().sendCommand("request watchface list", COMMAND_TYPE, CMD_WATCHFACE_LIST);
    }

    private void handleWatchfaceList(final XiaomiProto.WatchfaceList watchfaceList) {
        LOG.debug("Got {} watchfaces", watchfaceList.getWatchfaceCount());

        final List<GBDeviceApp> gbDeviceApps = new ArrayList<>();

        for (final XiaomiProto.WatchfaceInfo watchface : watchfaceList.getWatchfaceList()) {
            GBDeviceApp gbDeviceApp = new GBDeviceApp(
                    UUID.randomUUID(),
                    watchface.getName(),
                    "",
                    "",
                    GBDeviceApp.Type.WATCHFACE
            );
            gbDeviceApps.add(gbDeviceApp);
        }

        final GBDeviceEventAppInfo appInfoCmd = new GBDeviceEventAppInfo();
        appInfoCmd.apps = gbDeviceApps.toArray(new GBDeviceApp[0]);
        getSupport().evaluateGBDeviceEvent(appInfoCmd);
    }

    public void setWatchface(final UUID uuid) {
        // TODO
    }

    public void setWatchface(final String watchfaceId) {
        // TODO
    }

    public void deleteWatchface(final UUID uuid) {
        // TODO
    }

    public void deleteWatchface(final String watchfaceId) {
        // TODO
        // TODO prevent uninstall of non-uninstallable watchface
    }

    public void installWatchface(final Uri uri) {
        // TODO
    }
}
