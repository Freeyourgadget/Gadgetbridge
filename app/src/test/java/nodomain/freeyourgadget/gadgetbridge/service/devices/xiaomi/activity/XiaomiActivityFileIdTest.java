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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiActivityFileIdTest {
    @Test
    public void testEncode() {
        final byte[] expectedEncoding = GB.hexStringToByteArray("21F3286504008C");
        final XiaomiActivityFileId xiaomiActivityFileId = new XiaomiActivityFileId(
                new Date(1697182497000L),
                4,
                3,
                1,
                8,
                0
        );

        assertArrayEquals(expectedEncoding, xiaomiActivityFileId.toBytes());
    }

    @Test
    public void testDecode() {
        final byte[] bytes = GB.hexStringToByteArray("21F328650403A0");
        final XiaomiActivityFileId expectedFileId = XiaomiActivityFileId.from(bytes);

        assertEquals(1697182497000L, expectedFileId.getTimestamp().getTime());
        assertEquals(4, expectedFileId.getTimezone());
        assertEquals(3, expectedFileId.getVersion());
        assertEquals(XiaomiActivityFileId.Type.SPORTS, expectedFileId.getType());
        assertEquals(XiaomiActivityFileId.Subtype.SPORTS_FREESTYLE, expectedFileId.getSubtype());
        assertEquals(XiaomiActivityFileId.DetailType.DETAILS, expectedFileId.getDetailType());
    }

    @Test
    public void testDecodeEncode() {
        final byte[] bytes = GB.hexStringToByteArray("21F328650403A021F3286504008C");

        final ByteBuffer bufDecode = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        final List<XiaomiActivityFileId> fileIds = new ArrayList<>();

        while (bufDecode.position() < bufDecode.limit()) {
            final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(bufDecode);
            fileIds.add(fileId);
            System.out.println(fileId);
        }

        final ByteBuffer bufEncode = ByteBuffer.allocate(fileIds.size() * 7).order(ByteOrder.LITTLE_ENDIAN);

        for (final XiaomiActivityFileId fileId : fileIds) {
            bufEncode.put(fileId.toBytes());
        }

        assertArrayEquals(bytes, bufEncode.array());
    }

    @Test
    public void testZero() {
        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(new byte[]{0, 0, 0, 0, 0, 0, 0});
        assertTrue(fileId.getTimestamp().getTime() == 0 && fileId.getVersion() == 0);
    }
}
