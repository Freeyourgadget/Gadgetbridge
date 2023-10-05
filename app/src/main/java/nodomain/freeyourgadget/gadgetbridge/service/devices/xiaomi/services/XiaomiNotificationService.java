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

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class XiaomiNotificationService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiNotificationService.class);

    private static final SimpleDateFormat TIMESTAMP_SDF = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);

    public static final int COMMAND_TYPE = 7;

    public static final int CMD_NOTIFICATION_SEND = 0;
    public static final int CMD_CANNED_MESSAGES_GET = 9;
    public static final int CMD_CANNED_MESSAGES_SET = 12;

    public XiaomiNotificationService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCannedMessages(builder);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_CANNED_MESSAGES_GET:
                handleCannedMessages(cmd.getNotification().getCannedMessages());
                break;
        }

        // TODO

        LOG.warn("Unhandled notification command {}", cmd.getSubtype());
    }

    public void onNotification(final NotificationSpec notificationSpec) {
        // TODO this is not working
        if (true) {
            LOG.warn("Notifications disabled, they're not working");
            return;
        }

        final XiaomiProto.Notification3.Builder notification3 = XiaomiProto.Notification3.newBuilder()
                .setId(notificationSpec.getId())
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

        // TODO what is this?
        final String unknown12 = String.format(
                Locale.ROOT,
                "0|%s|%d|null|12345",
                notification3.getPackage(),
                notification3.getId() // i think this needs to be converted to unsigned
        );
        notification3.setUnknown12(unknown12);

        final XiaomiProto.Notification2 notification2 = XiaomiProto.Notification2.newBuilder()
                .setNotification3(notification3)
                .build();

        final XiaomiProto.Notification notification = XiaomiProto.Notification.newBuilder()
                .setNotification2(notification2)
                .build();

        getSupport().sendCommand(
                "send notification",
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
        // TODO
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

    public void requestCannedMessages(final TransactionBuilder builder) {
        getSupport().sendCommand(
                builder,
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
}
