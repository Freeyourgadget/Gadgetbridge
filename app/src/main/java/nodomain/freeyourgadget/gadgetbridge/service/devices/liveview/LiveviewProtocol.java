/*  Copyright (C) 2016-2019 Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.liveview;

import java.nio.ByteBuffer;
import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class LiveviewProtocol extends GBDeviceProtocol {

    @Override
    public byte[] encodeFindDevice(boolean start) {
        return encodeVibrateRequest((short) 100, (short) 200);
    }

    protected LiveviewProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        int length = responseData.length;
        if (length < 4) {
            //empty message
            return null;
        } else {
            ByteBuffer buffer = ByteBuffer.wrap(responseData, 0, length);
            byte msgId = buffer.get();
            buffer.get();
            int payloadLen = buffer.getInt();
            GBDeviceEventSendBytes reply = new GBDeviceEventSendBytes();
            if (payloadLen + 6 == length) {
                switch (msgId) {
                    case LiveviewConstants.MSG_DEVICESTATUS:
                        reply.encodedBytes = constructMessage(LiveviewConstants.MSG_DEVICESTATUS_ACK, new byte[]{LiveviewConstants.RESULT_OK});
                        break;
                    case LiveviewConstants.MSG_DISPLAYPANEL_ACK:
                        reply.encodedBytes = encodeVibrateRequest((short) 100, (short) 200); //hack to make the notifications vibrate!
                        break;
                    default:
                }
                GBDeviceEventSendBytes ack = new GBDeviceEventSendBytes();
                ack.encodedBytes = constructMessage(LiveviewConstants.MSG_ACK, new byte[]{msgId});

                return new GBDeviceEvent[]{ack, reply};
            }
        }


        return super.decodeResponse(responseData);
    }

    @Override
    public byte[] encodeSetTime() {
        int time = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
        time += Calendar.getInstance().get(Calendar.ZONE_OFFSET) / 1000;
        time += Calendar.getInstance().get(Calendar.DST_OFFSET) / 1000;
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.order(LiveviewConstants.BYTE_ORDER);
        buffer.putInt(time);
        buffer.put(LiveviewConstants.CLOCK_24H);
        return constructMessage(LiveviewConstants.MSG_GETTIME_RESP, buffer.array());
    }

    @Override
    public byte[] encodeNotification(NotificationSpec notificationSpec) {
        String headerText;
        // for SMS and EMAIL that came in though SMS or K9 receiver
        if (notificationSpec.sender != null) {
            headerText = notificationSpec.sender;
        } else {
            headerText = notificationSpec.title;
        }

        String footerText = (null != notificationSpec.sourceName) ? notificationSpec.sourceName : "";
        String bodyText = (null != notificationSpec.body) ? notificationSpec.body : "";

        byte[] headerTextArray = headerText.getBytes(LiveviewConstants.ENCODING);
        byte[] footerTextArray = footerText.getBytes(LiveviewConstants.ENCODING);
        byte[] bodyTextArray = bodyText.getBytes(LiveviewConstants.ENCODING);
        int size = 15 + headerTextArray.length + bodyTextArray.length + footerTextArray.length;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.put((byte) 80); //should alert but it doesn't make the liveview vibrate

        buffer.put((byte) 0); //0 is for plaintext vs bitmapimage (1) strings
        buffer.putShort((short) headerTextArray.length);
        buffer.put(headerTextArray);
        buffer.putShort((short) bodyTextArray.length);
        buffer.put(bodyTextArray);
        buffer.putShort((short) footerTextArray.length);
        buffer.put(footerTextArray);
        return constructMessage(LiveviewConstants.MSG_DISPLAYPANEL, buffer.array());
    }


    //specific messages

    public static byte[] constructMessage(byte messageType, byte[] payload) {
        ByteBuffer msgBuffer = ByteBuffer.allocate(payload.length + 6);
        msgBuffer.order(LiveviewConstants.BYTE_ORDER);
        msgBuffer.put(messageType);
        msgBuffer.put((byte) 4);
        msgBuffer.putInt(payload.length);
        msgBuffer.put(payload);
        return msgBuffer.array();
    }

    public byte[] encodeVibrateRequest(short delay, short time) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(LiveviewConstants.BYTE_ORDER);
        buffer.putShort(delay);
        buffer.putShort(time);
        return constructMessage(LiveviewConstants.MSG_SETVIBRATE, buffer.array());
    }

    public byte[] encodeCapabilitiesRequest() {
        byte[] version = LiveviewConstants.CLIENT_SOFTWARE_VERSION.getBytes(LiveviewConstants.ENCODING);
        ByteBuffer buffer = ByteBuffer.allocate(version.length + 1);
        buffer.order(LiveviewConstants.BYTE_ORDER);
        buffer.put((byte) version.length);
        buffer.put(version);
        return constructMessage(LiveviewConstants.MSG_GETCAPS, buffer.array());
    }
}
