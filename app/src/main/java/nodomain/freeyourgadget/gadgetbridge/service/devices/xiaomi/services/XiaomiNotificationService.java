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

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class XiaomiNotificationService extends AbstractXiaomiService implements XiaomiDataUploadService.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiNotificationService.class);

    private static final SimpleDateFormat TIMESTAMP_SDF = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);

    public static final int COMMAND_TYPE = 7;

    public static final int CMD_NOTIFICATION_SEND = 0;
    public static final int CMD_NOTIFICATION_DISMISS = 1;
    public static final int CMD_CALL_REJECT = 2;
    public static final int CMD_CALL_IGNORE = 5;
    public static final int CMD_CANNED_MESSAGES_GET = 9;
    public static final int CMD_CANNED_MESSAGES_SET = 12; // also canned message reply
    public static final int CMD_NOTIFICATION_ICON_REQUEST = 15;
    public static final int CMD_NOTIFICATION_ICON_QUERY = 16;

    private String iconPackageName;

    public XiaomiNotificationService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void initialize() {
        requestCannedMessages();
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        final GBDeviceEventCallControl deviceEvtCallControl = new GBDeviceEventCallControl();

        switch (cmd.getSubtype()) {
            case CMD_CALL_REJECT:
                LOG.debug("Reject call");
                deviceEvtCallControl.event = GBDeviceEventCallControl.Event.REJECT;
                getSupport().evaluateGBDeviceEvent(deviceEvtCallControl);
                return;
            case CMD_CALL_IGNORE:
                LOG.debug("Ignore call");
                deviceEvtCallControl.event = GBDeviceEventCallControl.Event.IGNORE;
                getSupport().evaluateGBDeviceEvent(deviceEvtCallControl);
                return;
            case CMD_CANNED_MESSAGES_GET:
                handleCannedMessages(cmd.getNotification().getCannedMessages());
                return;
            case CMD_NOTIFICATION_ICON_REQUEST:
                handleNotificationIconRequest(cmd.getNotification().getNotificationIconRequest());
                return;
            case CMD_NOTIFICATION_ICON_QUERY:
                this.iconPackageName = cmd.getNotification().getNotificationIconQuery().getPackage();
                LOG.debug("Watch querying notification icon for {}", iconPackageName);

                // TODO should we confirm that we have the icon before replying?
                getSupport().sendCommand(
                        "send notification reply for " + iconPackageName,
                        XiaomiProto.Command.newBuilder()
                                .setType(COMMAND_TYPE)
                                .setSubtype(CMD_NOTIFICATION_ICON_REQUEST)
                                .setNotification(XiaomiProto.Notification.newBuilder()
                                        .setNotificationIconReply(cmd.getNotification().getNotificationIconQuery())
                                ).build()
                );
                return;
        }

        LOG.warn("Unhandled notification command {}", cmd.getSubtype());
    }



    public void onNotification(final NotificationSpec notificationSpec) {
        final XiaomiProto.Notification3.Builder notification3 = XiaomiProto.Notification3.newBuilder()
                .setId(notificationSpec.getId())
                .setUnknown4("") // ?
                .setTimestamp(TIMESTAMP_SDF.format(new Date(notificationSpec.when)));

        if (notificationSpec.sourceAppId != null) {
            notification3.setPackage(notificationSpec.sourceAppId);
        } else {
            notification3.setPackage(BuildConfig.APPLICATION_ID);
        }

        final String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        if (!senderOrTitle.isEmpty()) {
            notification3.setTitle(senderOrTitle);
        }

        if (notificationSpec.body != null) {
            notification3.setBody(notificationSpec.body);
        }

        if (notificationSpec.sourceName != null) {
            notification3.setAppName(notificationSpec.sourceName);
        }

        // TODO Open on phone
        //final String unknown12 = String.format(
        //        Locale.ROOT,
        //        "0|%s|%d|null|12345",
        //        notification3.getPackage(),
        //        notification3.getId() // i think this needs to be converted to unsigned
        //);
        //notification3.setUnknown12(unknown12);
        //notification3.setOpenOnPhone(1);

        final XiaomiProto.Notification2 notification2 = XiaomiProto.Notification2.newBuilder()
                .setNotification3(notification3)
                .build();

        final XiaomiProto.Notification notification = XiaomiProto.Notification.newBuilder()
                .setNotification2(notification2)
                .build();

        getSupport().sendCommand(
                "send notification " + notificationSpec.getId(),
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_NOTIFICATION_SEND)
                        .setNotification(notification)
                        .build()
        );
    }

    public void onDeleteNotification(final int id) {
        // TODO
    }

    public void onSetCallState(final CallSpec callSpec) {
        // TODO handle callSpec.command
        if (callSpec.command == CallSpec.CALL_OUTGOING) {
            return;
        }

        if (callSpec.command != CallSpec.CALL_INCOMING) {
            final XiaomiProto.NotificationDismiss.Builder notification4 = XiaomiProto.NotificationDismiss.newBuilder()
                    .setNotificationId(XiaomiProto.NotificationId.newBuilder().setId(0).setPackage("phone"));

            final XiaomiProto.Notification notification = XiaomiProto.Notification.newBuilder()
                    .setNotification4(notification4)
                    .build();

            getSupport().sendCommand(
                    "send call end",
                    XiaomiProto.Command.newBuilder()
                            .setType(COMMAND_TYPE)
                            .setSubtype(CMD_NOTIFICATION_DISMISS)
                            .setNotification(notification)
                            .build()
            );
            return;
        }

        final XiaomiProto.Notification3.Builder notification3 = XiaomiProto.Notification3.newBuilder()
                .setId(0) // ?
                .setUnknown4("") // ?
                .setIsCall(true)
                .setRepliesAllowed(canSendSms())
                .setTimestamp(TIMESTAMP_SDF.format(new Date()));

        notification3.setPackage("phone");
        notification3.setAppName("phone");

        if (callSpec.name != null) {
            notification3.setTitle(callSpec.name);
        } else {
            notification3.setTitle("?");
        }
        if (callSpec.number != null) {
            notification3.setBody(callSpec.number);
        } else {
            notification3.setBody("?");
        }

        // TODO unknown caller i18n

        final XiaomiProto.Notification2 notification2 = XiaomiProto.Notification2.newBuilder()
                .setNotification3(notification3)
                .build();

        final XiaomiProto.Notification notification = XiaomiProto.Notification.newBuilder()
                .setNotification2(notification2)
                .build();

        getSupport().sendCommand(
                "send call",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_NOTIFICATION_SEND)
                        .setNotification(notification)
                        .build()
        );
    }

    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        if (cannedMessagesSpec.type != CannedMessagesSpec.TYPE_GENERIC) {
            LOG.warn("Got unsupported canned messages type: {}", cannedMessagesSpec.type);
            return;
        }

        final XiaomiProto.CannedMessages.Builder cannedMessagesBuilder = XiaomiProto.CannedMessages.newBuilder()
                // TODO get those from wathc
                // TODO enforce these
                .setMinReplies(1)
                .setMaxReplies(10);
        for (final String cannedMessage : cannedMessagesSpec.cannedMessages) {
            cannedMessagesBuilder.addReply(cannedMessage);
        }

        final XiaomiProto.Notification.Builder notificationBuilder = XiaomiProto.Notification.newBuilder()
                .setCannedMessages(cannedMessagesBuilder);

        getSupport().sendCommand(
                "set canned messages",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CANNED_MESSAGES_SET)
                        .setNotification(notificationBuilder)
                        .build()
        );
    }

    public void requestCannedMessages() {
        getSupport().sendCommand(
                "get canned messages",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CANNED_MESSAGES_GET)
                        .setNotification(XiaomiProto.Notification.newBuilder().setUnknown8(1))
                        .build()
        );
    }

    public void handleCannedMessages(final XiaomiProto.CannedMessages cannedMessages) {
        // TODO save them
        //final GBDeviceEventUpdatePreferences gbDeviceEventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        //gbDeviceEventUpdatePreferences.withPreference("canned_reply_" + i, message);
        //getSupport().evaluateGBDeviceEvent(gbDeviceEventUpdatePreferences);
    }

    public boolean canSendSms() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return getSupport().getContext().checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void handleNotificationIconRequest(final XiaomiProto.NotificationIconRequest notificationIconRequest) {
        if (iconPackageName == null) {
            LOG.warn("No icon package name");
            return;
        }

        int unk1 = notificationIconRequest.getUnknown1();
        int unk2 = notificationIconRequest.getUnknown2();

        final int size = notificationIconRequest.getSize();
        LOG.debug("Got notification icon request for size {} for {}, unk1={}, unk2={}", size, iconPackageName, unk1, unk2);

        final Drawable icon = NotificationUtils.getAppIcon(getSupport().getContext(), iconPackageName);
        if (icon == null) {
            LOG.warn("Failed to get icon for {}", iconPackageName);
            return;
        }

        // TODO avoid resize?
        final Bitmap bmp = BitmapUtil.toBitmap(icon);
        final Bitmap bmpResized = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmpResized);
        final Rect rect = new Rect(0, 0, size, size);
        canvas.drawBitmap(bmp, null, rect, null);

        // convert from RGBA To ABGR
        final ByteBuffer buf = ByteBuffer.allocate(size * size * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                buf.putInt(bmpResized.getPixel(x, y));
            }
        }

        getSupport().getDataUploader().setCallback(this);
        getSupport().getDataUploader().requestUpload(XiaomiDataUploadService.TYPE_NOTIFICATION_ICON, buf.array());
    }

    @Override
    public void onUploadFinish(final boolean success) {
        LOG.debug("Notification icon upload finished: {}", success);

        getSupport().getDataUploader().setCallback(null);
    }

    @Override
    public void onUploadProgress(final int progressPercent) {
        LOG.debug("Notification icon upload progress: {}", progressPercent);
    }
}
