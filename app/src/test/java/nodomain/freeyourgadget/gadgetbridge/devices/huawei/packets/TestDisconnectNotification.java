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

public class TestDisconnectNotification {

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
    public void testBluetoothDisconnectNotificationSetting() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlvEnable = new HuaweiTLV()
                .put(0x01, true);

        byte[] serializedEnable = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x0b, (byte) 0x03, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0xcd, (byte) 0x97, (byte) 0x7e, (byte) 0x01, (byte) 0x48, (byte) 0x34, (byte) 0x2a, (byte) 0x48, (byte) 0x58, (byte) 0x0d, (byte) 0x30, (byte) 0xc7, (byte) 0xbc, (byte) 0x2e, (byte) 0x40, (byte) 0xd4, (byte) 0x20, (byte) 0xaf};

        DisconnectNotification.DisconnectNotificationSetting.Request requestEnable = new DisconnectNotification.DisconnectNotificationSetting.Request(
                secretsProvider,
                true
        );

        Assert.assertEquals(0x0b, requestEnable.serviceId);
        Assert.assertEquals(0x03, requestEnable.commandId);
        Assert.assertEquals(expectedTlvEnable, tlvField.get(requestEnable));
        Assert.assertTrue(requestEnable.complete);
        List<byte[]> outEnable = requestEnable.serialize();
        Assert.assertEquals(1, outEnable.size());
        Assert.assertArrayEquals(serializedEnable, outEnable.get(0));


        HuaweiTLV expectedTlvDisable = new HuaweiTLV()
                .put(0x01, false);

        byte[] serializedDisable = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x0b, (byte) 0x03, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x28, (byte) 0x00, (byte) 0x99, (byte) 0x6f, (byte) 0x2a, (byte) 0xcb, (byte) 0x62, (byte) 0x3a, (byte) 0xe6, (byte) 0x54, (byte) 0x28, (byte) 0x54, (byte) 0xf8, (byte) 0xab, (byte) 0x54, (byte) 0x83, (byte) 0x39, (byte) 0x9D};

        DisconnectNotification.DisconnectNotificationSetting.Request requestDisable = new DisconnectNotification.DisconnectNotificationSetting.Request(
                secretsProvider,
                false
        );

        Assert.assertEquals(0x0b, requestDisable.serviceId);
        Assert.assertEquals(0x03, requestDisable.commandId);
        Assert.assertEquals(expectedTlvDisable, tlvField.get(requestDisable));
        Assert.assertTrue(requestDisable.complete);
        List<byte[]> outDisable = requestDisable.serialize();
        Assert.assertEquals(1, outDisable.size());
        Assert.assertArrayEquals(serializedDisable, outDisable.get(0));
    }
}
