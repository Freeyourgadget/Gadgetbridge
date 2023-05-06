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

import static org.apache.commons.lang3.ArrayUtils.subarray;

import android.Manifest;
import android.content.pm.PackageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsCannedMessagesService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsCannedMessagesService.class);

    public static final short ENDPOINT = 0x0013;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_REQUEST = 0x03;
    public static final byte CMD_RESPONSE = 0x04;
    public static final byte CMD_SET = 0x05;
    public static final byte CMD_SET_ACK = 0x06;
    public static final byte CMD_DELETE = 0x07;
    public static final byte CMD_DELETE_ACK = 0x08;
    public static final byte CMD_REPLY_SMS = 0x0b;
    public static final byte CMD_REPLY_SMS_ACK = 0x0c;
    public static final byte CMD_REPLY_SMS_CHECK = 0x0d;
    public static final byte CMD_REPLY_SMS_ALLOW = 0x0e;

    public ZeppOsCannedMessagesService(final Huami2021Support support) {
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
            case CMD_RESPONSE:
                decodeAndUpdateCannedMessagesResponse(payload);
                return;
            case CMD_SET_ACK:
                LOG.info("Canned Message set ACK, status = {}", payload[1]);
                return;
            case CMD_DELETE_ACK:
                LOG.info("Canned Message delete ACK, status = {}", payload[1]);
                return;
            case CMD_REPLY_SMS:
                handleCannedSmsReply(payload);
                return;
            case CMD_REPLY_SMS_CHECK:
                LOG.info("Canned Message reply SMS check");
                final boolean canSendSms;
                // TODO place this behind a setting as well?
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    canSendSms = getContext().checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
                } else {
                    canSendSms = true;
                }
                sendCannedSmsReplyAllow(canSendSms);
                return;
            default:
                LOG.warn("Unexpected canned messages payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    public void setCannedMessages(final CannedMessagesSpec cannedMessagesSpec) {
        if (cannedMessagesSpec.type != CannedMessagesSpec.TYPE_GENERIC) {
            LOG.warn("Got unsupported canned messages type: {}", cannedMessagesSpec.type);
            return;
        }

        final TransactionBuilder builder = new TransactionBuilder("set canned messages");

        for (int i = 0; i < 16; i++) {
            LOG.debug("Deleting canned message {}", i);
            final ByteBuffer buf = ByteBuffer.allocate(5);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(CMD_DELETE);
            buf.putInt(i);
            write(builder, buf.array());
        }

        int i = 0;
        for (String cannedMessage : cannedMessagesSpec.cannedMessages) {
            cannedMessage = StringUtils.truncate(cannedMessage, 140);
            LOG.debug("Setting canned message {} = '{}'", i, cannedMessage);

            final int length = cannedMessage.getBytes(StandardCharsets.UTF_8).length + 7;
            final ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(CMD_SET);
            buf.putInt(i++);
            buf.put((byte) cannedMessage.getBytes(StandardCharsets.UTF_8).length);
            buf.put((byte) 0x00);
            buf.put(cannedMessage.getBytes(StandardCharsets.UTF_8));
            write(builder, buf.array());
        }
        builder.queue(getSupport().getQueue());
    }

    public void requestCannedMessages(final TransactionBuilder builder) {
        LOG.info("Requesting canned messages");

        write(builder, new byte[]{CMD_REQUEST});
    }

    private void sendCannedSmsReplyAllow(final boolean allowed) {
        LOG.info("Sending SMS reply allowed = {}", allowed);

        write("allow sms reply", new byte[]{CMD_REPLY_SMS_ALLOW, bool(allowed)});
    }

    private void handleCannedSmsReply(final byte[] payload) {
        LOG.info("Canned Message SMS reply");

        final String phoneNumber = StringUtils.untilNullTerminator(payload, 1);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            LOG.warn("No phone number for SMS reply");
            ackCannedSmsReply(false);
            return;
        }

        final int messageLength = payload[phoneNumber.length() + 6] & 0xff;
        if (phoneNumber.length() + 8 + messageLength != payload.length) {
            LOG.warn("Unexpected message or payload lengths ({} / {})", messageLength, payload.length);
            ackCannedSmsReply(false);
            return;
        }

        final String message = new String(payload, phoneNumber.length() + 8, messageLength);
        if (StringUtils.isNullOrEmpty(message)) {
            LOG.warn("No message for SMS reply");
            ackCannedSmsReply(false);
            return;
        }

        LOG.debug("Sending SMS message '{}' to number '{}' and rejecting call", message, phoneNumber);
        final GBDeviceEventNotificationControl devEvtNotificationControl = new GBDeviceEventNotificationControl();
        devEvtNotificationControl.handle = -1;
        devEvtNotificationControl.phoneNumber = phoneNumber;
        devEvtNotificationControl.reply = message;
        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
        evaluateGBDeviceEvent(devEvtNotificationControl);

        final GBDeviceEventCallControl rejectCallCmd = new GBDeviceEventCallControl(GBDeviceEventCallControl.Event.REJECT);
        evaluateGBDeviceEvent(rejectCallCmd);

        ackCannedSmsReply(true); // FIXME probably premature
    }

    private void ackCannedSmsReply(final boolean success) {
        LOG.info("Acknowledging SMS reply, success = {}", success);

        write("ack sms reply", new byte[]{CMD_REPLY_SMS_ACK, bool(success)});
    }

    private void decodeAndUpdateCannedMessagesResponse(final byte[] payload) {
        final int numberMessages = payload[1] & 0xff;

        LOG.info("Got {} canned messages", numberMessages);

        final GBDeviceEventUpdatePreferences gbDeviceEventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        final Map<Integer, String> cannedMessages = new HashMap<>();

        int pos = 3;
        for (int i = 0; i < numberMessages; i++) {
            if (pos + 4 >= payload.length) {
                LOG.warn("Unexpected end of payload while parsing message {} at pos {}", i, pos);
                return;
            }

            final int messageId = BLETypeConversions.toUint32(subarray(payload, pos, pos + 4));
            final int messageLength = payload[pos + 4] & 0xff;

            if (pos + 6 + messageLength > payload.length) {
                LOG.warn("Unexpected end of payload for message of length {} while parsing message {} at pos {}", messageLength, i, pos);
                return;
            }

            final String messageText = new String(subarray(payload, pos + 6, pos + 6 + messageLength));

            LOG.debug("Canned message {}: {}", String.format("0x%x", messageId), messageText);

            final int cannedMessagePrefId = i + 1;
            if (cannedMessagePrefId > 16) {
                LOG.warn("Canned message ID {} is out of range", cannedMessagePrefId);
            } else {
                cannedMessages.put(cannedMessagePrefId, messageText);
            }

            pos += messageLength + 6;
        }

        for (int i = 1; i <= 16; i++) {
            String message = cannedMessages.get(i);
            if (StringUtils.isEmpty(message)) {
                message = null;
            }

            gbDeviceEventUpdatePreferences.withPreference("canned_reply_" + i, message);
        }

        evaluateGBDeviceEvent(gbDeviceEventUpdatePreferences);
    }
}
