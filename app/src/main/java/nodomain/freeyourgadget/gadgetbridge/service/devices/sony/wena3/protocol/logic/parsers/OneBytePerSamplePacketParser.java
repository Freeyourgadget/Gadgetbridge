/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers;

import java.nio.ByteBuffer;

public class OneBytePerSamplePacketParser extends LinearSamplePacketParser<Integer> {
    public static final int ONE_MINUTE_IN_MS = 60_000; // 1 sample per minute it seems

    public OneBytePerSamplePacketParser(int headerMarker, int sampleDistanceInMs) {
        super(headerMarker, sampleDistanceInMs);
    }

    @Override
    Integer takeSampleFromBuffer(ByteBuffer buffer) {
        return Integer.valueOf(buffer.get() & 0xFF);
    }

    @Override
    boolean canTakeSampleFromBuffer(ByteBuffer buffer) {
        return buffer.remaining() > 0;
    }
}
