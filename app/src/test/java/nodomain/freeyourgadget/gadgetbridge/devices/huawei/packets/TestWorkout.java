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
import java.nio.ByteBuffer;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class TestWorkout {

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
    public void testWorkoutCountRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        int start = 0x00000000;
        int end = 0x01020304;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x03, start)
                .put(0x04, end)
        );

        byte[] expected = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x17, (byte) 0x07, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xf7, (byte) 0x48, (byte) 0xf7, (byte) 0x49, (byte) 0x4a, (byte) 0xa5, (byte) 0xb2, (byte) 0xc9, (byte) 0x41, (byte) 0xf5, (byte) 0x7f, (byte) 0xb4, (byte) 0xe9, (byte) 0x17, (byte) 0xac, (byte) 0xb5, (byte) 0x5f, (byte) 0x8e};

        Workout.WorkoutCount.Request request = new Workout.WorkoutCount.Request(secretsProvider, start, end);

        Assert.assertEquals(0x17, request.serviceId);
        Assert.assertEquals(0x07, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expected, out.get(0));
    }

    @Test
    public void testWorkoutCountResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x4a, (byte) 0x00, (byte) 0x17, (byte) 0x07, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x30, (byte) 0xee, (byte) 0xdd, (byte) 0xa9, (byte) 0x23, (byte) 0x2c, (byte) 0xe4, (byte) 0x9f, (byte) 0x41, (byte) 0x0b, (byte) 0x9f, (byte) 0x7a, (byte) 0xc2, (byte) 0xe0, (byte) 0x72, (byte) 0x6d, (byte) 0xe1, (byte) 0x8f, (byte) 0xd0, (byte) 0xe7, (byte) 0x41, (byte) 0x59, (byte) 0x38, (byte) 0xac, (byte) 0x17, (byte) 0x66, (byte) 0xc8, (byte) 0x60, (byte) 0xd7, (byte) 0xd2, (byte) 0x32, (byte) 0x8b, (byte) 0xa5, (byte) 0x91, (byte) 0xc7, (byte) 0xc5, (byte) 0xe5, (byte) 0x7d, (byte) 0x8d, (byte) 0xa1, (byte) 0xd0, (byte) 0x6f, (byte) 0xe2, (byte) 0xe2, (byte) 0x24, (byte) 0x7d, (byte) 0xef, (byte) 0x02, (byte) 0x03, (byte) 0x59, (byte) 0x3e};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, (short) 0x1337)
                .put(0x85, new HuaweiTLV()
                        .put(0x06, (short) 0x0001)
                        .put(0x07, (short) 0x0002)
                        .put(0x08, (short) 0x0003)
                        .put(0x0a, (short) 0x0004)
                )
                .put(0x85, new HuaweiTLV()
                        .put(0x06, (short) 0x0005)
                        .put(0x07, (short) 0x0006)
                        .put(0x08, (short) 0x0007)
                        .put(0x0a, (short) 0x0008)
                )
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x17, packet.serviceId);
        Assert.assertEquals(0x07, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof Workout.WorkoutCount.Response);
        Assert.assertEquals(0x1337, ((Workout.WorkoutCount.Response) packet).count);
        Assert.assertEquals(2, ((Workout.WorkoutCount.Response) packet).workoutNumbers.size());

        Assert.assertArrayEquals(new byte[] {0x06, 0x02, 0x00, 0x01, 0x07, 0x02, 0x00, 0x02, 0x08, 0x02, 0x00, 0x03, 0x0a, 0x02, 0x00, 0x04}, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(0).rawData);
        Assert.assertEquals(0x01, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(0).workoutNumber);
        Assert.assertEquals(0x02, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(0).dataCount);
        Assert.assertEquals(0x03, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(0).paceCount);

        Assert.assertArrayEquals(new byte[] {0x06, 0x02, 0x00, 0x05, 0x07, 0x02, 0x00, 0x06, 0x08, 0x02, 0x00, 0x07, 0x0a, 0x02, 0x00, 0x08}, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(1).rawData);
        Assert.assertEquals(0x05, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(1).workoutNumber);
        Assert.assertEquals(0x06, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(1).dataCount);
        Assert.assertEquals(0x07, ((Workout.WorkoutCount.Response) packet).workoutNumbers.get(1).paceCount);
    }

    @Test
    public void testWorkoutTotalsRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        short number = 0x1337;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, number)
        );

        byte[] expected = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x17, (byte) 0x08, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xf6, (byte) 0xfb, (byte) 0xc0, (byte) 0xb6, (byte) 0x4f, (byte) 0x9a, (byte) 0xfa, (byte) 0x77, (byte) 0x53, (byte) 0x28, (byte) 0x7d, (byte) 0x13, (byte) 0xca, (byte) 0x49, (byte) 0xda, (byte) 0xfd, (byte) 0x26, (byte) 0x91};

        Workout.WorkoutTotals.Request request = new Workout.WorkoutTotals.Request(secretsProvider, number);

        Assert.assertEquals(0x17, request.serviceId);
        Assert.assertEquals(0x08, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expected, out.get(0));
    }

    @Test
    public void testWorkoutTotalsResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x5a, (byte) 0x00, (byte) 0x17, (byte) 0x08, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x40, (byte) 0x0f, (byte) 0xa0, (byte) 0x3a, (byte) 0x90, (byte) 0xae, (byte) 0x8c, (byte) 0xcf, (byte) 0x03, (byte) 0xce, (byte) 0x5a, (byte) 0x68, (byte) 0x87, (byte) 0x05, (byte) 0x51, (byte) 0xf7, (byte) 0x2f, (byte) 0x78, (byte) 0xbd, (byte) 0x84, (byte) 0xf1, (byte) 0x4f, (byte) 0xb8, (byte) 0x51, (byte) 0x28, (byte) 0xec, (byte) 0xfd, (byte) 0x8b, (byte) 0x2e, (byte) 0x99, (byte) 0xd3, (byte) 0x42, (byte) 0xd7, (byte) 0x65, (byte) 0xb2, (byte) 0x82, (byte) 0x02, (byte) 0x28, (byte) 0x00, (byte) 0x34, (byte) 0xbc, (byte) 0x39, (byte) 0x59, (byte) 0x8f, (byte) 0x0b, (byte) 0xa7, (byte) 0x3a, (byte) 0x5c, (byte) 0xfb, (byte) 0xf1, (byte) 0xd4, (byte) 0x8f, (byte) 0xf6, (byte) 0x6d, (byte) 0x98, (byte) 0xd6, (byte) 0x5a, (byte) 0x51, (byte) 0x0a, (byte) 0x4a, (byte) 0x1c, (byte) 0x42, (byte) 0xc8, (byte) 0x9d, (byte) 0xee, (byte) 0x55, (byte) 0x44};

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, (short) 0x1337)
                .put(0x03, (byte) 0x01)
                .put(0x04, 0x01020304)
                .put(0x05, 0x05060708)
                .put(0x06, 0x090a0b0c)
                .put(0x07, 0x0d0e0f10)
                .put(0x08, 0x11121314)
                .put(0x09, 0x15161718)
                .put(0x12, 0x191a1b1c)
                .put(0x14, (byte) 0x1d)
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        // TODO: find out what the status and type can be

        Assert.assertEquals(0x17, packet.serviceId);
        Assert.assertEquals(0x08, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof Workout.WorkoutTotals.Response);
        Assert.assertArrayEquals(new byte[] {0x02, 0x02, 0x13, 0x37, 0x03, 0x01, 0x01, 0x04, 0x04, 0x01, 0x02, 0x03, 0x04, 0x05, 0x04, 0x05, 0x06, 0x07, 0x08, 0x06, 0x04, 0x09, 0x0a, 0x0b, 0x0c, 0x07, 0x04, 0x0d, 0x0e, 0x0f, 0x10, 0x08, 0x04, 0x11, 0x12, 0x13, 0x14, 0x09, 0x04, 0x15, 0x16, 0x17, 0x18, 0x12, 0x04, 0x19, 0x1a, 0x1b, 0x1c, 0x14, 0x01, 0x1d}, ((Workout.WorkoutTotals.Response) packet).rawData);
        Assert.assertEquals(0x1337, ((Workout.WorkoutTotals.Response) packet).number);
        Assert.assertEquals(0x01, ((Workout.WorkoutTotals.Response) packet).status);
        Assert.assertEquals(0x01020304, ((Workout.WorkoutTotals.Response) packet).startTime);
        Assert.assertEquals(0x05060708, ((Workout.WorkoutTotals.Response) packet).endTime);
        Assert.assertEquals(0x090a0b0c, ((Workout.WorkoutTotals.Response) packet).calories);
        Assert.assertEquals(0x0d0e0f10, ((Workout.WorkoutTotals.Response) packet).distance);
        Assert.assertEquals(0x11121314, ((Workout.WorkoutTotals.Response) packet).stepCount);
        Assert.assertEquals(0x15161718, ((Workout.WorkoutTotals.Response) packet).totalTime);
        Assert.assertEquals(0x191a1b1c, ((Workout.WorkoutTotals.Response) packet).duration);
        Assert.assertEquals(0x1d, ((Workout.WorkoutTotals.Response) packet).type);
    }

    @Test
    public void testWorkoutDataRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        short workoutNumber = 0x0102;
        short dataNumber = 0x0304;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, workoutNumber)
                .put(0x03, dataNumber)
        );

        byte[] expected = {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x17, (byte) 0x0a, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xd2, (byte) 0xd7, (byte) 0x55, (byte) 0x23, (byte) 0xeb, (byte) 0x51, (byte) 0x4f, (byte) 0xe0, (byte) 0x35, (byte) 0x6c, (byte) 0x60, (byte) 0xc5, (byte) 0xbf, (byte) 0x61, (byte) 0x68, (byte) 0xd1, (byte) 0x03, (byte) 0x83};

        Workout.WorkoutData.Request request = new Workout.WorkoutData.Request(secretsProvider, workoutNumber, dataNumber);

        Assert.assertEquals(0x17, request.serviceId);
        Assert.assertEquals(0x0a, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expected, out.get(0));
    }

    @Test
    public void testWorkoutDataResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = {(byte) 0x5a, (byte) 0x00, (byte) 0x5a, (byte) 0x00, (byte) 0x17, (byte) 0x0a, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x40, (byte) 0x03, (byte) 0x66, (byte) 0xf5, (byte) 0x16, (byte) 0xc9, (byte) 0x60, (byte) 0xb9, (byte) 0xf2, (byte) 0xe3, (byte) 0x88, (byte) 0x99, (byte) 0xab, (byte) 0x50, (byte) 0x22, (byte) 0xcb, (byte) 0x83, (byte) 0x53, (byte) 0xd0, (byte) 0xb2, (byte) 0xc3, (byte) 0x66, (byte) 0xa9, (byte) 0x16, (byte) 0x23, (byte) 0xa5, (byte) 0x8e, (byte) 0x81, (byte) 0x68, (byte) 0x85, (byte) 0x38, (byte) 0x3e, (byte) 0xd5, (byte) 0x8e, (byte) 0x21, (byte) 0xc8, (byte) 0xa1, (byte) 0x80, (byte) 0x98, (byte) 0x2d, (byte) 0x78, (byte) 0x75, (byte) 0x80, (byte) 0xa1, (byte) 0x39, (byte) 0x61, (byte) 0xa6, (byte) 0x3e, (byte) 0x61, (byte) 0x2c, (byte) 0x5e, (byte) 0xe2, (byte) 0x6f, (byte) 0xef, (byte) 0xdf, (byte) 0xdb, (byte) 0x39, (byte) 0x8f, (byte) 0xab, (byte) 0x21, (byte) 0xde, (byte) 0xba, (byte) 0xdb, (byte) 0x2c, (byte) 0xff, (byte) 0x97, (byte) 0x94};

        short workoutNumber = 0x0102;
        short dataNumber = 0x0304;

        int timestamp = 0x05060708;
        byte interval = 0x09;
        short dataCount = 0x0002;
        byte dataLength = 0x0F; // Data length must match
        short bitmap = 0x0042; // Inner data and speed

        short speed1 = 0x0a0b;
        short cadence1 = 0x0c0d;
        short stepLength1 = 0x0e0f;
        short groundContactTime1 = 0x1011;
        byte groundImpact1 = 0x12;
        short swingAngle1 = 0x1314;
        byte foreFootLanding1 = 0x15;
        byte midFootLanding1 = 0x16;
        byte backFootLanding1 = 0x17;
        byte eversionAngle1 = 0x18;

        short speed2 = 0x191a;
        short cadence2 = 0x1b1c;
        short stepLength2 = 0x1d1e;
        short groundContactTime2 = 0x1f20;
        byte groundImpact2 = 0x21;
        short swingAngle2 = 0x2223;
        byte foreFootLanding2 = 0x24;
        byte midFootLanding2 = 0x25;
        byte backFootLanding2 = 0x26;
        byte eversionAngle2 = 0x27;

        // TODO: Add:
        //  - swolf
        //  - stoke rate
        //  - calories
        //  - cycling power
        //  - frequency
        //  - altitude

        ByteBuffer headerBuf = ByteBuffer.allocate(14);
        headerBuf.putShort(workoutNumber);
        headerBuf.putShort(dataNumber);
        headerBuf.putInt(timestamp);
        headerBuf.put(interval);
        headerBuf.putShort(dataCount);
        headerBuf.put(dataLength);
        headerBuf.putShort(bitmap);

        ByteBuffer dataBuf = ByteBuffer.allocate(30);

        dataBuf.putShort(speed1);
        dataBuf.putShort(cadence1);
        dataBuf.putShort(stepLength1);
        dataBuf.putShort(groundContactTime1);
        dataBuf.put(groundImpact1);
        dataBuf.putShort(swingAngle1);
        dataBuf.put(foreFootLanding1);
        dataBuf.put(midFootLanding1);
        dataBuf.put(backFootLanding1);
        dataBuf.put(eversionAngle1);

        dataBuf.putShort(speed2);
        dataBuf.putShort(cadence2);
        dataBuf.putShort(stepLength2);
        dataBuf.putShort(groundContactTime2);
        dataBuf.put(groundImpact2);
        dataBuf.putShort(swingAngle2);
        dataBuf.put(foreFootLanding2);
        dataBuf.put(midFootLanding2);
        dataBuf.put(backFootLanding2);
        dataBuf.put(eversionAngle2);

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, workoutNumber)
                .put(0x03, dataNumber)
                .put(0x04, headerBuf.array())
                .put(0x05, dataBuf.array())
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x17, packet.serviceId);
        Assert.assertEquals(0x0a, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof Workout.WorkoutData.Response);

        Assert.assertEquals(0x0102, ((Workout.WorkoutData.Response) packet).workoutNumber);
        Assert.assertEquals(0x0304, ((Workout.WorkoutData.Response) packet).dataNumber);
        Assert.assertArrayEquals(headerBuf.array(), ((Workout.WorkoutData.Response) packet).rawHeader);
        Assert.assertArrayEquals(dataBuf.array(), ((Workout.WorkoutData.Response) packet).rawData);

        Assert.assertEquals(0x0102, ((Workout.WorkoutData.Response) packet).header.workoutNumber);
        Assert.assertEquals(0x0304, ((Workout.WorkoutData.Response) packet).header.dataNumber);
        Assert.assertEquals(0x05060708, ((Workout.WorkoutData.Response) packet).header.timestamp);
        Assert.assertEquals(0x09, ((Workout.WorkoutData.Response) packet).header.interval);
        Assert.assertEquals(0x0002, ((Workout.WorkoutData.Response) packet).header.dataCount);
        Assert.assertEquals(0x0f, ((Workout.WorkoutData.Response) packet).header.dataLength);
        Assert.assertEquals(0x0042, ((Workout.WorkoutData.Response) packet).header.bitmap);

        Assert.assertEquals(2, ((Workout.WorkoutData.Response) packet).dataList.size());

        Assert.assertNull(((Workout.WorkoutData.Response) packet).dataList.get(0).unknownData);
        Assert.assertEquals(0x0a0b, ((Workout.WorkoutData.Response) packet).dataList.get(0).speed);
        Assert.assertEquals(0x0c0d, ((Workout.WorkoutData.Response) packet).dataList.get(0).cadence);
        Assert.assertEquals(0x0e0f, ((Workout.WorkoutData.Response) packet).dataList.get(0).stepLength);
        Assert.assertEquals(0x1011, ((Workout.WorkoutData.Response) packet).dataList.get(0).groundContactTime);
        Assert.assertEquals(0x12, ((Workout.WorkoutData.Response) packet).dataList.get(0).impact);
        Assert.assertEquals(0x1314, ((Workout.WorkoutData.Response) packet).dataList.get(0).swingAngle);
        Assert.assertEquals(0x15, ((Workout.WorkoutData.Response) packet).dataList.get(0).foreFootLanding);
        Assert.assertEquals(0x16, ((Workout.WorkoutData.Response) packet).dataList.get(0).midFootLanding);
        Assert.assertEquals(0x17, ((Workout.WorkoutData.Response) packet).dataList.get(0).backFootLanding);
        Assert.assertEquals(0x18, ((Workout.WorkoutData.Response) packet).dataList.get(0).eversionAngle);

        Assert.assertNull(((Workout.WorkoutData.Response) packet).dataList.get(1).unknownData);
        Assert.assertEquals(0x191a, ((Workout.WorkoutData.Response) packet).dataList.get(1).speed);
        Assert.assertEquals(0x1b1c, ((Workout.WorkoutData.Response) packet).dataList.get(1).cadence);
        Assert.assertEquals(0x1d1e, ((Workout.WorkoutData.Response) packet).dataList.get(1).stepLength);
        Assert.assertEquals(0x1f20, ((Workout.WorkoutData.Response) packet).dataList.get(1).groundContactTime);
        Assert.assertEquals(0x21, ((Workout.WorkoutData.Response) packet).dataList.get(1).impact);
        Assert.assertEquals(0x2223, ((Workout.WorkoutData.Response) packet).dataList.get(1).swingAngle);
        Assert.assertEquals(0x24, ((Workout.WorkoutData.Response) packet).dataList.get(1).foreFootLanding);
        Assert.assertEquals(0x25, ((Workout.WorkoutData.Response) packet).dataList.get(1).midFootLanding);
        Assert.assertEquals(0x26, ((Workout.WorkoutData.Response) packet).dataList.get(1).backFootLanding);
        Assert.assertEquals(0x27, ((Workout.WorkoutData.Response) packet).dataList.get(1).eversionAngle);
    }

    @Test
    public void testWorkoutPaceRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        short workoutNumber = 0x0102;
        short paceNumber = 0x0304;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, workoutNumber)
                .put(0x08, paceNumber)
        );

        byte[] expected = {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x17, (byte) 0x0c, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x0c, (byte) 0x18, (byte) 0x24, (byte) 0x67, (byte) 0x5e, (byte) 0xe9, (byte) 0x8d, (byte) 0x36, (byte) 0x5f, (byte) 0xde, (byte) 0x1c, (byte) 0x9e, (byte) 0xa0, (byte) 0xd7, (byte) 0x0a, (byte) 0x01, (byte) 0xd3, (byte) 0xce};

        Workout.WorkoutPace.Request request = new Workout.WorkoutPace.Request(secretsProvider, workoutNumber, paceNumber);

        Assert.assertEquals(0x17, request.serviceId);
        Assert.assertEquals(0x0c, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expected, out.get(0));
    }

    @Test
    public void testWorkoutPaceResponse() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.ParseException {
        byte[] raw = {(byte) 0x5a, (byte) 0x00, (byte) 0x4a, (byte) 0x00, (byte) 0x17, (byte) 0x0c, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x30, (byte) 0xe8, (byte) 0xfe, (byte) 0xb9, (byte) 0x27, (byte) 0xa6, (byte) 0xc5, (byte) 0x81, (byte) 0x65, (byte) 0x51, (byte) 0xb8, (byte) 0x24, (byte) 0xfe, (byte) 0x2a, (byte) 0xdc, (byte) 0x3d, (byte) 0x22, (byte) 0xd7, (byte) 0x34, (byte) 0x62, (byte) 0xaf, (byte) 0x06, (byte) 0x5f, (byte) 0xfe, (byte) 0x9c, (byte) 0xe8, (byte) 0xa6, (byte) 0x87, (byte) 0x23, (byte) 0xd6, (byte) 0xc7, (byte) 0x7a, (byte) 0xeb, (byte) 0x07, (byte) 0x06, (byte) 0x5c, (byte) 0x35, (byte) 0xe8, (byte) 0x99, (byte) 0xd3, (byte) 0x96, (byte) 0x0b, (byte) 0x99, (byte) 0x38, (byte) 0x65, (byte) 0x48, (byte) 0xcf, (byte) 0x0f, (byte) 0x99, (byte) 0xe2, (byte) 0x23};

        short workoutNumber = 0x0102;
        short paceNumber = 0x0304;

        short distance1 = 0x0506;
        byte type1 = 0x07;
        int pace1 = 0x08090a0b;

        short distance2 = 0x0c0d;
        byte type2 = 0x0e;
        int pace2 = 0x0f101112;
        short correction = 0x1314;

        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, new HuaweiTLV()
                .put(0x02, workoutNumber)
                .put(0x08, paceNumber)
                .put(0x83, new HuaweiTLV()
                        .put(0x04, distance1)
                        .put(0x05, type1)
                        .put(0x06, pace1)
                )
                .put(0x83, new HuaweiTLV()
                        .put(0x04, distance2)
                        .put(0x05, type2)
                        .put(0x06, pace2)
                        .put(0x09, correction)
                )
        );

        HuaweiPacket packet = new HuaweiPacket(secretsProvider).parse(raw);
        packet.parseTlv();

        Assert.assertEquals(0x17, packet.serviceId);
        Assert.assertEquals(0x0c, packet.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(packet));
        Assert.assertTrue(packet.complete);
        Assert.assertTrue(packet instanceof Workout.WorkoutPace.Response);
        Assert.assertEquals(0x0102, ((Workout.WorkoutPace.Response) packet).workoutNumber);
        Assert.assertEquals(0x0304, ((Workout.WorkoutPace.Response) packet).paceNumber);
        Assert.assertEquals(2, ((Workout.WorkoutPace.Response) packet).blocks.size());

        Assert.assertEquals(0x0506, ((Workout.WorkoutPace.Response) packet).blocks.get(0).distance);
        Assert.assertEquals(0x07, ((Workout.WorkoutPace.Response) packet).blocks.get(0).type);
        Assert.assertEquals(0x08090a0b, ((Workout.WorkoutPace.Response) packet).blocks.get(0).pace);
        Assert.assertEquals(0, ((Workout.WorkoutPace.Response) packet).blocks.get(0).correction);

        Assert.assertEquals(0x0c0d, ((Workout.WorkoutPace.Response) packet).blocks.get(1).distance);
        Assert.assertEquals(0x0e, ((Workout.WorkoutPace.Response) packet).blocks.get(1).type);
        Assert.assertEquals(0x0f101112, ((Workout.WorkoutPace.Response) packet).blocks.get(1).pace);
        Assert.assertEquals(0x1314, ((Workout.WorkoutPace.Response) packet).blocks.get(1).correction);
    }
}
