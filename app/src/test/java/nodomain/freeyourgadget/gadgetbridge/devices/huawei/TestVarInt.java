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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import org.junit.Assert;
import org.junit.Test;

public class TestVarInt {

    private void testValue(int intValue, byte[] bytesValue, int size) {
        Assert.assertEquals(size, VarInt.getVarIntSize(intValue));
        Assert.assertEquals(intValue, VarInt.getVarIntValue(bytesValue, 0));
        Assert.assertArrayEquals(bytesValue, VarInt.putVarIntValue(intValue));

        VarInt varInt = new VarInt(bytesValue, 0);
        Assert.assertEquals(size, varInt.size);
        Assert.assertEquals(intValue, varInt.dValue);
        Assert.assertArrayEquals(bytesValue, varInt.eValue);
    }

    @Test
    public void testZero() {
        testValue(0, new byte[]{0}, 1);
    }

    @Test
    public void testSingleValue() {
        testValue(17, new byte[]{17}, 1);
    }

    @Test
    public void test0x80() {
        // This is 1 << 8, the first 'overflowing' value
        testValue(0x80, new byte[]{(byte) 0x81, 0x00}, 2);
    }

    @Test
    public void testDoubleValue() {
        testValue(460, new byte[]{(byte) 0x83, 0x4C}, 2);
    }

    @Test
    public void testOffset() {
        int intValue = 460;
        byte[] bytesValue = {(byte) 0x83, 0x4C};
        byte[] bytesTest = {0x00, (byte) 0x83, 0x4C};
        int size = 2;

        Assert.assertEquals(size, VarInt.getVarIntSize(intValue));
        Assert.assertEquals(intValue, VarInt.getVarIntValue(bytesTest, 1));
        Assert.assertArrayEquals(bytesValue, VarInt.putVarIntValue(intValue));

        VarInt varInt = new VarInt(bytesTest, 1);
        Assert.assertEquals(size, varInt.size);
        Assert.assertEquals(intValue, varInt.dValue);
        Assert.assertArrayEquals(bytesValue, varInt.eValue);
    }
}
