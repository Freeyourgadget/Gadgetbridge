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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.ActivitySyncDataPacket;

public class ActivitySyncPacketProcessor {
    private static final int RESET_SEQ_NO = 0xFF;
    private static final int MAX_SEQ_NO = 0x7F;
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySyncPacketProcessor.class);

    private ActivityPacketParser currentParser = null;
    private final List<ActivityPacketParser> parsers = new ArrayList<>();
    private int currentSeqNo = RESET_SEQ_NO;
    private boolean haveHeader = false;

    public ActivitySyncPacketProcessor() {}

    public void registerParser(ActivityPacketParser parser) {
        parsers.add(parser);
    }

    public void receivePacket(ActivitySyncDataPacket packet, GBDevice device) {
        if(packet.sequenceNo == RESET_SEQ_NO) {
            LOG.info("Initial packet received, off we go with a sync!");
            currentSeqNo = RESET_SEQ_NO;
            resetAll();
            // Do NOT return here: the initial packet is still a packet!
        }

        if(packet.sequenceNo != currentSeqNo) {
            LOG.error("There was packet loss (skip "+currentSeqNo+" to "+packet.sequenceNo+")");
            finalizeCurrentParserIfNeeded(device);
            return;
        } else {
            if(currentSeqNo == MAX_SEQ_NO || currentSeqNo == RESET_SEQ_NO) {
                currentSeqNo = 0;
            } else {
                currentSeqNo ++;
            }
        }

        if(!packet.isCrcValid) {
            LOG.error("Received packet has invalid CRC");
            return;
        }

        switch(packet.type) {
            case HEADER:
                haveHeader = true;
                finalizeCurrentParserIfNeeded(device);
                for(ActivityPacketParser parser: parsers) {
                    if(parser.parseHeader(packet, device)) {
                        currentParser = parser;
                        break;
                    }
                }
                if(currentParser == null) {
                    LOG.warn("No parsers can understand " + packet);
                }
                break;

            case DATA:
                if(currentParser != null) {
                    currentParser.parsePacket(packet, device);
                } else {
                    if(!haveHeader) {
                        LOG.warn("DATA arrived before HEADER: dropped " + packet);
                    } else {
                        LOG.warn("No parser known: dropped data packet " + packet);
                    }
                }
                break;

            case FINISH:
                LOG.info("End of transmission received");
                finalizeCurrentParserIfNeeded(device);
                break;
        }
    }

    public void resetAll() {
        currentParser = null;
        for(ActivityPacketParser parser: parsers) parser.reset();
    }

    private void finalizeCurrentParserIfNeeded(GBDevice device) {
        if(currentParser != null) {
            currentParser.finishReceiving(device);
            currentParser = null;
        }
        haveHeader = false;
    }
}
