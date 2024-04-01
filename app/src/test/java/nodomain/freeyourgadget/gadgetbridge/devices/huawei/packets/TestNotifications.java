/*  Copyright (C) 2022-2023 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class TestNotifications {

    HuaweiPacket.ParamsProvider secretsProvider = new HuaweiPacket.ParamsProvider() {
        @Override
        public byte getDeviceSupportType() {
            return 0;
        }

        @Override
        public byte[] getSecretKey() {
            return new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }

        @Override
        public byte[] getIv() {
            return new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }

        @Override
        public boolean areTransactionsCrypted() {
            return true;
        }

        @Override
        public int getMtu() {
            return 0;
        }

        @Override
        public int getSliceSize() {
            return 0xF4;
        }
    };

    @Test
    public void testNotificationActionRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        short notificationId = 0x01;
        byte notificationType = 0x02;
        byte encoding = 0x02;
        String titleContent = "Title";
        String senderContent = "Sender";
        String bodyContent = "Body";
        String sourceAppId = "SourceApp";

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, notificationId)
                .put(0x02, notificationType)
                .put(0x03, true)
                .put(0x84, new HuaweiTLV()
                        .put(0x8C, new HuaweiTLV()
                                .put(0x8D, new HuaweiTLV()
                                        .put(0x0E, (byte) 0x03)
                                        .put(0x0F, encoding)
                                        .put(0x10, titleContent)
                                )
                                .put(0x8D, new HuaweiTLV()
                                        .put(0x0E, (byte) 0x02)
                                        .put(0x0F, encoding)
                                        .put(0x10, senderContent)
                                )
                                .put(0x8D, new HuaweiTLV()
                                        .put(0x0E, (byte) 0x01)
                                        .put(0x0F, encoding)
                                        .put(0x10, bodyContent)
                                )
                        )
                )
                .put(0x11, sourceAppId);

        Notifications.NotificationActionRequest request = new Notifications.NotificationActionRequest(
                secretsProvider,
                notificationId,
                notificationType,
                encoding,
                titleContent,
                senderContent,
                bodyContent,
                sourceAppId
        );

        Assert.assertEquals(0x02, request.serviceId);
        Assert.assertEquals(0x01, request.commandId);
        Assert.assertTrue(request.complete);
        Assert.assertEquals(expectedTlv, tlvField.get(request));

        // Only check that this doesn't error
        request.serialize();
    }

    @Test
    public void testSetNotificationRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlvTrue = new HuaweiTLV()
                .put(0x81, new HuaweiTLV()
                        .put(0x02, true)
                        .put(0x03, true)
                );

        HuaweiTLV expectedTlvFalse = new HuaweiTLV()
                .put(0x81, new HuaweiTLV()
                        .put(0x02, false)
                        .put(0x03, false)
                );

        byte[] expectedOutputTrue = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x02, (byte) 0x04, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xd9, (byte) 0xc4, (byte) 0xaa, (byte) 0x7d, (byte) 0xa3, (byte) 0x5c, (byte) 0x42, (byte) 0xab, (byte) 0x2d, (byte) 0xc2, (byte) 0xe7, (byte) 0x73, (byte) 0xc0, (byte) 0x4c, (byte) 0x97, (byte) 0x5a, (byte) 0x41, (byte) 0x23};
        byte[] expectedOutputFalse = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x02, (byte) 0x04, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xeb, (byte) 0x1f, (byte) 0x20, (byte) 0x0a, (byte) 0x7d, (byte) 0xe2, (byte) 0x25, (byte) 0x45, (byte) 0x01, (byte) 0x5b, (byte) 0xe8, (byte) 0x24, (byte) 0xe3, (byte) 0x7e, (byte) 0x1d, (byte) 0x9c, (byte) 0x47, (byte) 0x31};

        Notifications.NotificationStateRequest requestTrue = new Notifications.NotificationStateRequest(secretsProvider, true);
        Notifications.NotificationStateRequest requestFalse = new Notifications.NotificationStateRequest(secretsProvider, false);

        Assert.assertEquals(0x02, requestTrue.serviceId);
        Assert.assertEquals(0x04, requestTrue.commandId);
        Assert.assertTrue(requestTrue.complete);
        Assert.assertEquals(expectedTlvTrue, tlvField.get(requestTrue));
        List<byte[]> outTrue = requestTrue.serialize();
        Assert.assertEquals(1, outTrue.size());
        Assert.assertArrayEquals(expectedOutputTrue, outTrue.get(0));

        Assert.assertEquals(0x02, requestFalse.serviceId);
        Assert.assertEquals(0x04, requestFalse.commandId);
        Assert.assertTrue(requestFalse.complete);
        Assert.assertEquals(expectedTlvFalse, tlvField.get(requestFalse));
        List<byte[]> outFalse = requestFalse.serialize();
        Assert.assertEquals(1, outFalse.size());
        Assert.assertArrayEquals(expectedOutputFalse, outFalse.get(0));
    }

    @Test
    @Ignore("Broken since https://codeberg.org/psolyca/Gadgetbridge/commit/5b0736b7518aa5c998ac13207fff66286393965b")
    public void testSetWearMessagePushRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlvTrue = new HuaweiTLV()
                .put(0x01, true);

        HuaweiTLV expectedTlvFalse = new HuaweiTLV()
                .put(0x01, false);

        byte[] expectedOutputTrue = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x02, (byte) 0x08, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xcd, (byte) 0x97, (byte) 0x7e, (byte) 0x01, (byte) 0x48, (byte) 0x34, (byte) 0x2a, (byte) 0x48, (byte) 0x58, (byte) 0x0d, (byte) 0x30, (byte) 0xc7, (byte) 0xbc, (byte) 0x2e, (byte) 0x40, (byte) 0xd4, (byte) 0x29, (byte) 0xe0};
        byte[] expectedOutputFalse = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x02, (byte) 0x08, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x28, (byte) 0x00, (byte) 0x99, (byte) 0x6f, (byte) 0x2a, (byte) 0xcb, (byte) 0x62, (byte) 0x3a, (byte) 0xe6, (byte) 0x54, (byte) 0x28, (byte) 0x54, (byte) 0xf8, (byte) 0xab, (byte) 0x54, (byte) 0x83, (byte) 0x30, (byte) 0xd2};

        Notifications.WearMessagePushRequest requestTrue = new Notifications.WearMessagePushRequest(secretsProvider, true);
        Notifications.WearMessagePushRequest requestFalse = new Notifications.WearMessagePushRequest(secretsProvider, false);

        Assert.assertEquals(0x02, requestTrue.serviceId);
        Assert.assertEquals(0x08, requestTrue.commandId);
        Assert.assertTrue(requestTrue.complete);
        Assert.assertEquals(expectedTlvTrue, tlvField.get(requestTrue));
        List<byte[]> outTrue = requestTrue.serialize();
        Assert.assertEquals(1, outTrue.size());
        Assert.assertArrayEquals(expectedOutputTrue, outTrue.get(0));

        Assert.assertEquals(0x02, requestFalse.serviceId);
        Assert.assertEquals(0x08, requestFalse.commandId);
        Assert.assertTrue(requestFalse.complete);
        Assert.assertEquals(expectedTlvFalse, tlvField.get(requestFalse));
        List<byte[]> outFalse = requestFalse.serialize();
        Assert.assertEquals(1, outFalse.size());
        Assert.assertArrayEquals(expectedOutputFalse, outFalse.get(0));
    }
}
