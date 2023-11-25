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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetProgressAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiWatchfaceService extends AbstractXiaomiService implements XiaomiDataUploadService.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiWatchfaceService.class);

    public static final int COMMAND_TYPE = 4;

    public static final int CMD_WATCHFACE_LIST = 0;
    public static final int CMD_WATCHFACE_SET = 1;
    public static final int CMD_WATCHFACE_DELETE = 2;
    public static final int CMD_WATCHFACE_INSTALL = 4;

    private final Set<UUID> allWatchfaces = new HashSet<>();
    private final Set<UUID> userWatchfaces = new HashSet<>();
    private UUID activeWatchface = null;

    // Not null if we're installing a firmware
    private XiaomiFWHelper fwHelper = null;

    public XiaomiWatchfaceService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_WATCHFACE_LIST:
                handleWatchfaceList(cmd.getWatchface().getWatchfaceList());
                return;
            case CMD_WATCHFACE_SET:
                LOG.debug("Got watchface set response, ack={}", cmd.getWatchface().getAck());
                //requestWatchfaceList();
                return;
            case CMD_WATCHFACE_DELETE:
                LOG.debug("Got watchface delete response, ack={}", cmd.getWatchface().getAck());
                requestWatchfaceList();
                return;
            case CMD_WATCHFACE_INSTALL:
                final int installStatus = cmd.getWatchface().getInstallStatus();
                if (installStatus != 0) {
                    LOG.warn("Invalid watchface install status {} for {}", installStatus, fwHelper.getId());
                    return;
                }

                LOG.debug("Watchface install status 0, uploading");
                setDeviceBusy();
                getSupport().getDataUploader().setCallback(this);
                getSupport().getDataUploader().requestUpload(XiaomiDataUploadService.TYPE_WATCHFACE, fwHelper.getBytes());
                return;
        }

        LOG.warn("Unknown watchface command {}", cmd.getSubtype());
    }

    public void requestWatchfaceList() {
        getSupport().sendCommand("request watchface list", COMMAND_TYPE, CMD_WATCHFACE_LIST);
    }

    private void handleWatchfaceList(final XiaomiProto.WatchfaceList watchfaceList) {
        LOG.debug("Got {} watchfaces", watchfaceList.getWatchfaceCount());

        allWatchfaces.clear();
        userWatchfaces.clear();
        activeWatchface = null;

        final List<GBDeviceApp> gbDeviceApps = new ArrayList<>();

        for (final XiaomiProto.WatchfaceInfo watchface : watchfaceList.getWatchfaceList()) {
            final UUID uuid = toWatchfaceUUID(watchface.getId());
            allWatchfaces.add(uuid);
            if (watchface.getCanDelete()) {
                userWatchfaces.add(uuid);
            }
            if (watchface.getActive()) {
                activeWatchface = uuid;
            }

            GBDeviceApp gbDeviceApp = new GBDeviceApp(
                    uuid,
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
        final String id = toWatchfaceId(uuid);

        // TODO for now we need to allow when installing a watchface
        //if (!allWatchfaces.contains(uuid)) {
        //    LOG.warn("Unknown watchface {}", uuid);
        //    return;
        //}

        activeWatchface = uuid;

        LOG.debug("Set watchface to {}", id);

        getSupport().sendCommand(
                "set watchface to " + uuid,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_WATCHFACE_SET)
                        .setWatchface(XiaomiProto.Watchface.newBuilder().setWatchfaceId(id))
                        .build()
        );
    }

    public void setWatchface(final String watchfaceId) {
        setWatchface(toWatchfaceUUID(watchfaceId));
    }

    public void deleteWatchface(final UUID uuid) {
        final String id = toWatchfaceId(uuid);

        if (!userWatchfaces.contains(uuid)) {
            LOG.warn("Refusing to delete non-user watchface {}", id);
            return;
        }

        if (!allWatchfaces.contains(uuid)) {
            LOG.warn("Refusing to delete unknown watchface {}", id);
            return;
        }

        if (uuid.equals(activeWatchface)) {
            LOG.warn("Refusing to delete active watchface {}", id);
            return;
        }

        LOG.debug("Delete watchface {}", id);

        allWatchfaces.remove(uuid);
        userWatchfaces.remove(uuid);

        getSupport().sendCommand(
                "delete watchface " + id,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_WATCHFACE_DELETE)
                        .setWatchface(XiaomiProto.Watchface.newBuilder().setWatchfaceId(id))
                        .build()
        );
    }

    public void installWatchface(final XiaomiFWHelper fwHelper) {
        assert fwHelper.isValid();
        assert fwHelper.isWatchface();

        this.fwHelper = fwHelper;

        getSupport().sendCommand(
                "install watchface " + fwHelper.getId(),
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_WATCHFACE_INSTALL)
                        .setWatchface(XiaomiProto.Watchface.newBuilder().setWatchfaceInstallStart(
                                XiaomiProto.WatchfaceInstallStart.newBuilder()
                                        .setId(fwHelper.getId())
                                        .setSize(fwHelper.getBytes().length)
                        ))
                        .build()
        );
    }

    public static UUID toWatchfaceUUID(final String id) {
        // Watchface IDs are numbers as strings - pad them to the right with F
        // and encode as UUID
        final String padded = String.format("%-32s", id).replace(' ', 'F');
        return UUID.fromString(
                padded.substring(0, 8) + "-" +
                        padded.substring(8, 12) + "-" +
                        padded.substring(12, 16) + "-" +
                        padded.substring(16, 20) + "-" +
                        padded.substring(20, 32)
        );
    }

    public static String toWatchfaceId(final UUID uuid) {
        return uuid.toString()
                .replaceAll("-", "")
                .replaceAll("f", "")
                .replaceAll("F", "");
    }

    @Override
    public void onUploadFinish(final boolean success) {
        LOG.debug("Watchface upload finished: {}", success);

        getSupport().getDataUploader().setCallback(null);

        final String notificationMessage = success ?
                getSupport().getContext().getString(R.string.updatefirmwareoperation_update_complete) :
                getSupport().getContext().getString(R.string.updatefirmwareoperation_write_failed);

        GB.updateInstallNotification(notificationMessage, false, 100, getSupport().getContext());

        unsetDeviceBusy();

        if (success) {
            setWatchface(fwHelper.getId());
            requestWatchfaceList();
        }

        fwHelper = null;
    }

    @Override
    public void onUploadProgress(final int progressPercent) {
        try {
            final TransactionBuilder builder = getSupport().createTransactionBuilder("send data upload progress");
            builder.add(new SetProgressAction(
                    getSupport().getContext().getString(R.string.updatefirmwareoperation_update_in_progress),
                    true,
                    progressPercent,
                    getSupport().getContext()
            ));
            builder.queue(getSupport().getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }

    private void setDeviceBusy() {
        final GBDevice device = getSupport().getDevice();
        device.setBusyTask(getSupport().getContext().getString(R.string.updating_firmware));
        device.sendDeviceUpdateIntent(getSupport().getContext());
    }

    private void unsetDeviceBusy() {
        final GBDevice device = getSupport().getDevice();
        if (device != null && device.isConnected()) {
            if (device.isBusy()) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(getSupport().getContext());
            }
            device.sendDeviceUpdateIntent(getSupport().getContext());
        }
    }
}
