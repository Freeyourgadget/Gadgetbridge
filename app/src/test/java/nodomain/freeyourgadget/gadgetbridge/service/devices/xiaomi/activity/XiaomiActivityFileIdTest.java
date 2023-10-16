/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiActivityFileIdTest {
    @Test
    public void testEncode() {
        final byte[] bytes = GB.hexStringToByteArray("21F328650403A0");
        final XiaomiActivityFileId xiaomiActivityFileId = new XiaomiActivityFileId(
                new Date(1697182497000L),
                4,
                3,
                1,
                8,
                0
        );

        assertArrayEquals(bytes, xiaomiActivityFileId.toBytes());
    }

    @Test
    public void testDecode() {
        final byte[] bytes = GB.hexStringToByteArray("21F328650403A0");
        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(bytes);

        assertEquals(1697182497000L, fileId.getTimestamp().getTime());
        assertEquals(4, fileId.getTimezone());
        assertEquals(3, fileId.getVersion());
        assertEquals(1, fileId.getType());
        assertEquals(8, fileId.getSubtype());
        assertEquals(0, fileId.getDetailType());
    }
}
