/*  Copyright (C) 2024 Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import org.junit.Assert;
import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class TestHuaweiTruSleepParser {

    @Test
    public void testParseState() {
        byte[] test = GB.hexStringToByteArray("0100000002000000000000000000000003000000050000000000000000000000");

        HuaweiTruSleepParser.TruSleepStatus[] expected = new HuaweiTruSleepParser.TruSleepStatus[] {
                new HuaweiTruSleepParser.TruSleepStatus(1, 2),
                new HuaweiTruSleepParser.TruSleepStatus(3, 5)
        };

        HuaweiTruSleepParser.TruSleepStatus[] result = HuaweiTruSleepParser.parseState(test);

        Assert.assertArrayEquals(expected, result);
    }
}
