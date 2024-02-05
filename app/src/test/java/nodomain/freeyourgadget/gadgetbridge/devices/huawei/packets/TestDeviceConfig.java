/*  Copyright (C) 2022 Gaignon Damien, Martin.JM
    Copyright (C) 2022-2023 MartinJM

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

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCoordinatorSupplier;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class TestDeviceConfig {

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
    public void testLinkParamsRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01)
                .put(0x02)
                .put(0x03)
                .put(0x04);

        byte[] serialized = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x0b, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0xf1, (byte) 0x3b};
        DeviceConfig.LinkParams.Request request = new DeviceConfig.LinkParams.Request(
                secretsProvider, HuaweiCoordinatorSupplier.HuaweiDeviceType.BLE
        );

        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x01, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(serialized, out.get(0));
    }

    @Test
    public void testLinkParamsResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawLinkParams = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x1b, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x03, (byte) 0x02, (byte) 0x00, (byte) 0x14, (byte) 0x05, (byte) 0x12, (byte) 0x00, (byte) 0x01, (byte) 0x6a, (byte) 0xa2, (byte) 0x96, (byte) 0xe3, (byte) 0x76, (byte) 0x41, (byte) 0xb1, (byte) 0x0c, (byte) 0xf8, (byte) 0xaa, (byte) 0xf7, (byte) 0x47, (byte) 0x05, (byte) 0x5d, (byte) 0x0a, (byte) 0xa3, (byte) 0xe8, (byte) 0x9f};

        byte[] expectedServerNonceWithAuth = new byte[] {(byte) 0x00, (byte) 0x01, (byte) 0x6A, (byte) 0xA2, (byte) 0x96, (byte) 0xE3, (byte) 0x76, (byte) 0x41, (byte) 0xB1, (byte) 0x0C, (byte) 0xF8, (byte) 0xAA, (byte) 0xF7, (byte) 0x47, (byte) 0x05, (byte) 0x5D, (byte) 0x0A, (byte) 0xA3};
        byte[] expectedServerNonce = new byte[expectedServerNonceWithAuth.length - 2];
        System.arraycopy(expectedServerNonceWithAuth, 2, expectedServerNonce, 0, expectedServerNonceWithAuth.length - 2);

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x03, new byte[] {0x00, 0x14})
                .put(0x05, expectedServerNonceWithAuth);

        HuaweiPacket packetLinkParams = new HuaweiPacket(secretsProvider).parse(rawLinkParams);
        packetLinkParams.parseTlv();

        Assert.assertEquals(0x01, packetLinkParams.serviceId);
        Assert.assertEquals(0x01, packetLinkParams.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packetLinkParams));
        Assert.assertTrue(packetLinkParams.complete);
        Assert.assertTrue(packetLinkParams instanceof DeviceConfig.LinkParams.Response);
        Assert.assertEquals(0x01, ((DeviceConfig.LinkParams.Response) packetLinkParams).authVersion);
        Assert.assertArrayEquals(expectedServerNonce, ((DeviceConfig.LinkParams.Response) packetLinkParams).serverNonce);
    }

    @Test(expected=HuaweiPacket.ParseException.class)
    public void testLinkParamsResponseException() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawLinkParams = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x17, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x12, (byte) 0x00, (byte) 0x01, (byte) 0x6a, (byte) 0xa2, (byte) 0x96, (byte) 0xe3, (byte) 0x76, (byte) 0x41, (byte) 0xb1, (byte) 0x0c, (byte) 0xf8, (byte) 0xaa, (byte) 0xf7, (byte) 0x47, (byte) 0x05, (byte) 0x5d, (byte) 0x0a, (byte) 0xa3, (byte) 0xdd, (byte) 0x41};

        HuaweiPacket packetLinkParams = new HuaweiPacket(secretsProvider).parse(rawLinkParams);
        packetLinkParams.parseTlv();
    }

    @Test
    public void testSupportedServicesRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        byte[] allSupportedServices = new byte[] {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f, (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x1b, (byte) 0x1c, (byte) 0x1d, (byte) 0x1e, (byte) 0x1f, (byte) 0x20, (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24, (byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2a, (byte) 0x2b, (byte) 0x2c, (byte) 0x2d, (byte) 0x2e, (byte) 0x2f, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, allSupportedServices);

        byte[] serialized = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x5a, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x40, (byte) 0x14, (byte) 0xb1, (byte) 0x75, (byte) 0x7d, (byte) 0xc0, (byte) 0xa5, (byte) 0x32, (byte) 0xeb, (byte) 0xc1, (byte) 0x20, (byte) 0x7b, (byte) 0xb8, (byte) 0x59, (byte) 0xdb, (byte) 0xdb, (byte) 0xfe, (byte) 0x5b, (byte) 0x01, (byte) 0x0a, (byte) 0x7d, (byte) 0xb7, (byte) 0x76, (byte) 0xfc, (byte) 0xcc, (byte) 0x5f, (byte) 0x22, (byte) 0xff, (byte) 0x13, (byte) 0xcb, (byte) 0xbb, (byte) 0x4f, (byte) 0xe2, (byte) 0xcd, (byte) 0x6e, (byte) 0x4b, (byte) 0xd7, (byte) 0x7c, (byte) 0x05, (byte) 0x24, (byte) 0x85, (byte) 0x65, (byte) 0x5f, (byte) 0x95, (byte) 0x32, (byte) 0xb4, (byte) 0x5e, (byte) 0x16, (byte) 0xef, (byte) 0xad, (byte) 0x62, (byte) 0x38, (byte) 0xd5, (byte) 0x88, (byte) 0x63, (byte) 0xa4, (byte) 0xb0, (byte) 0x29, (byte) 0xbb, (byte) 0x90, (byte) 0x66, (byte) 0x8c, (byte) 0x3f, (byte) 0x58, (byte) 0x69, (byte) 0x40, (byte) 0x22};
        DeviceConfig.SupportedServices.Request request = new DeviceConfig.SupportedServices.Request(
                secretsProvider,
                allSupportedServices
        );

        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x02, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(serialized, out.get(0));
    }

    @Test
    public void testSupportedServicesResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawSupportedServices = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x5a, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x40, (byte) 0xC8, (byte) 0x9F, (byte) 0x1E, (byte) 0x2F, (byte) 0xE8, (byte) 0x31, (byte) 0xC8, (byte) 0x1E, (byte) 0x92, (byte) 0xB0, (byte) 0xE8, (byte) 0x9E, (byte) 0xC7, (byte) 0x2E, (byte) 0x76, (byte) 0xD7, (byte) 0x6C, (byte) 0x64, (byte) 0x22, (byte) 0x5A, (byte) 0x6C, (byte) 0xF9, (byte) 0xAF, (byte) 0xFB, (byte) 0x8E, (byte) 0x98, (byte) 0x74, (byte) 0xB6, (byte) 0xF9, (byte) 0x84, (byte) 0x3C, (byte) 0x1E, (byte) 0x3D, (byte) 0xCB, (byte) 0x7C, (byte) 0x23, (byte) 0x4F, (byte) 0x7B, (byte) 0x34, (byte) 0x0C, (byte) 0x49, (byte) 0xBD, (byte) 0x80, (byte) 0x94, (byte) 0x67, (byte) 0x1B, (byte) 0x5C, (byte) 0x64, (byte) 0x6B, (byte) 0xA4, (byte) 0xB9, (byte) 0xEC, (byte) 0xA7, (byte) 0x97, (byte) 0x95, (byte) 0x6F, (byte) 0x44, (byte) 0x13, (byte) 0x66, (byte) 0x7C, (byte) 0xF5, (byte) 0x9F, (byte) 0x05, (byte) 0x72, (byte) 0xc9, (byte) 0xe9};
        byte[] expectedSupportedServices = new byte[] {(byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x02, expectedSupportedServices);

        HuaweiPacket packetSupportedServices = new HuaweiPacket(secretsProvider).parse(rawSupportedServices);
        packetSupportedServices.parseTlv();

        Assert.assertEquals(0x01, packetSupportedServices.serviceId);
        Assert.assertEquals(0x02, packetSupportedServices.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packetSupportedServices));
        Assert.assertTrue(packetSupportedServices.complete);
        Assert.assertTrue(packetSupportedServices instanceof DeviceConfig.SupportedServices.Response);
        Assert.assertArrayEquals(expectedSupportedServices, ((DeviceConfig.SupportedServices.Response) packetSupportedServices).supportedServices);
    }

    @Test(expected=HuaweiPacket.ParseException.class)
    public void testSupportedServicesResponseException() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawSupportedServices = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x5A, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x40, (byte) 0x28, (byte) 0xB0, (byte) 0x4D, (byte) 0x00, (byte) 0xCF, (byte) 0xE1, (byte) 0xD1, (byte) 0x7A, (byte) 0x5D, (byte) 0xE7, (byte) 0x61, (byte) 0x0E, (byte) 0xE1, (byte) 0xA5, (byte) 0xE8, (byte) 0xF9, (byte) 0x6D, (byte) 0x2D, (byte) 0x32, (byte) 0xB3, (byte) 0xC3, (byte) 0x7C, (byte) 0x07, (byte) 0xBC, (byte) 0x11, (byte) 0x03, (byte) 0x8A, (byte) 0x66, (byte) 0x8C, (byte) 0x47, (byte) 0x94, (byte) 0x86, (byte) 0x8C, (byte) 0x0D, (byte) 0xC6, (byte) 0xBC, (byte) 0xDF, (byte) 0xB3, (byte) 0x00, (byte) 0xFB, (byte) 0x68, (byte) 0x11, (byte) 0xC1, (byte) 0xB3, (byte) 0x66, (byte) 0x6D, (byte) 0x85, (byte) 0x6F, (byte) 0xF0, (byte) 0xA9, (byte) 0xD0, (byte) 0x49, (byte) 0xDF, (byte) 0xF5, (byte) 0x82, (byte) 0x01, (byte) 0x9F, (byte) 0xE4, (byte) 0x60, (byte) 0x36, (byte) 0x81, (byte) 0xAA, (byte) 0x31, (byte) 0xA1, (byte) 0x39, (byte) 0xD6};

        HuaweiPacket packetSupportedServices = new HuaweiPacket(secretsProvider).parse(rawSupportedServices);
        packetSupportedServices.parseTlv();
    }

    @Test
    public void testSupportedCommandsRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        byte service1 = (byte) 0x01;
        byte[] commands1 = new byte[]{(byte) 0x04, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0D, (byte) 0x0E, (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x1B, (byte) 0x1A, (byte) 0x1D, (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24, (byte) 0x29, (byte) 0x2A, (byte) 0x2B, (byte) 0x32, (byte) 0x2E, (byte) 0x31, (byte) 0x30, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x2F};
        byte service2 = (byte) 0x02;
        byte[] commands2 = new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08};
        byte service3 = (byte) 0x04;
        byte[] commands3 = new byte[]{(byte) 0x01};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x81, new HuaweiTLV()
                    .put(0x02, service1)
                    .put(0x03, commands1)
                    .put(0x02, service2)
                    .put(0x03, commands2)
                    .put(0x02, service3)
                    .put(0x03, commands3)
                );

        byte[] expectedSerialized = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x5A, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x40, (byte) 0x81, (byte) 0xED, (byte) 0x07, (byte) 0xF6, (byte) 0x51, (byte) 0xA3, (byte) 0x19, (byte) 0xBD, (byte) 0xCE, (byte) 0x35, (byte) 0x13, (byte) 0x23, (byte) 0x1E, (byte) 0xFC, (byte) 0x1A, (byte) 0x51, (byte) 0x92, (byte) 0xBB, (byte) 0x43, (byte) 0xC5, (byte) 0xF5, (byte) 0xD9, (byte) 0x4E, (byte) 0xCC, (byte) 0x2F, (byte) 0xE0, (byte) 0xDB, (byte) 0xB1, (byte) 0x5E, (byte) 0x78, (byte) 0x66, (byte) 0x69, (byte) 0x61, (byte) 0x85, (byte) 0x46, (byte) 0xB2, (byte) 0x50, (byte) 0xEC, (byte) 0xB5, (byte) 0x3F, (byte) 0x74, (byte) 0x68, (byte) 0x47, (byte) 0x03, (byte) 0x87, (byte) 0xC1, (byte) 0xB3, (byte) 0x53, (byte) 0x7B, (byte) 0x53, (byte) 0xDB, (byte) 0xE8, (byte) 0x5E, (byte) 0x82, (byte) 0x56, (byte) 0xFD, (byte) 0x16, (byte) 0x66, (byte) 0x03, (byte) 0xB2, (byte) 0x56, (byte) 0xA3, (byte) 0x14, (byte) 0x70, (byte) 0x38, (byte) 0x3E};
        DeviceConfig.SupportedCommands.Request request = new DeviceConfig.SupportedCommands.Request(
                secretsProvider
        );
        request.addCommandsForService(service1, commands1);
        request.addCommandsForService(service2, commands2);
        request.addCommandsForService(service3, commands3);

        List<byte[]> out = request.serialize();

        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x03, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expectedSerialized, out.get(0));
    }

    @Test
    public void testSupportedCommandsResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawSupportedCommands = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x5A, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x40, (byte) 0x8B, (byte) 0x86, (byte) 0x29, (byte) 0xE3, (byte) 0x90, (byte) 0x25, (byte) 0x4E, (byte) 0x14, (byte) 0xE1, (byte) 0xDD, (byte) 0x96, (byte) 0x63, (byte) 0x66, (byte) 0xB8, (byte) 0x1E, (byte) 0x4A, (byte) 0xC1, (byte) 0xA7, (byte) 0x49, (byte) 0xB0, (byte) 0x9F, (byte) 0x21, (byte) 0x7C, (byte) 0xE8, (byte) 0x2C, (byte) 0x72, (byte) 0x93, (byte) 0x9F, (byte) 0xAC, (byte) 0x37, (byte) 0x3B, (byte) 0x4D, (byte) 0x1A, (byte) 0xCB, (byte) 0xC2, (byte) 0xFF, (byte) 0x64, (byte) 0xE5, (byte) 0xF0, (byte) 0x3E, (byte) 0x5B, (byte) 0xFF, (byte) 0xB1, (byte) 0x9C, (byte) 0x59, (byte) 0xB2, (byte) 0xF1, (byte) 0xD6, (byte) 0x4B, (byte) 0x2B, (byte) 0x99, (byte) 0xFB, (byte) 0xEA, (byte) 0x29, (byte) 0x66, (byte) 0xD3, (byte) 0x90, (byte) 0x0B, (byte) 0xC9, (byte) 0xF0, (byte) 0xB4, (byte) 0x9B, (byte) 0x3B, (byte) 0x3E, (byte) 0x50, (byte) 0xFA};

        List<DeviceConfig.SupportedCommands.Response.CommandsList> expectedSupportedCommandsList = new ArrayList<>();
        DeviceConfig.SupportedCommands.Response.CommandsList commandsList = new DeviceConfig.SupportedCommands.Response.CommandsList();
        commandsList.service = 1;
        commandsList.commands = new byte[] {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0D, (byte) 0x0F, (byte) 0x10, (byte) 0x11};
        expectedSupportedCommandsList.add(commandsList);
        commandsList.service = 2;
        commandsList.commands = new byte[] {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x06};
        expectedSupportedCommandsList.add(commandsList);
        commandsList.service = 4;
        commandsList.commands = new byte[] {(byte) 0x01};
        expectedSupportedCommandsList.add(commandsList);

        byte[] expectedSupportedCommands = new byte[] {(byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x04, (byte) 0x1E, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x06, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x04, (byte) 0x04, (byte) 0x01, (byte) 0x01};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x81, expectedSupportedCommands);

        HuaweiPacket packetSupportedCommands = new HuaweiPacket(secretsProvider).parse(rawSupportedCommands);
        packetSupportedCommands.parseTlv();

        Assert.assertEquals(0x01, packetSupportedCommands.serviceId);
        Assert.assertEquals(0x03, packetSupportedCommands.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packetSupportedCommands));
        Assert.assertTrue(packetSupportedCommands.complete);
        Assert.assertTrue(packetSupportedCommands instanceof DeviceConfig.SupportedCommands.Response);
        Assert.assertEquals(expectedSupportedCommandsList.size(), ((DeviceConfig.SupportedCommands.Response) packetSupportedCommands).commandsLists.size());
    }

    @Test(expected=HuaweiPacket.ParseException.class)
    public void testSupportedCommandsResponseException02() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawSupportedCommands = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x4A, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x30, (byte) 0x57, (byte) 0xC7, (byte) 0xF7, (byte) 0x47, (byte) 0x0E, (byte) 0xA9, (byte) 0xA2, (byte) 0x9E, (byte) 0xF3, (byte) 0xB0, (byte) 0xBD, (byte) 0x02, (byte) 0xE0, (byte) 0x79, (byte) 0x3C, (byte) 0x12, (byte) 0xE6, (byte) 0x58, (byte) 0xDA, (byte) 0xF7, (byte) 0x0B, (byte) 0xC3, (byte) 0x93, (byte) 0x8D, (byte) 0x37, (byte) 0x2E, (byte) 0xA9, (byte) 0xB8, (byte) 0xF8, (byte) 0xF7, (byte) 0x97, (byte) 0xF3, (byte) 0x22, (byte) 0x08, (byte) 0xDF, (byte) 0xAD, (byte) 0x2B, (byte) 0x62, (byte) 0x33, (byte) 0x11, (byte) 0x93, (byte) 0x66, (byte) 0xD1, (byte) 0xAE, (byte) 0xF3, (byte) 0x02, (byte) 0x18, (byte) 0x49, (byte) 0xD0, (byte) 0x40};

        HuaweiPacket packetSupportedCommands = new HuaweiPacket(secretsProvider).parse(rawSupportedCommands);
        packetSupportedCommands.parseTlv();
    }

    @Test(expected=HuaweiPacket.ParseException.class)
    public void testSupportedCommandsResponseException04() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawSupportedCommands = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x2A, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x10, (byte) 0x25, (byte) 0x26, (byte) 0xE0, (byte) 0x79, (byte) 0x1D, (byte) 0x19, (byte) 0x82, (byte) 0xDB, (byte) 0x0A, (byte) 0x3A, (byte) 0x21, (byte) 0x6E, (byte) 0x70, (byte) 0x52, (byte) 0xAB, (byte) 0xF3, (byte) 0x14, (byte) 0xB4};

        HuaweiPacket packetSupportedCommands = new HuaweiPacket(secretsProvider).parse(rawSupportedCommands);
        packetSupportedCommands.parseTlv();
    }

    @Test
    public void testDateFormatRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        byte dateformat = (byte) 0x02;
        byte timeFormat = (byte) 0x02;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x81, new HuaweiTLV()
                    .put(0x02, dateformat)
                    .put(0x03, timeFormat)
                );

        byte[] serialized = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x01, (byte) 0x04, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x63, (byte) 0xe0, (byte) 0xf1, (byte) 0xbf, (byte) 0x40, (byte) 0xab, (byte) 0x09, (byte) 0x63, (byte) 0x51, (byte) 0x7c, (byte) 0xa7, (byte) 0x8c, (byte) 0x2e, (byte) 0xd9, (byte) 0x6a, (byte) 0x6c, (byte) 0xdc, (byte) 0xe9};
        DeviceConfig.DateFormat.Request request = new DeviceConfig.DateFormat.Request (
                secretsProvider,
                dateformat,
                timeFormat
        );

        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x04, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(serialized, out.get(0));
    }

    @Test
    public void testTimeRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        int timestamp = 1633987331;
        short zoneOffset = (short) 512;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000L);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+2"));

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, timestamp)
                .put(0x02, zoneOffset);

        byte[] serialized = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x2A, (byte) 0x00, (byte) 0x01, (byte) 0x05, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x10, (byte) 0xED, (byte) 0x67, (byte) 0x61, (byte) 0x8A, (byte) 0x8E, (byte) 0x44, (byte) 0x67, (byte) 0xB1, (byte) 0x2A, (byte) 0xB4, (byte) 0xFA, (byte) 0x86, (byte) 0x76, (byte) 0x17, (byte) 0x8C, (byte) 0x61, (byte) 0xFC, (byte) 0x99};
        DeviceConfig.TimeRequest request = new DeviceConfig.TimeRequest(secretsProvider, calendar);

        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x05, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(serialized, out.get(0));
    }

    @Test
    public void testProductInformationRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV();
        byte[] expectedTags = {0x01, 0x02, 0x07, 0x09, 0x0A, 0x11, 0x12, 0x16, 0x1A, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23};
        for (byte tag : expectedTags) {
            expectedTlv.put(tag);
        }

        // Outdated
        //byte[] serialized = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x01, (byte) 0x07, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x20, (byte) 0x10, (byte) 0x9B, (byte) 0x27, (byte) 0x5D, (byte) 0xB1, (byte) 0x3C, (byte) 0xFD, (byte) 0x40, (byte) 0x4B, (byte) 0xA8, (byte) 0xAC, (byte) 0xAF, (byte) 0x8A, (byte) 0xB6, (byte) 0xA5, (byte) 0x3D, (byte) 0x40, (byte) 0x30, (byte) 0x2C, (byte) 0x79, (byte) 0x98, (byte) 0x6D, (byte) 0xEC, (byte) 0xD1, (byte) 0x39, (byte) 0xE6, (byte) 0xFE, (byte) 0x5C, (byte) 0xE8, (byte) 0xB2, (byte) 0xF3, (byte) 0x9E, (byte) 0x3E, (byte) 0x1B};
        DeviceConfig.ProductInfo.Request request = new DeviceConfig.ProductInfo.Request (
                secretsProvider, HuaweiCoordinatorSupplier.HuaweiDeviceType.BLE
        );

        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x07, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        // Assert.assertArrayEquals(serialized, out.get(0));
    }

    @Test
    public void testProductInformationResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawProductInformation = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x4A, (byte) 0x00, (byte) 0x01, (byte) 0x07, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x30, (byte) 0x50, (byte) 0x75, (byte) 0xA5, (byte) 0x4F, (byte) 0x26, (byte) 0xF7, (byte) 0x74, (byte) 0x0B, (byte) 0xB2, (byte) 0xD8, (byte) 0x01, (byte) 0xBA, (byte) 0xDC, (byte) 0x7E, (byte) 0x40, (byte) 0x36, (byte) 0xD5, (byte) 0x6D, (byte) 0x4B, (byte) 0x7B, (byte) 0x8F, (byte) 0xC6, (byte) 0xFB, (byte) 0x48, (byte) 0xFC, (byte) 0x89, (byte) 0x54, (byte) 0xF8, (byte) 0xBB, (byte) 0xC0, (byte) 0x48, (byte) 0x9E, (byte) 0x34, (byte) 0x0E, (byte) 0xB1, (byte) 0x24, (byte) 0xD8, (byte) 0x89, (byte) 0x02, (byte) 0x7E, (byte) 0x6C, (byte) 0x3E, (byte) 0x81, (byte) 0x7D, (byte) 0x38, (byte) 0x0F, (byte) 0xD9, (byte) 0x2A, (byte) 0x98, (byte) 0xE3};
        String softwareVersion = "1.0.10.78";
        String productModel = "Crius";

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x03, new byte[] {(byte) 0x4E, (byte) 0x41})
                .put(0x07, new byte[] {(byte) 0x31, (byte) 0x2E, (byte) 0x30, (byte) 0x2E, (byte) 0x31, (byte) 0x30, (byte) 0x2E, (byte) 0x37, (byte) 0x38})
                .put(0x0A, new byte[] {(byte) 0x43, (byte) 0x72, (byte) 0x69, (byte) 0x75, (byte) 0x73, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        HuaweiPacket packetProductInformation = new HuaweiPacket(secretsProvider).parse(rawProductInformation);
        packetProductInformation.parseTlv();

        Assert.assertEquals(0x01, packetProductInformation.serviceId);
        Assert.assertEquals(0x07, packetProductInformation.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packetProductInformation));
        Assert.assertTrue(packetProductInformation.complete);
        Assert.assertTrue(packetProductInformation instanceof DeviceConfig.ProductInfo.Response);
        Assert.assertTrue(softwareVersion.equals(((DeviceConfig.ProductInfo.Response) packetProductInformation).softwareVersion));
        System.out.println(((DeviceConfig.ProductInfo.Response) packetProductInformation).productModel);
        System.out.println(((DeviceConfig.ProductInfo.Response) packetProductInformation).productModel.length());
        Assert.assertTrue(productModel.equals(((DeviceConfig.ProductInfo.Response) packetProductInformation).productModel));
    }

    @Test(expected=HuaweiPacket.ParseException.class)
    public void testProductInformationResponseException07() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawProductInformation = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x01, (byte) 0x07, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x20, (byte) 0xFE, (byte) 0x24, (byte) 0x4C, (byte) 0x68, (byte) 0xFC, (byte) 0x1D, (byte) 0xAD, (byte) 0x64, (byte) 0x77, (byte) 0xC9, (byte) 0xE9, (byte) 0x26, (byte) 0x8D, (byte) 0x3C, (byte) 0x3C, (byte) 0x8C, (byte) 0xB6, (byte) 0xA6, (byte) 0xF1, (byte) 0xBF, (byte) 0xAC, (byte) 0xB6, (byte) 0x7A, (byte) 0x75, (byte) 0xA3, (byte) 0xA9, (byte) 0x07, (byte) 0x5F, (byte) 0x39, (byte) 0x0F, (byte) 0x28, (byte) 0x61, (byte) 0x50, (byte) 0x61};

        HuaweiPacket packetProductInformation = new HuaweiPacket(secretsProvider).parse(rawProductInformation);
        packetProductInformation.parseTlv();
    }

    @Test(expected=HuaweiPacket.ParseException.class)
    public void testProductInformationResponseException0A() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawProductInformation = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x2A, (byte) 0x00, (byte) 0x01, (byte) 0x07, (byte) 0x7C, (byte) 0x01, (byte) 0x01, (byte) 0x7D, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E, (byte) 0x10, (byte) 0xB6, (byte) 0x67, (byte) 0xA5, (byte) 0x6A, (byte) 0x46, (byte) 0x0F, (byte) 0x08, (byte) 0x1E, (byte) 0xAC, (byte) 0x1E, (byte) 0x6B, (byte) 0xF2, (byte) 0x11, (byte) 0x4A, (byte) 0x54, (byte) 0x20, (byte) 0xCF, (byte) 0xB6};

        HuaweiPacket packetProductInformation = new HuaweiPacket(secretsProvider).parse(rawProductInformation);
        packetProductInformation.parseTlv();
    }

    @Test
    public void testBondRequest() throws NoSuchFieldException, IllegalAccessException {
        byte[] clientSerial = new byte[] {(byte) 0x54, (byte) 0x56, (byte) 0x64, (byte) 0x54, (byte) 0x4D, (byte) 0x44};
        String mac = "FF:FF:FF:FF:FF:CC";
        HuaweiCrypto huaweiCrypto = new HuaweiCrypto(0x01);
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);
        try {
            byte[] encryptionKey = huaweiCrypto.createSecretKey(mac);
            byte[] iv = secretsProvider.getIv();
            byte[] key = huaweiCrypto.encryptBondingKey(secretsProvider.getEncryptMethod(), secretsProvider.getSecretKey(), encryptionKey, iv);
            HuaweiTLV expectedTlv = new HuaweiTLV()
                    .put(0x01)
                    .put(0x03, (byte) 0x00)
                    .put(0x05, clientSerial)
                    .put(0x06, key)
                    .put(0x07, iv);

            byte[] serialized = new byte[]{(byte) 0x5A, (byte) 0x00, (byte) 0x44, (byte) 0x00, (byte) 0x01, (byte) 0x0E, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0x06, (byte) 0x54, (byte) 0x56, (byte) 0x64, (byte) 0x54, (byte) 0x4D, (byte) 0x44, (byte) 0x06, (byte) 0x20, (byte) 0x88, (byte) 0x45, (byte) 0xAA, (byte) 0xB5, (byte) 0x9C, (byte) 0x84, (byte) 0x39, (byte) 0xAE, (byte) 0xD8, (byte) 0xE9, (byte) 0x71, (byte) 0x01, (byte) 0x5D, (byte) 0xC8, (byte) 0x34, (byte) 0x05, (byte) 0xC5, (byte) 0x9A, (byte) 0x6B, (byte) 0xDB, (byte) 0x62, (byte) 0x7D, (byte) 0xC8, (byte) 0xC3, (byte) 0xF4, (byte) 0xCC, (byte) 0x30, (byte) 0x74, (byte) 0x21, (byte) 0xD4, (byte) 0x45, (byte) 0x0E, (byte) 0x07, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x72, (byte) 0xFC};
            DeviceConfig.Bond.Request request = new DeviceConfig.Bond.Request(
                    secretsProvider,
                    clientSerial,
                    key,
                    iv
            );

            Assert.assertEquals(0x01, request.serviceId);
            Assert.assertEquals(0x0E, request.commandId);
            Assert.assertEquals(expectedTlv, tlvField.get(request));
            Assert.assertTrue(request.complete);
            List<byte[]> out = request.serialize();
            Assert.assertEquals(1, out.size());
            Assert.assertArrayEquals(serialized, out.get(0));
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | HuaweiPacket.CryptoException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testBondParamsRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        byte[] clientSerial = new byte[] {(byte) 0x54, (byte) 0x56, (byte) 0x64, (byte) 0x54, (byte) 0x4D, (byte) 0x44};
        byte[] mac = new byte[] {(byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x43, (byte) 0x43};
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01)
                .put(0x03, clientSerial)
                .put(0x04, (byte) 0x02)
                .put(0x05)
                .put(0x07, mac)
                .put(0x09);

        byte[] serialized = new byte[] {(byte) 0x5A, (byte) 0x00, (byte) 0x27, (byte) 0x00, (byte) 0x01, (byte) 0x0F, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x06, (byte) 0x54, (byte) 0x56, (byte) 0x64, (byte) 0x54, (byte) 0x4D, (byte) 0x44, (byte) 0x04, (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0x00, (byte) 0x07, (byte) 0x11, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x46, (byte) 0x46, (byte) 0x3A, (byte) 0x43, (byte) 0x43, (byte) 0x09, (byte) 0x00, (byte) 0xE5, (byte) 0xD8};
        DeviceConfig.BondParams.Request request = new DeviceConfig.BondParams.Request(
                secretsProvider,
                clientSerial,
                mac
        );
        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x0F, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(serialized, out.get(0));
    }

    @Test
    public void testAuthRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        byte[] challenge = new byte[] {(byte) 0x9D, (byte) 0xF6, (byte) 0x52, (byte) 0x69, (byte) 0x06, (byte) 0x7B, (byte) 0xEB, (byte) 0x46, (byte) 0x94, (byte) 0xAD, (byte) 0x35, (byte) 0xE2, (byte) 0x88, (byte) 0xC3, (byte) 0x84, (byte) 0x24, (byte) 0xA2, (byte) 0x55, (byte) 0xD8, (byte) 0x0F, (byte) 0xA7, (byte) 0x68, (byte) 0x21, (byte) 0x9B, (byte) 0xA1, (byte) 0xC3, (byte) 0xDC, (byte) 0x09, (byte) 0x24, (byte) 0x81, (byte) 0x51, (byte) 0x61};
        byte[] nonce = new byte[] {(byte) 0x00, (byte) 0x01, (byte) 0xBF, (byte) 0x1F, (byte) 0xEF, (byte) 0x9F, (byte) 0xF0, (byte) 0xFE, (byte) 0xEF, (byte) 0xEF, (byte) 0x9F, (byte) 0xEF, (byte) 0xF0, (byte) 0xEF, (byte) 0xF8, (byte) 0xFA, (byte) 0xEF, (byte) 0xF0};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, challenge)
                .put(0x02, nonce);

        byte[] serialized = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x39, (byte) 0x00, (byte) 0x01, (byte) 0x13, (byte) 0x01, (byte) 0x20, (byte) 0x9d, (byte) 0xf6, (byte) 0x52, (byte) 0x69, (byte) 0x06, (byte) 0x7b, (byte) 0xeb, (byte) 0x46, (byte) 0x94, (byte) 0xad, (byte) 0x35, (byte) 0xe2, (byte) 0x88, (byte) 0xc3, (byte) 0x84, (byte) 0x24, (byte) 0xa2, (byte) 0x55, (byte) 0xd8, (byte) 0x0f, (byte) 0xa7, (byte) 0x68, (byte) 0x21, (byte) 0x9b, (byte) 0xa1, (byte) 0xc3, (byte) 0xdc, (byte) 0x09, (byte) 0x24, (byte) 0x81, (byte) 0x51, (byte) 0x61, (byte) 0x02, (byte) 0x12, (byte) 0x00, (byte) 0x01, (byte) 0xbf, (byte) 0x1f, (byte) 0xef, (byte) 0x9f, (byte) 0xf0, (byte) 0xfe, (byte) 0xef, (byte) 0xef, (byte) 0x9f, (byte) 0xef, (byte) 0xf0, (byte) 0xef, (byte) 0xf8, (byte) 0xfa, (byte) 0xef, (byte) 0xf0, (byte) 0xdc, (byte) 0x88};
        DeviceConfig.Auth.Request request = new DeviceConfig.Auth.Request(
                secretsProvider,
                challenge,
                nonce
        );

        Assert.assertEquals(0x01, request.serviceId);
        Assert.assertEquals(0x13, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(serialized, out.get(0));
    }
}
