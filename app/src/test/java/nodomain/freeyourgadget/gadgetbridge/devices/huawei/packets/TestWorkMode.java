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

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class TestWorkMode {

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
    public void testSwitchStatusRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlvTrue = new HuaweiTLV()
                .put(0x01, true);
        HuaweiTLV expectedTlvFalse = new HuaweiTLV()
                .put(0x01, false);

        byte[] serializedTrue = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x26, (byte) 0x02, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xcd, (byte) 0x97, (byte) 0x7e, (byte) 0x01, (byte) 0x48, (byte) 0x34, (byte) 0x2a, (byte) 0x48, (byte) 0x58, (byte) 0x0d, (byte) 0x30, (byte) 0xc7, (byte) 0xbc, (byte) 0x2e, (byte) 0x40, (byte) 0xd4, (byte) 0x5c, (byte) 0x5a};
        byte[] serializedFalse = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x26, (byte) 0x02, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x28, (byte) 0x00, (byte) 0x99, (byte) 0x6f, (byte) 0x2a, (byte) 0xcb, (byte) 0x62, (byte) 0x3a, (byte) 0xe6, (byte) 0x54, (byte) 0x28, (byte) 0x54, (byte) 0xf8, (byte) 0xab, (byte) 0x54, (byte) 0x83, (byte) 0x45, (byte) 0x68};

        WorkMode.SwitchStatusRequest requestTrue = new WorkMode.SwitchStatusRequest(secretsProvider, true);
        WorkMode.SwitchStatusRequest requestFalse = new WorkMode.SwitchStatusRequest(secretsProvider, false);

        Assert.assertEquals(0x26, requestTrue.serviceId);
        Assert.assertEquals(0x02, requestTrue.commandId);
        Assert.assertEquals(expectedTlvTrue, tlvField.get(requestTrue));
        Assert.assertTrue(requestTrue.complete);
        List<byte[]> outTrue = requestTrue.serialize();
        Assert.assertEquals(1, outTrue.size());
        Assert.assertArrayEquals(serializedTrue, outTrue.get(0));

        Assert.assertEquals(0x26, requestFalse.serviceId);
        Assert.assertEquals(0x02, requestFalse.commandId);
        Assert.assertEquals(expectedTlvFalse, tlvField.get(requestFalse));
        Assert.assertTrue(requestFalse.complete);
        List<byte[]> outFalse = requestFalse.serialize();
        Assert.assertEquals(1, outFalse.size());
        Assert.assertArrayEquals(serializedFalse, outFalse.get(0));
    }
}
