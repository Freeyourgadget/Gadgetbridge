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
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class TestFitnessData {

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
    public void testMessageCountRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        int startSleep = 0x00000000;
        int endSleep = 0x01020304;
        int startStep = 0x01020304;
        int endStep = 0x10203040;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlvSleep = new HuaweiTLV()
                .put(0x81)
                .put(0x03, startSleep)
                .put(0x04, endSleep);
        HuaweiTLV expectedTlvStep = new HuaweiTLV()
                .put(0x81)
                .put(0x03, startStep)
                .put(0x04, endStep);

        byte[] sleepSerialized = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x0c, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x32, (byte) 0x69, (byte) 0x7b, (byte) 0x51, (byte) 0x85, (byte) 0x20, (byte) 0x9b, (byte) 0x16, (byte) 0x6b, (byte) 0x93, (byte) 0x8a, (byte) 0x3d, (byte) 0xd5, (byte) 0x9a, (byte) 0xf9, (byte) 0x29, (byte) 0xdf, (byte) 0x07};
        byte[] stepSerialized = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x0a, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x4d, (byte) 0x52, (byte) 0x79, (byte) 0x57, (byte) 0x49, (byte) 0x30, (byte) 0x75, (byte) 0xc6, (byte) 0x28, (byte) 0x5b, (byte) 0x79, (byte) 0xd5, (byte) 0xab, (byte) 0x89, (byte) 0x0d, (byte) 0x1e, (byte) 0xa9, (byte) 0xc9};

        FitnessData.MessageCount.Request sleepRequest = new FitnessData.MessageCount.Request(secretsProvider, FitnessData.MessageCount.sleepId, startSleep, endSleep);
        FitnessData.MessageCount.Request stepRequest = new FitnessData.MessageCount.Request(secretsProvider, FitnessData.MessageCount.stepId, startStep, endStep);

        Assert.assertEquals(0x07, sleepRequest.serviceId);
        Assert.assertEquals(0x0c, sleepRequest.commandId);
        Assert.assertEquals(expectedTlvSleep, tlvField.get(sleepRequest));
        Assert.assertTrue(sleepRequest.complete);
        List<byte[]> outSleep = sleepRequest.serialize();
        Assert.assertEquals(1, outSleep.size());
        Assert.assertArrayEquals(sleepSerialized, outSleep.get(0));

        Assert.assertEquals(0x07, stepRequest.serviceId);
        Assert.assertEquals(0x0a, stepRequest.commandId);
        Assert.assertEquals(expectedTlvStep, tlvField.get(stepRequest));
        Assert.assertTrue(stepRequest.complete);
        List<byte[]> outStep = stepRequest.serialize();
        Assert.assertEquals(1, outStep.size());
        Assert.assertArrayEquals(stepSerialized, outStep.get(0));
    }

    @Test
    public void testMessageCountResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] rawSleep = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x0c, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xf6, (byte) 0xfb, (byte) 0xc0, (byte) 0xb6, (byte) 0x4f, (byte) 0x9a, (byte) 0xfa, (byte) 0x77, (byte) 0x53, (byte) 0x28, (byte) 0x7d, (byte) 0x13, (byte) 0xca, (byte) 0x49, (byte) 0xda, (byte) 0xfd, (byte) 0x93, (byte) 0x09};
        byte[] rawStep = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x0a, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xf6, (byte) 0xfb, (byte) 0xc0, (byte) 0xb6, (byte) 0x4f, (byte) 0x9a, (byte) 0xfa, (byte) 0x77, (byte) 0x53, (byte) 0x28, (byte) 0x7d, (byte) 0x13, (byte) 0xca, (byte) 0x49, (byte) 0xda, (byte) 0xfd, (byte) 0xd4, (byte) 0x93};

        short expectedCount = 0x1337;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV().put(0x02, expectedCount));

        HuaweiPacket packetSleep = new HuaweiPacket(secretsProvider).parse(rawSleep);
        HuaweiPacket packetStep = new HuaweiPacket(secretsProvider).parse(rawStep);
        packetSleep.parseTlv();
        packetStep.parseTlv();

        Assert.assertEquals(0x07, packetSleep.serviceId);
        Assert.assertEquals(0x0c, packetSleep.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packetSleep));
        Assert.assertTrue(packetSleep.complete);
        Assert.assertTrue(packetSleep instanceof FitnessData.MessageCount.Response);
        Assert.assertEquals(expectedCount, ((FitnessData.MessageCount.Response) packetSleep).count);

        Assert.assertEquals(0x07, packetStep.serviceId);
        Assert.assertEquals(0x0a, packetStep.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packetStep));
        Assert.assertTrue(packetStep.complete);
        Assert.assertTrue(packetStep instanceof FitnessData.MessageCount.Response);
        Assert.assertEquals(expectedCount, ((FitnessData.MessageCount.Response) packetStep).count);
    }

    @Test
    public void testMessageDataRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiCrypto.CryptoException, HuaweiPacket.CryptoException {
        short count = 0x1337;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV().put(0x02, count));
        expectedTlv.encrypt(secretsProvider);

        byte[] expectedSleep = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x0d, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xf6, (byte) 0xfb, (byte) 0xc0, (byte) 0xb6, (byte) 0x4f, (byte) 0x9a, (byte) 0xfa, (byte) 0x77, (byte) 0x53, (byte) 0x28, (byte) 0x7d, (byte) 0x13, (byte) 0xca, (byte) 0x49, (byte) 0xda, (byte) 0xfd, (byte) 0x7d, (byte) 0xad};
        byte[] expectedStep = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x0b, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xf6, (byte) 0xfb, (byte) 0xc0, (byte) 0xb6, (byte) 0x4f, (byte) 0x9a, (byte) 0xfa, (byte) 0x77, (byte) 0x53, (byte) 0x28, (byte) 0x7d, (byte) 0x13, (byte) 0xca, (byte) 0x49, (byte) 0xda, (byte) 0xfd, (byte) 0x3a, (byte) 0x37};

        FitnessData.MessageData.Request sleepRequest = new FitnessData.MessageData.Request(secretsProvider, FitnessData.MessageData.sleepId, count);
        FitnessData.MessageData.Request stepRequest = new FitnessData.MessageData.Request(secretsProvider, FitnessData.MessageData.stepId, count);

        Assert.assertEquals(0x07, sleepRequest.serviceId);
        Assert.assertEquals(0x0d, sleepRequest.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(sleepRequest));
        Assert.assertTrue(sleepRequest.complete);
        List<byte[]> outSleep = sleepRequest.serialize();
        Assert.assertEquals(1, outSleep.size());
        Assert.assertArrayEquals(expectedSleep, outSleep.get(0));

        Assert.assertEquals(0x07, stepRequest.serviceId);
        Assert.assertEquals(0x0b, stepRequest.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(stepRequest));
        Assert.assertTrue(stepRequest.complete);
        List<byte[]> outStep = stepRequest.serialize();
        Assert.assertEquals(1, outStep.size());
        Assert.assertArrayEquals(expectedStep, outStep.get(0));
    }

    @Test
    public void testMessageDataSleepResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x07, (byte) 0x0d, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x20, (byte) 0xa4, (byte) 0x9e, (byte) 0xd8, (byte) 0xd3, (byte) 0x7a, (byte) 0x0e, (byte) 0x51, (byte) 0x55, (byte) 0xc5, (byte) 0x48, (byte) 0x07, (byte) 0x99, (byte) 0xf5, (byte) 0x99, (byte) 0x48, (byte) 0x3e, (byte) 0x41, (byte) 0xed, (byte) 0x16, (byte) 0xf1, (byte) 0x52, (byte) 0xd2, (byte) 0x9f, (byte) 0x38, (byte) 0xe8, (byte) 0xb1, (byte) 0x83, (byte) 0xd6, (byte) 0xcb, (byte) 0x52, (byte) 0xb0, (byte) 0x9f, (byte) 0x48, (byte) 0x05};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, (short) 0x1337)
                .put(0x83, new HuaweiTLV()
                        .put(0x04, (byte) 0x00)
                        .put(0x05, new byte[] {})
                )
                .put(0x83, new HuaweiTLV()
                        .put(0x04, (byte) 0x01)
                        .put(0x05, new byte[] {0x01, 0x02})
                )
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x07, packet.serviceId);
        Assert.assertEquals(0x0d, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof FitnessData.MessageData.SleepResponse);
        Assert.assertEquals(0x1337, ((FitnessData.MessageData.SleepResponse) packet).number);
        Assert.assertEquals(2, ((FitnessData.MessageData.SleepResponse) packet).containers.size());
        Assert.assertEquals(0x00, ((FitnessData.MessageData.SleepResponse) packet).containers.get(0).type);
        Assert.assertArrayEquals(new byte[] {}, ((FitnessData.MessageData.SleepResponse) packet).containers.get(0).timestamp);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.SleepResponse) packet).containers.get(1).type);
        Assert.assertArrayEquals(new byte[] {0x01, 0x02}, ((FitnessData.MessageData.SleepResponse) packet).containers.get(1).timestamp);
    }

    @Test
    public void testMessageDataStepResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x5a, (byte) 0x00, (byte) 0x07, (byte) 0x0b, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x40, (byte) 0xdc, (byte) 0xb7, (byte) 0xf6, (byte) 0xaa, (byte) 0xb2, (byte) 0xf1, (byte) 0x03, (byte) 0x53, (byte) 0x25, (byte) 0x39, (byte) 0xe4, (byte) 0x79, (byte) 0xdd, (byte) 0xbf, (byte) 0x18, (byte) 0x7b, (byte) 0x98, (byte) 0x30, (byte) 0xb7, (byte) 0x4c, (byte) 0x33, (byte) 0xd2, (byte) 0x0c, (byte) 0xa5, (byte) 0xee, (byte) 0xfe, (byte) 0x5f, (byte) 0xa5, (byte) 0x12, (byte) 0x20, (byte) 0xec, (byte) 0x79, (byte) 0x38, (byte) 0xec, (byte) 0x9e, (byte) 0x4d, (byte) 0xfc, (byte) 0xc3, (byte) 0x5c, (byte) 0x59, (byte) 0x67, (byte) 0x51, (byte) 0x4b, (byte) 0xef, (byte) 0x50, (byte) 0x48, (byte) 0xb7, (byte) 0xf8, (byte) 0xc7, (byte) 0xe3, (byte) 0xf7, (byte) 0xdf, (byte) 0x82, (byte) 0xb4, (byte) 0x1a, (byte) 0xb8, (byte) 0x94, (byte) 0x78, (byte) 0x0d, (byte) 0xda, (byte) 0x53, (byte) 0xe3, (byte) 0xbe, (byte) 0xbf, (byte) 0x21, (byte) 0xc2};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        // TODO: add HR data

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, (short) 0x1337)
                .put(0x03, 0xCAFEBEEF)
                .put(0x084, new HuaweiTLV()
                        .put(0x05, (byte) 0x00)
                        .put(0x06, new byte[] {})
                )
                .put(0x84, new HuaweiTLV()
                        .put(0x05, (byte) 0x01)
                        .put(0x06, new byte[] {0x01, 0x02})
                )
                .put(0x84, new HuaweiTLV()
                        .put(0x05, (byte) 0x02)
                        .put(0x06, new byte[] {0x0e, 0x00, 0x01, 0x00, 0x02, 0x00, 0x03})
                )
                .put(0x84, new HuaweiTLV()
                        .put(0x05, (byte) 0x02)
                        .put(0x06, new byte[] {0x01, 0x00, 0x01})
                )
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x07, packet.serviceId);
        Assert.assertEquals(0x0b, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof FitnessData.MessageData.StepResponse);

        Assert.assertEquals(0x1337, ((FitnessData.MessageData.StepResponse) packet).number);
        Assert.assertEquals(0xCAFEBEEF, ((FitnessData.MessageData.StepResponse) packet).timestamp);
        Assert.assertEquals(4, ((FitnessData.MessageData.StepResponse) packet).containers.size());

        Assert.assertEquals(0x00, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).timestampOffset);
        Assert.assertArrayEquals(new byte[] {}, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).data);
        Assert.assertEquals(0xCAFEBEEF, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).timestamp);
        Assert.assertNull(((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData);
        Assert.assertEquals("Data is missing feature bitmap.", ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedDataError);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).steps);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).calories);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).distance);
        Assert.assertNull(((FitnessData.MessageData.StepResponse) packet).containers.get(0).unknownTVs);

        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).timestampOffset);
        Assert.assertArrayEquals(new byte[] {0x01, 0x02}, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).data);
        Assert.assertEquals(0xCAFEBF2B, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).timestamp);
        Assert.assertNull(((FitnessData.MessageData.StepResponse) packet).containers.get(1).parsedData);
        Assert.assertEquals("Data is too short for selected features.", ((FitnessData.MessageData.StepResponse) packet).containers.get(1).parsedDataError);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).steps);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).calories);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).distance);
        Assert.assertEquals(0, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).unknownTVs.size());

        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).timestampOffset);
        Assert.assertArrayEquals(new byte[] {0x0e, 0x00, 0x01, 0x00, 0x02, 0x00, 0x03}, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).data);
        Assert.assertEquals(0xCAFEBF67, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).timestamp);
        Assert.assertEquals(3, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.size());
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(0).bitmap);
        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(0).tag);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(0).value);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(1).bitmap);
        Assert.assertEquals(0x04, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(1).tag);
        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(1).value);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(2).bitmap);
        Assert.assertEquals(0x08, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(2).tag);
        Assert.assertEquals(0x03, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedData.get(2).value);
        Assert.assertEquals("", ((FitnessData.MessageData.StepResponse) packet).containers.get(2).parsedDataError);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).steps);
        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).calories);
        Assert.assertEquals(0x03, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).distance);
        Assert.assertEquals(0, ((FitnessData.MessageData.StepResponse) packet).containers.get(2).unknownTVs.size());

        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).timestampOffset);
        Assert.assertArrayEquals(new byte[] {0x01, 0x00, 0x01}, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).data);
        Assert.assertEquals(0xCAFEBF67, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).timestamp);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).parsedData.size());
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).parsedData.get(0).bitmap);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).parsedData.get(0).tag);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).parsedData.get(0).value);
        Assert.assertEquals("", ((FitnessData.MessageData.StepResponse) packet).containers.get(3).parsedDataError);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).steps);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).calories);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).distance);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).unknownTVs.size());
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).unknownTVs.get(0).bitmap);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).unknownTVs.get(0).tag);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(3).unknownTVs.get(0).value);
    }

    @Test
    public void testMessageDataStepResponseSingleByte() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x07, (byte) 0x0b, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x20, (byte) 0xcf, (byte) 0xa7, (byte) 0x76, (byte) 0x30, (byte) 0x69, (byte) 0xa3, (byte) 0x83, (byte) 0x6e, (byte) 0xd2, (byte) 0x84, (byte) 0x70, (byte) 0xc8, (byte) 0xca, (byte) 0x94, (byte) 0x87, (byte) 0xd2, (byte) 0x0d, (byte) 0x1e, (byte) 0xf5, (byte) 0x60, (byte) 0x72, (byte) 0xa4, (byte) 0xd9, (byte) 0x8f, (byte) 0xf6, (byte) 0xdf, (byte) 0x09, (byte) 0x35, (byte) 0x3c, (byte) 0x86, (byte) 0x62, (byte) 0x00, (byte) 0x0a, (byte) 0x3b};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        // TODO: change test as 0x40 is now added as HR

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, (short) 0x01)
                .put(0x03, 0x02)
                .put(0x84, new HuaweiTLV()
                        .put(0x05, (byte) 0x00)
                        .put(0x06, new byte[] {0x20, 0x01})
                )
                .put(0x84, new HuaweiTLV()
                        .put(0x05, (byte) 0x01)
                        .put(0x06, new byte[] {0x40, 0x02})
                )
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x07, packet.serviceId);
        Assert.assertEquals(0x0b, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof FitnessData.MessageData.StepResponse);

        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).number);
        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).timestamp);
        Assert.assertEquals(2, ((FitnessData.MessageData.StepResponse) packet).containers.size());

        Assert.assertEquals(0x00, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).timestampOffset);
        Assert.assertArrayEquals(new byte[] {0x20, 0x01}, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).data);
        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).timestamp);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.size());
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.get(0).bitmap);
        Assert.assertEquals(0x20, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.get(0).tag);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.get(0).value);
        Assert.assertEquals("", ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedDataError);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).steps);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).calories);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).distance);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).unknownTVs.size());
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).unknownTVs.get(0).bitmap);
        Assert.assertEquals(0x20, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).unknownTVs.get(0).tag);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).unknownTVs.get(0).value);

        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).timestampOffset);
        Assert.assertArrayEquals(new byte[] {0x40, 0x02}, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).data);
        Assert.assertEquals(0x3e, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).timestamp);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).parsedData.size());
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).parsedData.get(0).bitmap);
        Assert.assertEquals(0x40, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).parsedData.get(0).tag);
        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).parsedData.get(0).value);
        Assert.assertEquals("", ((FitnessData.MessageData.StepResponse) packet).containers.get(1).parsedDataError);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).steps);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).calories);
        Assert.assertEquals(-1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).distance);
        Assert.assertEquals(0, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).unknownTVs.size());
