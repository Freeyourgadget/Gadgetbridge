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
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class XiaomiNotificationService extends AbstractXiaomiService implements XiaomiDataUploadService.Callback {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiNotificationService.class);

    private static final SimpleDateFormat TIMESTAMP_SDF = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ROOT);

    public static final int COMMAND_TYPE = 7;

    public static final int CMD_NOTIFICATION_SEND = 0;
    public static final int CMD_NOTIFICATION_DISMISS = 1;
    public static final int CMD_CALL_REJECT = 2;
    public static final int CMD_CALL_IGNORE = 5;
    public static final int CMD_SCREEN_ON_ON_NOTIFICATIONS_GET = 6;
    public static final int CMD_SCREEN_ON_ON_NOTIFICATIONS_SET = 7;
    public static final int CMD_OPEN_ON_PHONE = 8;
    public static final int CMD_CANNED_MESSAGES_GET = 9;
    public static final int CMD_CANNED_MESSAGES_SET = 12; // also canned message reply
    public static final int CMD_CALL_REPLY_SEND = 13;
    public static final int CMD_CALL_REPLY_ACK = 14;
    public static final int CMD_NOTIFICATION_ICON_REQUEST = 15;
    public static final int CMD_NOTIFICATION_ICON_QUERY = 16;

    // Maintain a queue of the last seen package names, since the band will send
    // requests with the package name truncated, and without an ID
    private final Queue<String> mPackages = new LinkedList<>();

    // Keep track of package names and keys for notification dismissal
    private final LimitedQueue<Integer, String> mNotificationPackageName = new LimitedQueue<>(128);
    private final LimitedQueue<Integer, String> mNotificationKey = new LimitedQueue<>(128);

    private String iconPackageName;

    public XiaomiNotificationService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void initialize() {
        getSupport().sendCommand("get screen on on notifications", COMMAND_TYPE, CMD_SCREEN_ON_ON_NOTIFICATIONS_GET);
        requestCannedMessages();
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        final GBDeviceEventCallControl deviceEvtCallControl = new GBDeviceEventCallControl();
        final GBDeviceEventNotificationControl deviceEvtNotificationControl = new GBDeviceEventNotificationControl();

        switch (cmd.getSubtype()) {
            case CMD_NOTIFICATION_DISMISS:
                LOG.info("Watch dismiss {} notifications", cmd.getNotification().getNotificationDismiss().getNotificationIdCount());
                for (final XiaomiProto.NotificationId notificationId : cmd.getNotification().getNotificationDismiss().getNotificationIdList()) {
                    LOG.debug("Watch dismiss {}", notificationId.getId());
                    deviceEvtNotificationControl.handle = notificationId.getId();
                    deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.DISMISS;
                    getSupport().evaluateGBDeviceEvent(deviceEvtNotificationControl);
                }
                return;
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
            case CMD_SCREEN_ON_ON_NOTIFICATIONS_GET:
                final boolean screenOnOnNotifications = cmd.getNotification().getScreenOnOnNotifications();
                LOG.debug("Got screen on on notifications: {}", screenOnOnNotifications);
                final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences(
                        DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS,
                        screenOnOnNotifications
                ).withPreference(
                        XiaomiPreferences.FEAT_SCREEN_ON_ON_NOTIFICATIONS,
                        true
                );
                getSupport().evaluateGBDeviceEvent(eventUpdatePreferences);
                return;
            case CMD_OPEN_ON_PHONE:
                LOG.debug("Open on phone {}", cmd.getNotification().getOpenOnPhone().getId());
                deviceEvtNotificationControl.handle = cmd.getNotification().getOpenOnPhone().getId();
                deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.OPEN;
                getSupport().evaluateGBDeviceEvent(deviceEvtNotificationControl);
            case CMD_CANNED_MESSAGES_GET:
                handleCannedMessages(cmd.getNotification().getCannedMessages());
                return;
            case CMD_CALL_REPLY_SEND:
                handleCannedSmsReply(cmd.getNotification().getNotificationReply());
                return;
            case CMD_NOTIFICATION_ICON_REQUEST:
                handleNotificationIconRequest(cmd.getNotification().getNotificationIconRequest());
                return;
            case CMD_NOTIFICATION_ICON_QUERY:
                handleNotificationIconQuery(cmd.getNotification().getNotificationIconQuery());
                return;
        }

        LOG.warn("Unhandled notification command {}", cmd.getSubtype());
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS:
                setScreenOnOnNotifications();
                return true;
        }

        return super.onSendConfiguration(config, prefs);
    }

    public void onNotification(final NotificationSpec notificationSpec) {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring");
            return;
        }

        final XiaomiProto.Notification3.Builder notification3 = XiaomiProto.Notification3.newBuilder()
                .setId(notificationSpec.getId())
                .setUnknown4("") // ?
                .setTimestamp(TIMESTAMP_SDF.format(new Date(notificationSpec.when)));

        if (notificationSpec.sourceAppId != null) {
            notification3.setPackage(notificationSpec.sourceAppId);
        } else {
            notification3.setPackage(BuildConfig.APPLICATION_ID);
        }

        mNotificationPackageName.add(notificationSpec.getId(), notificationSpec.sourceAppId);
        mNotificationKey.add(notificationSpec.getId(), notificationSpec.key);

        mPackages.add(notification3.getPackage());
        if (mPackages.size() > 32) {
            mPackages.poll();
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

        if (notificationSpec.key != null) {
            notification3.setKey(notificationSpec.key);
            notification3.setOpenOnPhone(true);
        }

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
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring delete");
            return;
        }

        final XiaomiProto.NotificationId notificationId = XiaomiProto.NotificationId.newBuilder()
                .setId(id)
                .setPackage(mNotificationPackageName.lookup(id))
                .setKey(mNotificationKey.lookup(id))
                .build();

        final XiaomiProto.NotificationDismiss notificationDismiss = XiaomiProto.NotificationDismiss.newBuilder()
                .addNotificationId(notificationId)
                .build();

        final XiaomiProto.Notification notification = XiaomiProto.Notification.newBuilder()
                .setNotificationDismiss(notificationDismiss)
                .build();

        getSupport().sendCommand(
                "delete notification " + id,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_NOTIFICATION_DISMISS)
                        .setNotification(notification)
                        .build()
        );
    }

    public void onSetCallState(final CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_OUTGOING) {
            return;
        }

        if (callSpec.command != CallSpec.CALL_INCOMING) {
            final XiaomiProto.NotificationDismiss.Builder notification4 = XiaomiProto.NotificationDismiss.newBuilder()
                    .addNotificationId(XiaomiProto.NotificationId.newBuilder().setId(0).setPackage("phone"));

            final XiaomiProto.Notification notification = XiaomiProto.Notification.newBuilder()
                    .setNotificationDismiss(notification4)
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

    private void setScreenOnOnNotifications() {
        final Prefs prefs = getDevicePrefs();

        final boolean screenOnOnNotificationsEnabled = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SCREEN_ON_ON_NOTIFICATIONS, true);

        LOG.info("Setting screen on on notification: {}", screenOnOnNotificationsEnabled);

        getSupport().sendCommand(
                "set screen on on notification",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_SCREEN_ON_ON_NOTIFICATIONS_SET)
                        .setNotification(XiaomiProto.Notification.newBuilder().setScreenOnOnNotifications(screenOnOnNotificationsEnabled).build())
                        .build()
        );
    }

    public void onSetCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        if (cannedMessagesSpec.type != CannedMessagesSpec.TYPE_REJECTEDCALLS) {
            LOG.warn("Got unsupported canned messages type: {}", cannedMessagesSpec.type);
            return;
        }

        final int minReplies = getDevicePrefs().getInt(XiaomiPreferences.PREF_CANNED_MESSAGES_MIN, 0);
        final int maxReplies = getDevicePrefs().getInt(XiaomiPreferences.PREF_CANNED_MESSAGES_MAX, 0);

        if (maxReplies == 0) {
            LOG.warn("Attempting to set canned messages, but max replies is 0");
            return;
        }

        final XiaomiProto.CannedMessages.Builder cannedMessagesBuilder = XiaomiProto.CannedMessages.newBuilder()
                .setMinReplies(minReplies)
                .setMaxReplies(maxReplies);
        int i = 0;
        for (final String cannedMessage : cannedMessagesSpec.cannedMessages) {
            if (i >= maxReplies) {
                LOG.warn("Got too many canned messages ({}), limit is {}", cannedMessagesSpec.cannedMessages.length, maxReplies);
                break;
            }

            cannedMessagesBuilder.addReply(cannedMessage);

            i++;
        }

        for (; i < minReplies; i++) {
            cannedMessagesBuilder.addReply("-");
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
        LOG.info("Got {} canned messages", cannedMessages.getReplyCount());

        final int minReplies = cannedMessages.getMinReplies();
        final int maxReplies = cannedMessages.getMaxReplies();
        final GBDeviceEventUpdatePreferences gbDeviceEventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(XiaomiPreferences.PREF_CANNED_MESSAGES_MIN, minReplies)
                .withPreference(XiaomiPreferences.PREF_CANNED_MESSAGES_MAX, maxReplies);

        int i = 1;
        for (final String reply : cannedMessages.getReplyList()) {
            gbDeviceEventUpdatePreferences.withPreference("canned_message_dismisscall_" + i, reply);
            i++;

            if (i > maxReplies || i > 16) {
                LOG.warn("Got too many canned messages ({})", i);
                break;
            }
        }

        for (int j = i; j <= maxReplies; j++) {
            gbDeviceEventUpdatePreferences.withPreference("canned_message_dismisscall_" + j, null);
        }

        getSupport().evaluateGBDeviceEvent(gbDeviceEventUpdatePreferences);
    }

    public boolean canSendSms() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return getSupport().getContext().checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void handleNotificationIconQuery(final XiaomiProto.NotificationIconPackage notificationIconPackage) {
        LOG.debug("Watch querying notification icon for {}", notificationIconPackage.getPackage());

        iconPackageName = notificationIconPackage.getPackage();

        if (NotificationUtils.getAppIcon(getSupport().getContext(), iconPackageName) == null) {
            // Attempt to find truncated package name
            for (final String fullPackage : mPackages) {
                if (fullPackage.startsWith(iconPackageName)) {
                    iconPackageName = fullPackage;
                    break;
                }
            }

            if (iconPackageName.equals(notificationIconPackage.getPackage())) {
                LOG.warn("Failed to match a full package for {}", iconPackageName);
                return;
            }

            if (NotificationUtils.getAppIcon(getSupport().getContext(), iconPackageName) == null) {
                LOG.warn("Failed to find icon for {} and {}", notificationIconPackage.getPackage(), iconPackageName);
                return;
            }
        }

        getSupport().sendCommand(
                "send notification reply for " + iconPackageName,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_NOTIFICATION_ICON_REQUEST)
                        .setNotification(XiaomiProto.Notification.newBuilder()
                                .setNotificationIconReply(notificationIconPackage)
                        ).build()
        );
    }

    private void handleCannedSmsReply(final XiaomiProto.NotificationReply notificationReply) {
        final String phoneNumber = notificationReply.getNumber();
        if (phoneNumber == null) {
            LOG.warn("Missing phone number for sms reply");
            ackSmsReply(false);
            return;
        }

        final String message = notificationReply.getMessage();
        if (message == null) {
            LOG.warn("Missing message for sms reply");
            ackSmsReply(false);
            return;
        }

        LOG.debug("Sending SMS message '{}' to number '{}' and rejecting call", message, phoneNumber);

        final GBDeviceEventNotificationControl devEvtNotificationControl = new GBDeviceEventNotificationControl();
        devEvtNotificationControl.handle = -1;
        devEvtNotificationControl.phoneNumber = phoneNumber;
        devEvtNotificationControl.reply = message;
        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
        getSupport().evaluateGBDeviceEvent(devEvtNotificationControl);

        final GBDeviceEventCallControl rejectCallCmd = new GBDeviceEventCallControl(GBDeviceEventCallControl.Event.REJECT);
        getSupport().evaluateGBDeviceEvent(rejectCallCmd);

        // FIXME probably premature
        ackSmsReply(true);
    }

    private void ackSmsReply(final boolean success) {
        getSupport().sendCommand(
                "ack sms reply success=" + success,
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_CALL_REPLY_ACK)
                        .setNotification(XiaomiProto.Notification.newBuilder()
                                .setNotificationReplyStatus(success ? 0 : 1)
                        ).build()
        );
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
            // FIXME the packageName is sometimes truncated
            LOG.warn("Failed to get icon for {}", iconPackageName);
            return;
        }

        final Bitmap bmp = BitmapUtil.toBitmap(icon);
        final Bitmap bmpResized = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmpResized);
        final Rect rect = new Rect(0, 0, size, size);
        canvas.drawBitmap(bmp, null, rect, null);

        // convert from RGBA To ABGR
        final ByteBuffer buf = ByteBuffer.allocate(size * size * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                //noinspection SuspiciousNameCombination x and y are flipped on purpose
                buf.putInt(bmpResized.getPixel(y, x));
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
