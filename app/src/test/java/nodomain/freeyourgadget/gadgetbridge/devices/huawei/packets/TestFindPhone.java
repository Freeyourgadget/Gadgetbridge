/*  Copyright (C) 2022 Martin.JM

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
import org.junit.Test;

import java.lang.reflect.Field;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class TestFindPhone {

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
    public void testStartFindPhone() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x0b, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0xcc, (byte) 0xf1};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x01, true);

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x0b, packet.serviceId);
        Assert.assertEquals(0x01, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof FindPhone.Response);
        Assert.assertTrue(((FindPhone.Response) packet).start);
    }

    @Test
    public void testStopFindPhone() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x0b, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0xdc, (byte) 0xd0};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x01, false);

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x0b, packet.serviceId);
        Assert.assertEquals(0x01, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof FindPhone.Response);
        Assert.assertFalse(((FindPhone.Response) packet).start);
    }

    @Test
    public void testFindPhoneMissingTag() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x0b, (byte) 0x01, (byte) 0xa1, (byte) 0x91};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV();

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x0b, packet.serviceId);
        Assert.assertEquals(0x01, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof FindPhone.Response);
        Assert.assertFalse(((FindPhone.Response) packet).start);
    }
}
