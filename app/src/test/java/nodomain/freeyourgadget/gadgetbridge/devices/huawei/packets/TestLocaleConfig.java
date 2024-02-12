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

public class TestLocaleConfig {

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
    public void testSetLocaleRequest() throws NoSuchFieldException, IllegalAccessException, HuaweiPacket.CryptoException {
        Field tlvField = HuaweiPacket.class.getDeclaredField("tlv");
        tlvField.setAccessible(true);

        HuaweiTLV expectedTlv = new HuaweiTLV()
                .put(0x01, new byte[] {0x45, 0x4e, 0x2d, 0x47, 0x42})
                .put(0x02, (byte) 0x00);

        byte[] serialized = new byte[] {(byte) 0x5a, (byte) 0x00, (byte) 0x2a, (byte) 0x00, (byte) 0x0c, (byte) 0x01, (byte) 0x7c, (byte) 0x01, (byte) 0x01, (byte) 0x7d, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7e, (byte) 0x10, (byte) 0x4e, (byte) 0xb0, (byte) 0x71, (byte) 0x05, (byte) 0x7b, (byte) 0xf1, (byte) 0x07, (byte) 0x31, (byte) 0xc4, (byte) 0x6c, (byte) 0x5b, (byte) 0x6d, (byte) 0xbf, (byte) 0x07, (byte) 0xf5, (byte) 0x55, (byte) 0x65, (byte) 0x06};

        LocaleConfig.SetLanguageSetting request = new LocaleConfig.SetLanguageSetting(
                paramsProvider,
                new byte[] {0x45, 0x4e, 0x2d, 0x47, 0x42},
                (byte) 0x00
        );

        Assert.assertEquals(0x0c, request.serviceId);
        Assert.assertEquals(0x01, request.commandId);
        Assert.assertEquals(expectedTlv, tlvField.get(request));
        Assert.assertTrue(request.complete);
        List<byte[]> out = request.serialize();
        Assert.assertEquals(1, out.size());
        Assert.assertArrayEquals(serialized, out.get(0));
    }
}
