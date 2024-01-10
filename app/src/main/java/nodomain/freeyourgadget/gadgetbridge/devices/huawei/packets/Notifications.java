/*  Copyright (C) 2024 Damien Gaignon

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
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Notifications {
    public static final byte id = 0x02;

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
                byte titleEncoding,
                String titleContent,
                byte senderEncoding,
                String senderContent,
                byte bodyEncoding,
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
            if (titleContent != null)
                subTlv.put(0x8D, new HuaweiTLV()
                        .put(0x0E, (byte) 0x03)
                        .put(0x0F, titleEncoding)
                        .put(0x10, titleContent)
                );

            if (senderContent != null)
                subTlv.put(0x8D, new HuaweiTLV()
                        .put(0x0E, (byte) 0x02)
                        .put(0x0F, senderEncoding)
                        .put(0x10, senderContent)
                );

            if (bodyContent != null)
                subTlv.put(0x8D, new HuaweiTLV()
                        .put(0x0E, (byte) 0x01)
                        .put(0x0F, bodyEncoding)
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
                if (bValue.capacity() == 2)
                    bBuffer.putShort(position, bValue.getShort());
                bBuffer.put(position, (byte)0x00);
                bBuffer.put(bValue.get());
            }

            @Override
            public void parseTlv() throws ParseException {
                this.constraints = ByteBuffer.allocate(14);
                List<HuaweiTLV> subContainers = this.tlv
                        .getObject(0x81)
                        .getObject(0x82)
                        .getObjects(0x90);
                for (HuaweiTLV subContainer : subContainers) {
                    HuaweiTLV subSubContainer = subContainer.getObject(0x91);
                    if (subSubContainer.getByte(0x12) == 0x01)
                        putByteBuffer(constraints, NotificationConstraintsType.contentLength,subSubContainer.getBytes(0x14));
                    if (subSubContainer.getByte(0x12) == 0x05) {
                        constraints.put(NotificationConstraintsType.yellowPagesSupport,(byte)0x01);
                        constraints.put(NotificationConstraintsType.yellowPagesFormat,subSubContainer.getByte(0x13));
                        putByteBuffer(constraints, NotificationConstraintsType.yellowPagesLength,subSubContainer.getBytes(0x14));
                    }
                    if (subSubContainer.getByte(0x12) == 0x06) {
                        constraints.put(NotificationConstraintsType.contentSignSupport,(byte)0x01);
                        constraints.put(NotificationConstraintsType.contentSignFormat,subSubContainer.getByte(0x13));
                        putByteBuffer(constraints, NotificationConstraintsType.contentSignLength,subSubContainer.getBytes(0x14));
                    }
                    if (subSubContainer.getByte(0x12) == 0x07 ) {
                        constraints.put(NotificationConstraintsType.incomingNumberSupport,(byte)0x01);
                        constraints.put(NotificationConstraintsType.incomingNumberFormat,subSubContainer.getByte(0x13));
                        putByteBuffer(constraints, NotificationConstraintsType.incomingNumberLength,subSubContainer.getBytes(0x14));
                    }
                }
                constraints.rewind();
            }
        }
    }

    public static class NotificationConstraintsType {
        // TODO: enum?

        public static final byte contentLength = 0x00;
        public static final byte yellowPagesSupport = 0x02;
        public static final byte yellowPagesFormat = 0x03;
        public static final byte yellowPagesLength = 0x04;
        public static final byte contentSignSupport = 0x06;
        public static final byte contentSignFormat = 0x07;
        public static final byte contentSignLength = 0x08;
        public static final byte incomingNumberSupport = 0x0A;
        public static final byte incomingNumberFormat = 0x0B;
        public static final byte incomingNumberLength = 0x0C;
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

    public static class TextEncoding {
        // TODO: enum?

        public static final byte unknown = 0x01;
        public static final byte standard = 0x02;
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
