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

import static org.apache.commons.lang3.ArrayUtils.subarray;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Consumer;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsNotificationService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsNotificationService.class);

    public static final short ENDPOINT = 0x001e;

    public static final byte NOTIFICATION_CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte NOTIFICATION_CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte NOTIFICATION_CMD_SEND = 0x03;
    public static final byte NOTIFICATION_CMD_REPLY = 0x04;
    public static final byte NOTIFICATION_CMD_DISMISS = 0x05;
    public static final byte NOTIFICATION_CMD_REPLY_ACK = 0x06;
    public static final byte NOTIFICATION_CMD_ICON_REQUEST = 0x10;
    public static final byte NOTIFICATION_CMD_ICON_REQUEST_ACK = 0x11;
    public static final byte NOTIFICATION_CMD_PICTURE_REQUEST = 0x19;
    public static final byte NOTIFICATION_CMD_PICTURE_REQUEST_ACK = 0x1a;
    public static final byte NOTIFICATION_TYPE_NORMAL = (byte) 0xfa;
    public static final byte NOTIFICATION_TYPE_CALL = 0x03;
    public static final byte NOTIFICATION_TYPE_SMS = (byte) 0x05;
    public static final byte NOTIFICATION_SUBCMD_SHOW = 0x00;
    public static final byte NOTIFICATION_SUBCMD_DISMISS_FROM_PHONE = 0x02;
    public static final byte NOTIFICATION_DISMISS_NOTIFICATION = 0x03;
    public static final byte NOTIFICATION_DISMISS_MUTE_CALL = 0x02;
    public static final byte NOTIFICATION_DISMISS_REJECT_CALL = 0x01;
    public static final byte NOTIFICATION_CALL_STATE_START = 0x00;
    public static final byte NOTIFICATION_CALL_STATE_END = 0x02;

    private int version = -1;
    private boolean supportsPictures = false;
    private boolean supportsNotificationKey = false;

    // Keep track of Notification ID -> action handle, as BangleJSDeviceSupport.
    // This needs to be simplified.
    private final LimitedQueue<Integer, Long> mNotificationReplyAction = new LimitedQueue<>(16);

    // Keep track of notification pictures
    private final LimitedQueue<Integer, String> mNotificationPictures = new LimitedQueue<>(16);

    private final ZeppOsFileTransferService fileTransferService;

    public ZeppOsNotificationService(final ZeppOsSupport support, final ZeppOsFileTransferService fileTransferService) {
        super(support, true);
        this.fileTransferService = fileTransferService;
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, NOTIFICATION_CMD_CAPABILITIES_REQUEST);
    }

    @Override
    public void handlePayload(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        final byte cmd = buf.get();

        final GBDeviceEventNotificationControl deviceEvtNotificationControl = new GBDeviceEventNotificationControl();
        final GBDeviceEventCallControl deviceEvtCallControl = new GBDeviceEventCallControl();

        switch (cmd) {
            case NOTIFICATION_CMD_CAPABILITIES_RESPONSE: {
                version = buf.get() & 0xff;
                if (version < 4 || version > 5) {
                    // Untested, might work, might not..
                    LOG.warn("Unsupported notification service version {}", version);
                }
                if (version >= 4) {
                    final short unk1 = buf.getShort(); // 100
                    final byte unk2 = buf.get(); // 1
                    final byte unk3 = buf.get(); // 1
                    final short unk4count = buf.getShort();
                    buf.get(new byte[unk4count]);
                }
                if (version >= 5) {
                    supportsPictures = buf.get() != 0;
                    supportsNotificationKey = buf.get() != 0;
                }
                LOG.info("Notification service version={}, supportsPictures={}", version, supportsPictures);
                break;
            }
            case NOTIFICATION_CMD_REPLY: {
                // TODO make this configurable?
                final int notificationId = BLETypeConversions.toUint32(subarray(payload, 1, 5));
                final Long replyHandle = mNotificationReplyAction.lookup(notificationId);
                if (replyHandle == null) {
                    LOG.warn("Failed to find reply handle for notification ID {}", notificationId);
                    return;
                }
                final String replyMessage = StringUtils.untilNullTerminator(payload, 5);
                if (replyMessage == null) {
                    LOG.warn("Failed to parse reply message for notification ID {}", notificationId);
                    return;
                }

                LOG.info("Got reply to notification {} with '{}'", notificationId, replyMessage);

                deviceEvtNotificationControl.handle = replyHandle;
                deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
                deviceEvtNotificationControl.reply = replyMessage;
                evaluateGBDeviceEvent(deviceEvtNotificationControl);

                ackNotificationReply(notificationId); // FIXME: premature?
                deleteNotification(notificationId); // FIXME: premature?
                return;
            }
            case NOTIFICATION_CMD_DISMISS:
                switch (payload[1]) {
                    case NOTIFICATION_DISMISS_NOTIFICATION:
                        // TODO make this configurable?
                        final int dismissNotificationId = BLETypeConversions.toUint32(subarray(payload, 2, 6));
                        LOG.info("Dismiss notification {}", dismissNotificationId);
                        deviceEvtNotificationControl.handle = dismissNotificationId;
                        deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.DISMISS;
                        evaluateGBDeviceEvent(deviceEvtNotificationControl);
                        return;
                    case NOTIFICATION_DISMISS_MUTE_CALL:
                        LOG.info("Mute call");
                        deviceEvtCallControl.event = GBDeviceEventCallControl.Event.IGNORE;
                        evaluateGBDeviceEvent(deviceEvtCallControl);
                        return;
                    case NOTIFICATION_DISMISS_REJECT_CALL:
                        LOG.info("Reject call");
                        deviceEvtCallControl.event = GBDeviceEventCallControl.Event.REJECT;
                        evaluateGBDeviceEvent(deviceEvtCallControl);
                        return;
                    default:
                        LOG.warn("Unexpected notification dismiss byte {}", String.format("0x%02x", payload[1]));
                        return;
                }
            case NOTIFICATION_CMD_ICON_REQUEST: {
                final String packageName = StringUtils.untilNullTerminator(payload, 1);
                if (packageName == null) {
                    LOG.error("Failed to decode package name from payload");
                    return;
                }
                LOG.info("Got notification icon request for {}", packageName);

                final int expectedLength = packageName.length() + 7;
                if (payload.length != expectedLength) {
                    LOG.error("Unexpected icon request payload length {}, expected {}", payload.length, expectedLength);
                    return;
                }
                int pos = 1 + packageName.length() + 1;
                final byte iconFormat = payload[pos];
                pos++;
                int width = BLETypeConversions.toUint16(subarray(payload, pos, pos + 2));
                pos += 2;
                int height = BLETypeConversions.toUint16(subarray(payload, pos, pos + 2));
                sendIconForPackage(packageName, iconFormat, width, height);
                return;
            }
            case NOTIFICATION_CMD_PICTURE_REQUEST: {
                final String packageName = StringUtils.untilNullTerminator(buf);
                if (packageName == null) {
                    LOG.error("Failed to decode package name for picture from payload");
                    return;
                }

                final int notificationId = buf.getInt();
                final byte pictureFormat = buf.get();
                final int width = buf.getShort();
                final int height = buf.getShort();

                LOG.info(
                        "Got notification picture request for {}, {}, {}, {}x{}",
                        packageName,
                        notificationId,
                        pictureFormat,
                        width,
                        height
                );

                sendNotificationPicture(packageName, notificationId, pictureFormat, width);
                return;
            }
            default:
                LOG.warn("Unexpected notification byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public int maxLength() {
        return 512;
    }

    public void setCallState(final CallSpec callSpec) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            final TransactionBuilder builder = new TransactionBuilder("send notification");

            baos.write(NOTIFICATION_CMD_SEND);

            // ID
            baos.write(BLETypeConversions.fromUint32(0));

            baos.write(NOTIFICATION_TYPE_CALL);
            if (callSpec.command == CallSpec.CALL_INCOMING) {
                baos.write(NOTIFICATION_CALL_STATE_START);
            } else if ((callSpec.command == CallSpec.CALL_START) || (callSpec.command == CallSpec.CALL_END)) {
                baos.write(NOTIFICATION_CALL_STATE_END);
            }

            baos.write(0x00); // ?
            if (callSpec.name != null) {
                baos.write(callSpec.name.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0x00);

            baos.write(0x00); // ?
            baos.write(0x00); // ?

            if (callSpec.number != null) {
                baos.write(callSpec.number.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0x00);

            // TODO put this behind a setting?
            baos.write(callSpec.number != null ? 0x01 : 0x00); // reply from watch

            write(builder, baos.toByteArray());
            builder.queue(getSupport().getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to send call", e);
        }
    }

    public void sendNotification(final NotificationSpec notificationSpec) {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring");
            return;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final String senderOrTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);

        // TODO Check real limit for notificationMaxLength / respect across all fields

        try {
            final TransactionBuilder builder = new TransactionBuilder("send notification");

            baos.write(NOTIFICATION_CMD_SEND);
            baos.write(BLETypeConversions.fromUint32(notificationSpec.getId()));
            if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                baos.write(NOTIFICATION_TYPE_SMS);
            } else {
                baos.write(NOTIFICATION_TYPE_NORMAL);
            }
            baos.write(NOTIFICATION_SUBCMD_SHOW);

            // app package
            if (notificationSpec.sourceAppId != null) {
                baos.write(notificationSpec.sourceAppId.getBytes(StandardCharsets.UTF_8));
            } else {
                // Send the GB package name, otherwise the last notification icon will
                // be used wrongly (eg. when receiving an SMS)
                baos.write(BuildConfig.APPLICATION_ID.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // sender/title
            if (!senderOrTitle.isEmpty()) {
                baos.write(senderOrTitle.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // body
            if (notificationSpec.body != null) {
                baos.write(StringUtils.truncate(notificationSpec.body, maxLength()).getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // app name
            if (notificationSpec.sourceName != null) {
                baos.write(notificationSpec.sourceName.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0);

            // reply
            boolean hasReply = false;
            if (notificationSpec.attachedActions != null && !notificationSpec.attachedActions.isEmpty()) {
                for (int i = 0; i < notificationSpec.attachedActions.size(); i++) {
                    final NotificationSpec.Action action = notificationSpec.attachedActions.get(i);

                    switch (action.type) {
                        case NotificationSpec.Action.TYPE_WEARABLE_REPLY:
                        case NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR:
                            hasReply = true;
                            mNotificationReplyAction.add(notificationSpec.getId(), action.handle);
                            break;
                        default:
                            break;
                    }
                }
            }

            baos.write((byte) (hasReply ? 1 : 0));
            if (version >= 5) {
                baos.write(0); // 1 for silent
            }
            if (supportsPictures) {
                baos.write((byte) (notificationSpec.picturePath != null ? 1 : 0));
                if (notificationSpec.picturePath != null) {
                    mNotificationPictures.add(notificationSpec.getId(), notificationSpec.picturePath);
                }
            }
            if (supportsNotificationKey) {
                if (notificationSpec.key != null) {
                    baos.write(notificationSpec.key.getBytes(StandardCharsets.UTF_8));
                }
                baos.write(0);
            }

            write(builder, baos.toByteArray());
            builder.queue(getSupport().getQueue());
        } catch (final Exception e) {
            LOG.error("Failed to send notification", e);
        }
    }

    public void deleteNotification(final int id) {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring delete");
            return;
        }

        mNotificationPictures.remove(id);

        LOG.info("Deleting notification {} from band", id);

        final ByteBuffer buf = ByteBuffer.allocate(12);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATION_CMD_SEND);
        buf.putInt(id);
        buf.put(NOTIFICATION_TYPE_NORMAL);
        buf.put(NOTIFICATION_SUBCMD_DISMISS_FROM_PHONE);
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?

        write("delete notification", buf.array());
    }

    private void ackNotificationReply(final int notificationId) {
        final ByteBuffer buf = ByteBuffer.allocate(9);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATION_CMD_REPLY_ACK);
        buf.putInt(notificationId);
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x00); // ?

        write("ack notification reply", buf.array());
    }

    private void ackNotificationAfterIconSent(final String queuedIconPackage, final boolean success) {
        if (!success) {
            return;
        }

        LOG.info("Acknowledging icon send for {}", queuedIconPackage);

        final ByteBuffer buf = ByteBuffer.allocate(1 + queuedIconPackage.length() + 1 + 1);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATION_CMD_ICON_REQUEST_ACK);
        buf.put(queuedIconPackage.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.put((byte) 0x01); // TODO !success?

        write("ack icon send", buf.array());
    }

    private void ackNotificationAfterPictureSent(final String packageName, final int notificationId, final boolean success) {
        LOG.info("Acknowledging picture send for {}", packageName);

        final ByteBuffer buf = ByteBuffer.allocate(1 + packageName.length() + 1 + 4 + 1).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(NOTIFICATION_CMD_PICTURE_REQUEST_ACK);
        buf.put(packageName.getBytes(StandardCharsets.UTF_8));
        buf.put((byte) 0x00);
        buf.putInt(notificationId);
        buf.put((byte) (success ? 0x01 : 0x02));

        write("ack picture send", buf.array());
    }

    private void sendIconForPackage(final String packageName, final byte iconFormat, final int width, final int height) {
        final BitmapFormat format = BitmapFormat.fromCode(iconFormat);
        if (format == null) {
            LOG.error("Unknown icon bitmap format code {}", String.format("0x%02x", iconFormat));
            return;
        }

        final Drawable icon = NotificationUtils.getAppIcon(getContext(), packageName);
        if (icon == null) {
            LOG.warn("Failed to get icon for {}", packageName);
            return;
        }

        final Bitmap bmp = BitmapUtil.toBitmap(icon);
        final byte[] tga = encodeBitmap(bmp, format, width, height);

        final String url = String.format(
                Locale.ROOT,
                "notification://logo?app_id=%s&width=%d&height=%d&format=%s",
                packageName,
                width,
                height,
                format
        );
        final String filename = String.format("logo_%s.tga", packageName.replace(".", "_"));

        sendFile(url, filename, tga, false, success -> ackNotificationAfterIconSent(packageName, success));
    }

    private void sendNotificationPicture(final String packageName, final int notificationId, final byte pictureFormat, final int width) {
        final BitmapFormat format = BitmapFormat.fromCode(pictureFormat);
        if (format == null) {
            LOG.error("Unknown picture bitmap format code {}", String.format("0x%02x", pictureFormat));
            ackNotificationAfterPictureSent(packageName, notificationId, false);
            return;
        }

        final String picturePath = mNotificationPictures.lookup(notificationId);
        if (picturePath == null) {
            LOG.warn("Failed to find picture path for {}", notificationId);
            ackNotificationAfterPictureSent(packageName, notificationId, false);
            return;
        }

        final Bitmap bmp = BitmapFactory.decodeFile(picturePath);
        if (bmp == null) {
            LOG.warn("Failed to decode bitmap from {}", picturePath);
            ackNotificationAfterPictureSent(packageName, notificationId, false);
            return;
        }

        // FIXME: On the GTR 4, the band sends 358 on the url, but the actual image has 368 width
        //  if sent as requested, it gets all corrupted...
        final int targetWidth = width + 10;
        final int targetHeight = (int) Math.round(bmp.getHeight() * ((double) targetWidth / bmp.getWidth()));
        final byte[] tga = encodeBitmap(bmp, format, targetWidth, targetHeight);

        final String url = String.format(
                Locale.ROOT,
                "notification://content_image?app_id=%s&uid=%d&width=%d&height=%d&format=%s",
                packageName,
                notificationId,
                width,
                targetHeight,
                format
        );
        final String filename = String.format(Locale.ROOT, "picture_%d.tga", notificationId);

        sendFile(url, filename, tga, true, success -> ackNotificationAfterPictureSent(packageName, notificationId, success));
    }

    private void sendFile(final String url,
                          final String filename,
                          final byte[] bytes,
                          final boolean compress,
                          final Consumer<Boolean> uploadFinishCallback) {
        if (getSupport().getMTU() < 247) {
            LOG.warn("Sending files requires high MTU, current MTU is {}", getSupport().getMTU());
            return;
        }

        fileTransferService.sendFile(
                url,
                filename,
                bytes,
                true,
                new ZeppOsFileTransferService.Callback() {
                    @Override
                    public void onFileUploadFinish(final boolean success) {
                        LOG.info("Finished sending '{}' to '{}', success={}", filename, url, success);
                        uploadFinishCallback.accept(success);
                    }

                    @Override
                    public void onFileUploadProgress(final int progress) {
                        LOG.trace("File send progress: {}", progress);
                    }

                    @Override
                    public void onFileDownloadFinish(final String url, final String filename, final byte[] data) {
                        LOG.warn("Receiver unexpected file: url={} filename={} length={}", url, filename, data.length);
                    }
                }
        );

        LOG.info("Queueing file send '{}' to '{}'", filename, url);
    }

    private static byte[] encodeBitmap(final Bitmap bmp, final BitmapFormat format, final int width, final int height) {
        // Without the expected tga id and format string they seem to get corrupted,
        // but the encoding seems to actually be the same...?
        // The TGA needs to have this ID, or the band does not accept it
        final byte[] tgaIdBytes = new byte[46];
        System.arraycopy(format.getTgaId().getBytes(StandardCharsets.UTF_8), 0, tgaIdBytes, 0, 5);

        return BitmapUtil.convertToTgaRGB565(bmp, width, height, tgaIdBytes);
    }

    private enum BitmapFormat {
        TGA_RGB565_GCNANOLITE(0x04, "SOMHP"),
        TGA_RGB565_DAVE2D(0x08, "SOMH6"),
        ;

        private final byte code;
        private final String tgaId;

        BitmapFormat(final int code, final String tgaId) {
            this.code = (byte) code;
            this.tgaId = tgaId;
        }

        public byte getCode() {
            return code;
        }

        public String getTgaId() {
            return tgaId;
        }

        @Nullable
        public static BitmapFormat fromCode(final byte code) {
            for (final BitmapFormat format : BitmapFormat.values()) {
                if (format.code == code) {
                    return format;
                }
            }

            return null;
        }
    }
}
