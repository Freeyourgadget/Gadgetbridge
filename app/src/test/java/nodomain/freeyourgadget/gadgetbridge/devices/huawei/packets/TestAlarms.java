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

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

public class TestAlarms {

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
    public void testEventAlarmsRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        Field alarmField = Alarms.EventAlarmsRequest.class.getDeclaredField("alarms");
        alarmField.setAccessible(true);

        HuaweiTLV expectedAlarmsTlv = new HuaweiTLV();

        Alarms.EventAlarmsRequest request = new Alarms.EventAlarmsRequest(paramsProvider);

        expectedAlarmsTlv.put(0x82, new HuaweiTLV()
                .put(0x03, (byte) 1)
                .put(0x04, true)
                .put(0x05, (short) 0x1337)
                .put(0x06, (byte) 0)
                .put(0x07, "Alarm1")
        );

        request.addEventAlarm(
                new Alarms.EventAlarm(
                        (byte) 1,
                        true,
                        (byte) 0x13,
                        (byte) 0x37,
                        (byte) 0,
                        "Alarm1"
                )
        );

        Assert.assertEquals(0x08, request.serviceId);
        Assert.assertEquals(0x01, request.commandId);
        // TODO: check count in request
        Assert.assertEquals(expectedAlarmsTlv, alarmField.get(request));

        // A serialize will change the tlv, so we cannot test it here

        expectedAlarmsTlv.put(0x82,new HuaweiTLV()
                .put(0x03, (byte) 2)
                .put(0x04, false)
                .put(0x05, (short) 0xCAFE)
                .put(0x06, (byte) 1)
                .put(0x07, "Alarm2")
        );

        request.addEventAlarm(
                new Alarms.EventAlarm(
                        (byte) 2,
                        false,
                        (byte) 0xCA,
                        (byte) 0xFE,
                        (byte) 1,
                        "Alarm2"
                )
        );

        Assert.assertEquals(expectedAlarmsTlv, alarmField.get(request));

        HuaweiTLV expectedTlv = new HuaweiTLV().put(0x81, expectedAlarmsTlv);

        // Different order for better assertion messages in case of failure
        List<byte[]> listOut = request.serialize();
        Assert.assertEquals(1, listOut.size());
        Assert.assertEquals(expectedAlarmsTlv, alarmField.get(request));
        Assert.assertEquals(expectedTlv, tlvField.get(request));
    }

    @Test
    public void testSmartAlarmRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        byte[] expectedOutput = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x08, (byte) 0x02, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x20, (byte) 0xcd, (byte) 0x7f, (byte) 0x80, (byte) 0x67, (byte) 0x02, (byte) 0x8d, (byte) 0x46, (byte) 0xfb, (byte) 0xc1, (byte) 0x0b, (byte) 0xed, (byte) 0x6c, (byte) 0x46, (byte) 0xb7, (byte) 0x59, (byte) 0xba, (byte) 0x08, (byte) 0xfd, (byte) 0xde, (byte) 0x3b, (byte) 0xee, (byte) 0x54, (byte) 0xbd, (byte) 0x4f, (byte) 0x27, (byte) 0xf6, (byte) 0x52, (byte) 0x9a, (byte) 0xae, (byte) 0xbf, (byte) 0x55, (byte) 0xd9, (byte) 0xe0, (byte) 0xa6};

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x81, new HuaweiTLV()
                        .put(0x82, new HuaweiTLV()
                                .put(0x03, (byte) 0x01)
                                .put(0x04, true)
                                .put(0x05, (short) 0x1337)
                                .put(0x06, (byte) 1)
                                .put(0x07, (byte) 2)
                        )
                );

        Alarms.SmartAlarmRequest request = new Alarms.SmartAlarmRequest(
                paramsProvider,
                new Alarms.SmartAlarm(
                        true,
                        (byte) 0x13,
                        (byte) 0x37,
                        (byte) 1,
                        (byte) 2
                )
        );

        Assert.assertEquals(0x08, request.serviceId);
        Assert.assertEquals(0x02, request.commandId);
        Assert.assertTrue(request.complete);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(expectedOutput, out.get(0));
    }
}