//        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).unknownTVs.get(0).bitmap);
//        Assert.assertEquals(0x40, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).unknownTVs.get(0).tag);
//        Assert.assertEquals(0x02, ((FitnessData.MessageData.StepResponse) packet).containers.get(1).unknownTVs.get(0).value);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testActivityReminderRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException, HuaweiCrypto.CryptoException {
        boolean longSitSwitch = false;
        byte longSitInterval = 0x00;
        byte[] longSitStart = {0x01, 0x02};
        byte[] longSitEnd = {0x03, 0x04};
        byte cycle = 0x05;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x81, new HuaweiTLV()
                        .put(0x02, longSitSwitch)
                        .put(0x03, longSitInterval)
                        .put(0x04, longSitStart)
                        .put(0x05, longSitEnd)
                        .put(0x06, cycle)
                );
        expectedTlv.encrypt(secretsProvider);

        byte[] expected = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x07, (byte) 0x07, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x20, (byte) 0x5b, (byte) 0x9b, (byte) 0x16, (byte) 0xa8, (byte) 0x65, (byte) 0x81, (byte) 0xc1, (byte) 0x18, (byte) 0x2f, (byte) 0x42, (byte) 0xab, (byte) 0xf3, (byte) 0x43, (byte) 0x1e, (byte) 0x5c, (byte) 0x32, (byte) 0x9a, (byte) 0xa9, (byte) 0xa2, (byte) 0x18, (byte) 0x36, (byte) 0xb3, (byte) 0x60, (byte) 0x39, (byte) 0xeb, (byte) 0xdb, (byte) 0x6b, (byte) 0xe5, (byte) 0xac, (byte) 0x7b, (byte) 0x45, (byte) 0x36, (byte) 0xbc, (byte) 0x0c};

        FitnessData.ActivityReminder.Request request = new FitnessData.ActivityReminder.Request(
                secretsProvider,
                longSitSwitch,
                longSitInterval,
                longSitStart,
                longSitEnd,
                cycle
        );

        Assert.assertEquals(0x07, request.serviceId);
        Assert.assertEquals(0x07, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expected, out.get(0));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testTruSleepRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiCrypto.CryptoException, HuaweiPacket.CryptoException {
        boolean truSleepSwitch = false;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, truSleepSwitch);
        expectedTlv.encrypt(secretsProvider);

        byte [] expected = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x16, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x28, (byte) 0x00, (byte) 0x99, (byte) 0x6f, (byte) 0x2a, (byte) 0xcb, (byte) 0x62, (byte) 0x3a, (byte) 0xe6, (byte) 0x54, (byte) 0x28, (byte) 0x54, (byte) 0xf8, (byte) 0xab, (byte) 0x54, (byte) 0x83, (byte) 0x02, (byte) 0x23};

        FitnessData.TruSleep.Request request = new FitnessData.TruSleep.Request(secretsProvider, truSleepSwitch);

        Assert.assertEquals(0x07, request.serviceId);
        Assert.assertEquals(0x16, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expected, out.get(0));
    }

    @Test
    public void testMessageDataStepResponseNoCount() throws NoSuchFieldException, IllegalAccessException {
        // I've seen this happening because of a bug in the counts, so it's probably best to stop the sync if this happens.
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x07, (byte) 0x0b, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x4c, (byte) 0xdd, (byte) 0x99, (byte) 0x79, (byte) 0xf6, (byte) 0x3c, (byte) 0x1e, (byte) 0xbb, (byte) 0x0a, (byte) 0x95, (byte) 0x8d, (byte) 0x12, (byte) 0x05, (byte) 0x81, (byte) 0x7f, (byte) 0xff, (byte) 0xeb, (byte) 0x45};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        try {
            HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
            packet.parseTlv();
            Assert.fail();
        } catch (HuaweiPacket.ParseException e) {
            if (e instanceof HuaweiPacket.MissingTagException) {
                Assert.assertNotNull(e.getMessage());
                if (!e.getMessage().equals("Missing tag: 2")) {
                    Assert.fail();
                }
            } else {
                Assert.fail();
            }
        }
    }

    @Test
    public void testSpoData() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = {(byte) 0x5a, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x07, (byte) 0x0b, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x20, (byte) 0x30, (byte) 0xdf, (byte) 0x42, (byte) 0xc9, (byte) 0x79, (byte) 0x91, (byte) 0x36, (byte) 0x3d, (byte) 0x80, (byte) 0x6b, (byte) 0x99, (byte) 0xd3, (byte) 0x3f, (byte) 0xbf, (byte) 0x1f, (byte) 0x1e, (byte) 0xc1, (byte) 0x0b, (byte) 0xbf, (byte) 0xcd, (byte) 0xae, (byte) 0x38, (byte) 0x89, (byte) 0x60, (byte) 0x60, (byte) 0xf7, (byte) 0x93, (byte) 0x84, (byte) 0x3a, (byte) 0x09, (byte) 0xd3, (byte) 0x77, (byte) 0x1e, (byte) 0xb9};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, (short) 0x1337)
                .put(0x03, 0xCAFEBEEF)
                .put(0x84, new HuaweiTLV()
                        .put(0x05, (byte) 0x00)
                        .put(0x06, new byte[] {(byte) 0x80, 0x01, 0x42})
                )
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x07, packet.serviceId);
        Assert.assertEquals(0x0b, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof FitnessData.MessageData.StepResponse);

        Assert.assertEquals(0x1337, ((FitnessData.MessageData.StepResponse) packet).number);
        Assert.assertEquals(0xCAFEBEEF, ((FitnessData.MessageData.StepResponse) packet).timestamp);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.size());

        Assert.assertEquals(0x00, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).timestampOffset);
        Assert.assertArrayEquals(new byte[] {(byte) 0x80, 0x01, 0x42}, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).data);
        Assert.assertEquals(0xCAFEBEEF, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).timestamp);
        Assert.assertEquals(1, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.size());
        Assert.assertEquals(2, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.get(0).bitmap);
        Assert.assertEquals(0x01, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.get(0).tag);
        Assert.assertEquals(0x42, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedData.get(0).value);
        Assert.assertEquals("", ((FitnessData.MessageData.StepResponse) packet).containers.get(0).parsedDataError);
        Assert.assertEquals(0, ((FitnessData.MessageData.StepResponse) packet).containers.get(0).unknownTVs.size());
    }
}
