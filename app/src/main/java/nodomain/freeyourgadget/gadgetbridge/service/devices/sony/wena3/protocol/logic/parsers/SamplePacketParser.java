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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.ActivityPacketParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.ActivitySyncDataPacket;

abstract class SamplePacketParser<Sample> implements ActivityPacketParser {
    private static final Logger LOG = LoggerFactory.getLogger(SamplePacketParser.class);
    private final int headerMarker;
    public List<Sample> accumulator = new ArrayList<>();
    private enum State {
        READY, RECEIVING, FINISHED
    }
    private State currentState = State.READY;

    public SamplePacketParser(int headerMarker) {
        this.headerMarker = headerMarker;
        reset();
    }

    @Override
    public void reset() {
        accumulator = new ArrayList<>();
        currentState = State.READY;
    }
    @Override
    public boolean parseHeader(ActivitySyncDataPacket packet, GBDevice sourceDevice) {
        assert packet.isCrcValid;
        assert packet.type == ActivitySyncDataPacket.PacketType.HEADER;

        ByteBuffer buf = packet.dataBuffer();

        int type = buf.get();
        if(type != this.headerMarker) {
            LOG.debug("Received ASDP with marker "+type+", not expected type");
            return false;
        }
        if(currentState != State.READY)
            return false;

        if(!tryExtractingMetadataFromHeaderBuffer(buf)) {
            return false;
        }

        currentState = State.RECEIVING;
        LOG.info("Ready to receive packets");

        return true;
    }

    @Override
    public void parsePacket(ActivitySyncDataPacket packet, GBDevice sourceDevice) {
        assert currentState == State.RECEIVING;
        assert packet.isCrcValid;
        assert packet.type == ActivitySyncDataPacket.PacketType.DATA;

        ByteBuffer buf = packet.dataBuffer();
        while(canTakeSampleFromBuffer(buf))
            accumulator.add(takeSampleFromBuffer(buf));

        LOG.info("Accumulated "+accumulator.size()+" samples");
    }

    @Override
    public void finishReceiving(GBDevice device) {
        assert currentState == State.RECEIVING;
        currentState = State.FINISHED;

        reset();
    }

    abstract Sample takeSampleFromBuffer(ByteBuffer buffer);
    abstract boolean canTakeSampleFromBuffer(ByteBuffer buffer);
    abstract boolean tryExtractingMetadataFromHeaderBuffer(ByteBuffer buffer);
}
