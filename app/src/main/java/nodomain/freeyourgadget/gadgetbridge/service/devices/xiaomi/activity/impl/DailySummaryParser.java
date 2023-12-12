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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DailySummaryParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(DailySummaryParser.class);

    @Override
    public boolean parse(final XiaomiSupport support, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        switch (version) {
            case 5:
                headerSize = 4;
                break;
            default:
                LOG.warn("Unable to parse daily summary version {}", fileId.getVersion());
                return false;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Header: {}", GB.hexdump(header));

        final int steps = buf.getInt();
        final int unk1 = buf.get() & 0xff; // 0
        final int unk2 = buf.get() & 0xff; // 0
        final int unk3 = buf.get() & 0xff; // 0
        final int hrResting = buf.get() & 0xff;
        final int hrMax = buf.get() & 0xff;
        final int hrMaxTs = buf.getInt();
        final int hrMin = buf.get() & 0xff;
        final int hrMinTs = buf.getInt();
        final int hrAvg = buf.get() & 0xff;
        final int stressAvg = buf.get() & 0xff;
        final int stressMax = buf.get() & 0xff;
        final int stressMin = buf.get() & 0xff;
        final int unk4 = buf.get() & 0xff; // 0
        final int unk5 = buf.get() & 0xff; // 0
        final int unk6 = buf.get() & 0xff; // 0
        final int calories = buf.getShort();
        final int unk7 = buf.get() & 0xff; // 0
        final int unk8 = buf.get() & 0xff; // 0
        final int unk9 = buf.get() & 0xff; // 0
        final int spo2Max = buf.get() & 0xff;
        final int spo2MaxTs = buf.getInt();
        final int spo2Min = buf.get() & 0xff;
        final int spo2MinTs = buf.getInt();
        final int spo2Avg = buf.get() & 0xff;
        final int trainingLoadMaybe1 = buf.getShort();
        final int trainingLoadMaybe2 = buf.getShort();
        final int trainingLoadMaybe3 = buf.get() & 0xff;

        // TODO vitality somewhere?
        // TODO persist everything

        LOG.warn("Persisting daily summary is not implemented");

        return true;
    }
}
