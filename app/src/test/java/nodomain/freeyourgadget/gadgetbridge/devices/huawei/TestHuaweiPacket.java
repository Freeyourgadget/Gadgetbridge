/*  Copyright (C) 2023 Martin.JM

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

package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FindPhone;

public class TestHuaweiPacket {

    HuaweiPacket.ParamsProvider paramsProvider = new HuaweiPacket.ParamsProvider() {

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
            return false;
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

    HuaweiPacket.ParamsProvider paramsProviderEncrypt = new HuaweiPacket.ParamsProvider() {

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

    HuaweiPacket.ParamsProvider paramsProviderSmallSlice = new HuaweiPacket.ParamsProvider() {

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
            return false;
        }

        @Override
        public int getMtu() {
            return 0;
        }

        @Override
        public int getSliceSize() {
            return 0x10;
        }
    };

    @Test
    public void testEmptyPacketParse() {
        byte[] input = {};

        HuaweiPacket packet = new HuaweiPacket(paramsProvider);

        try {
            packet.parse(input);
        } catch (HuaweiPacket.LengthMismatchException e) {
            // Pass
        } catch (HuaweiPacket.ParseException e) {
            Assert.fail();
        }
    }

    @Test
    public void testUnknownUnencryptedPacketParse() throws HuaweiPacket.ParseException {
        byte[] input = {0x5a, 0x00, 0x07, 0x00, 0x7f, 0x7f, 0x01, 0x02, 0x03, 0x04, 0x40, (byte) 0xb6};

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, (short) 0x0304);

        HuaweiPacket packet = new HuaweiPacket(paramsProvider);
        packet = packet.parse(input);

        Assert.assertEquals(HuaweiPacket.class, packet.getClass());
        Assert.assertEquals(0x7f, packet.serviceId);
        Assert.assertEquals(0x7f, packet.commandId);
        Assert.assertFalse(packet.isEncrypted);
        Assert.assertTrue(packet.complete);
        Assert.assertEquals(expectedTlv, packet.getTlv());
    }

    @Test
    public void testUnknownEncryptedPacketParse() throws HuaweiPacket.ParseException {
        byte[] input = {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x7f, (byte) 0x7f, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x9e, (byte) 0x40, (byte) 0xe1, (byte) 0xea, (byte) 0x15, (byte) 0xf6, (byte) 0x50, (byte) 0x80, (byte) 0x8c, (byte) 0x45, (byte) 0x19, (byte) 0xd5, (byte) 0x2a, (byte) 0xbb, (byte) 0x29, (byte) 0xb8, (byte) 0xD5, (byte) 0x24};

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, (short) 0x0304);

        HuaweiPacket packet = new HuaweiPacket(paramsProvider);
        packet = packet.parse(input);

        Assert.assertEquals(HuaweiPacket.class, packet.getClass());
        Assert.assertEquals(0x7f, packet.serviceId);
        Assert.assertEquals(0x7f, packet.commandId);
        Assert.assertTrue(packet.isEncrypted);
        Assert.assertTrue(packet.complete);
        Assert.assertEquals(expectedTlv, packet.getTlv());
    }

    @Test
    public void testKnownEncryptedPacketParse() throws HuaweiPacket.ParseException {
        byte[] input = {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x0b, (byte) 0x01, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x28, (byte) 0x00, (byte) 0x99, (byte) 0x6f, (byte) 0x2a, (byte) 0xcb, (byte) 0x62, (byte) 0x3a, (byte) 0xe6, (byte) 0x54, (byte) 0x28, (byte) 0x54, (byte) 0xf8, (byte) 0xab, (byte) 0x54, (byte) 0x83, (byte) 0xf4, (byte) 0xf4};

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, false);

        HuaweiPacket packet = new HuaweiPacket(paramsProvider);
        packet = packet.parse(input);

        Assert.assertEquals(FindPhone.Response.class, packet.getClass());
        Assert.assertEquals(0x0b, packet.serviceId);
        Assert.assertEquals(0x01, packet.commandId);
        Assert.assertTrue(packet.isEncrypted);
        Assert.assertTrue(packet.complete);
        Assert.assertEquals(expectedTlv, packet.getTlv());
    }

    @Test
    public void testUnencryptedUnslicedSerialize() throws HuaweiPacket.CryptoException {
        byte serviceId = 0x01;
        byte commandId = 0x02;
        HuaweiTLV tlv = new HuaweiTLV()
                .put(0x03, 0x05060708);

        byte[] expected = {(byte) 0x5a, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0xA4, (byte) 0xF0};

        HuaweiPacket packet = new HuaweiPacket(paramsProvider);
        packet.isSliced = false;
        packet.isEncrypted = false;
        packet.serviceId = serviceId;
        packet.commandId = commandId;
        packet.setTlv(tlv);

        List<byte[]> output = packet.serialize();

        Assert.assertEquals(1, output.size());
        Assert.assertArrayEquals(expected, output.get(0));
    }

    @Test
    public void testEncryptedUnslicedSerialize() throws HuaweiPacket.CryptoException {
        byte serviceId = 0x01;
        byte commandId = 0x02;
        HuaweiTLV tlv = new HuaweiTLV()
                .put(0x03, 0x05060708);

        byte[] expected = {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x3b, (byte) 0x89, (byte) 0xfc, (byte) 0x79, (byte) 0xd9, (byte) 0x05, (byte) 0x5e, (byte) 0xed, (byte) 0x52, (byte) 0x35, (byte) 0xfe, (byte) 0x16, (byte) 0xa0, (byte) 0x8a, (byte) 0x4d, (byte) 0x53, (byte) 0x93, (byte) 0xc7};

        HuaweiPacket packet = new HuaweiPacket(paramsProviderEncrypt);
        packet.isSliced = false;
        packet.isEncrypted = true;
        packet.serviceId = serviceId;
        packet.commandId = commandId;
        packet.setTlv(tlv);

        List<byte[]> output = packet.serialize();

        Assert.assertEquals(1, output.size());
        Assert.assertArrayEquals(expected, output.get(0));
    }

    @Test
    public void testUnencryptedSlicedSerialize() throws HuaweiPacket.CryptoException {
        byte serviceId = 0x01;
        byte commandId = 0x02;
        HuaweiTLV tlv = new HuaweiTLV()
                .put(0x01, 0x00)
                .put(0x02, 0x00)
                .put(0x03, 0x00)
                .put(0x04, 0x00);

        byte[] expected1 = {(byte) 0x5a, (byte) 0x00, (byte) 0x0b, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0xcc, (byte) 0x98};
        byte[] expected2 = {(byte) 0x5a, (byte) 0x00, (byte) 0x0b, (byte) 0x02, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0xfa, (byte) 0xd3};
        byte[] expected3 = {(byte) 0x5a, (byte) 0x00, (byte) 0x0a, (byte) 0x03, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x37, (byte) 0xca};

        HuaweiPacket packet = new HuaweiPacket(paramsProviderSmallSlice);
        packet.isSliced = true;
        packet.isEncrypted = false;
        packet.serviceId = serviceId;
        packet.commandId = commandId;
        packet.setTlv(tlv);

        List<byte[]> output = packet.serialize();

        Assert.assertEquals(3, output.size());
        Assert.assertArrayEquals(expected1, output.get(0));
        Assert.assertArrayEquals(expected2, output.get(1));
        Assert.assertArrayEquals(expected3, output.get(2));
    }
}
