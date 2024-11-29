/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Notifications {
    public static final byte id = 0x02;
    public static final byte[] defaultConstraints = new byte[]{
            0x00, 0x02,   0x00, 0x0F,
            0x00, 0x00,   0x00, 0x02,   0x00, 0x1E,
            0x00, 0x00,   0x00, 0x02,   0x00, 0x1E,
            0x00, 0x00,   0x00, 0x02,   0x00, 0x1E
    };

    public static class NotificationActionRequest extends HuaweiPacket {
        public static final byte id = 0x01;

        // TODO: support other types of notifications
        //        public static final int send = 0x01;
        //        public static final int notificationId = 0x01;
        //        public static final int notificationType = 0x02;
        //        public static final int vibrate = 0x03;
        //        public static final int payloadEmpty = 0x04;
        //        public static final int imageHeight = 0x08;
        //        public static final int imageWidth = 0x09;
        //        public static final int imageColor = 0x0A;
        //        public static final int imageData = 0x0B;
        //        public static final int textType = 0x0E;
        //        public static final int textEncoding = 0x0F;
        //        public static final int textContent = 0x10;
        //        public static final int sourceAppId = 0x11;
        //        public static final int payloadText = 0x84;
        //        public static final int payloadImage = 0x86;
        //        public static final int textList = 0x8C;
        //        public static final int textItem = 0x8D;

        public NotificationActionRequest(
                ParamsProvider paramsProvider,
                short notificationId,
                byte notificationType,
                int encoding,
                String titleContent,
                String senderContent,
                String bodyContent,
                String sourceAppId
        ) {
            super(paramsProvider);

            this.serviceId = Notifications.id;
            this.commandId = id;

            // TODO: Add notification information per type if necessary

            this.tlv = new HuaweiTLV()
                    .put(0x01, notificationId)
                    .put(0x02, notificationType)
                    .put(0x03, true); // This used to be vibrate, but doesn't work

            HuaweiTLV subTlv = new HuaweiTLV();
            if (titleContent != null && !titleContent.isEmpty())
                subTlv.put(0x8D, new HuaweiTLV()
                        .put(0x0E, (byte) TextType.title)
                        .put(0x0F, (byte) encoding)
                        .put(0x10, titleContent)
                );

            if (senderContent != null && !senderContent.isEmpty())
                subTlv.put(0x8D, new HuaweiTLV()
                        .put(0x0E, (byte) TextType.sender)
                        .put(0x0F, (byte) encoding)
                        .put(0x10, senderContent)
                );

            if (bodyContent != null && !bodyContent.isEmpty())
                subTlv.put(0x8D, new HuaweiTLV()
                        .put(0x0E, (byte) TextType.text)
                        .put(0x0F, (byte) encoding)
                        .put(0x10, bodyContent)
                );

            if (subTlv.length() != 0) {
                this.tlv.put(0x84, new HuaweiTLV().put(0x8C, subTlv));
            } else {
                this.tlv.put(0x04);
            }

            if (sourceAppId != null)
                this.tlv.put(0x11, sourceAppId);

            this.complete = true;
        }
    }

    public static class NotificationConstraints {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Notifications.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                    .put(0x01);
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public ByteBuffer constraints;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Notifications.id;
                this.commandId = id;
                this.complete = true;
            }

            private void putByteBuffer(ByteBuffer bBuffer, byte position, byte[] value) {
                ByteBuffer bValue = ByteBuffer.wrap(value);
                if (bValue.capacity() == 4) {
                    short highbytes = bValue.getShort();
                    if (highbytes != 0) {
                        throw new RuntimeException("This should not happen until very large messages allowed");
                    }
                    bBuffer.putShort(position, bValue.getShort());
                } else if (bValue.capacity() == 2) {
                    bBuffer.putShort(position, bValue.getShort());
                } else {
                    bBuffer.put(position, (byte) 0x00);
                    bBuffer.put(position + 1, bValue.get());
                }
            }

            @Override
            public void parseTlv() throws ParseException {
                this.constraints = ByteBuffer.allocate(22);
                HuaweiTLV container = this.tlv
                        .getObject(0x81)
                        .getObject(0x82)
                        .getObject(0x90);
                for (HuaweiTLV subContainer : container.getObjects(0x91)) {
                    if (subContainer.getByte(0x12) == 0x01) {
                        putByteBuffer(constraints, NotificationConstraintsType.contentFormat, new byte[] {0x02}); //Always 0x02 even if gadget report 0x03
                        putByteBuffer(constraints, NotificationConstraintsType.contentLength, subContainer.getBytes(0x14));
                    }
                    if (subContainer.getByte(0x12) == 0x05) {
                        constraints.putShort(NotificationConstraintsType.yellowPagesSupport,(short)0x01);
                        putByteBuffer(constraints, NotificationConstraintsType.yellowPagesFormat,subContainer.getBytes(0x13));
                        putByteBuffer(constraints, NotificationConstraintsType.yellowPagesLength,subContainer.getBytes(0x14));
                    }
                    if (subContainer.getByte(0x12) == 0x06) {
                        constraints.putShort(NotificationConstraintsType.contentSignSupport,(short)0x01);
                        putByteBuffer(constraints, NotificationConstraintsType.contentSignFormat,subContainer.getBytes(0x13));
                        putByteBuffer(constraints, NotificationConstraintsType.contentSignLength,subContainer.getBytes(0x14));
                    }
                    if (subContainer.getByte(0x12) == 0x07 ) {
                        constraints.putShort(NotificationConstraintsType.incomingNumberSupport,(short)0x01);
                        putByteBuffer(constraints, NotificationConstraintsType.incomingNumberFormat,subContainer.getBytes(0x13));
                        putByteBuffer(constraints, NotificationConstraintsType.incomingNumberLength,subContainer.getBytes(0x14));
                    }
                }
                constraints.rewind();
            }
        }
    }

    public static class NotificationConstraintsType {
        // TODO: enum?
        public static final byte contentFormat = 0x00;
        public static final byte contentLength = 0x02;
        public static final byte yellowPagesSupport = 0x04;
        public static final byte yellowPagesFormat = 0x06;
        public static final byte yellowPagesLength = 0x08;
        public static final byte contentSignSupport = 0x0A;
        public static final byte contentSignFormat = 0x0C;
        public static final byte contentSignLength = 0x0E;
        public static final byte incomingNumberSupport = 0x10;
        public static final byte incomingNumberFormat = 0x12;
        public static final byte incomingNumberLength = 0x14;
    }

    public static class NotificationType {
        // TODO: enum?
        public static final byte call = 0x01;
        public static final byte sms = 0x02;
        public static final byte weChat = 0x03;
        public static final byte qq = 0x0B;
        public static final byte stopNotification = 0x0C; // To stop showing a (call) notification
        public static final byte missedCall = 0x0E;
        public static final byte email = 0x0F;
        public static final byte generic = 0x7F;
    }

    public static class TextType {
        // TODO: enum?
        public static final int text = 0x01;
        public static final int sender = 0x02;
        public static final int title = 0x03;
        public static final int yellowPage = 0x05;
        public static final int contentSign = 0x06;
        public static final int flight = 0x07;
        public static final int train = 0x08;
        public static final int warmRemind = 0x09;
        public static final int weather = 0x0A;
    }

    public static class NotificationStateRequest extends HuaweiPacket {
        public static final byte id = 0x04;

        public NotificationStateRequest(
                ParamsProvider paramsProvider,
                boolean status
        ) {
            super(paramsProvider);

            this.serviceId = Notifications.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x81, new HuaweiTLV()
                            .put(0x02, status)
                            .put(0x03, status)
                    );

            this.complete = true;
        }
    }

    public static class NotificationCapabilities {
        public static final byte id = 0x05;

        public static class Request extends HuaweiPacket {
            public Request(
                    ParamsProvider paramsProvider
            ){
                super(paramsProvider);
                this.serviceId = Notifications.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x01);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public byte capabilities = 0x00;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = DeviceConfig.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (this.tlv.contains(0x01))
                    this.capabilities = this.tlv.getByte(0x01);
            }
        }
    }

    public static class WearMessagePushRequest extends HuaweiPacket {
        public static final byte id = 0x08;

        public WearMessagePushRequest(
                ParamsProvider paramsProvider,
                boolean status
        ) {
            super(paramsProvider);

            this.serviceId = Notifications.id;
            this.commandId = id;

            /* Value sent is the opposite of the switch status */
            this.tlv = new HuaweiTLV()
                    .put(0x01, !status);

            this.complete = true;
        }
    }
}
