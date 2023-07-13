/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.VivomoveHrCommunicator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.GncsDataSourceMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.GncsDataSourceResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public class GncsDataSourceQueue {
    private static final Logger LOG = LoggerFactory.getLogger(GncsDataSourceQueue.class);

    private final VivomoveHrCommunicator communicator;
    private final int maxPacketSize;
    private final Queue<byte[]> queue = new LinkedList<>();

    private byte[] currentPacket;
    private int currentDataOffset;
    private int lastSentSize;

    public GncsDataSourceQueue(VivomoveHrCommunicator communicator, int maxPacketSize) {
        this.communicator = communicator;
        this.maxPacketSize = maxPacketSize;
    }

    public void addToQueue(byte[] packet) {
        queue.add(packet);
        checkStartUpload();
    }

    public void responseReceived(GncsDataSourceResponseMessage responseMessage) {
        if (currentPacket == null) {
            LOG.error("Unexpected GNCS data source response, no current packet");
            return;
        }
        switch (responseMessage.response) {
            case GncsDataSourceResponseMessage.RESPONSE_TRANSFER_SUCCESSFUL:
                LOG.debug("Confirmed {}B@{} GNCS transfer", lastSentSize, currentDataOffset);
                currentDataOffset += lastSentSize;
                if (currentDataOffset >= currentPacket.length) {
                    LOG.debug("ANCS packet transfer done");
                    currentPacket = null;
                    checkStartUpload();
                } else {
                    sendNextMessage();
                }
                break;

            case GncsDataSourceResponseMessage.RESPONSE_RESEND_LAST_DATA_PACKET:
                LOG.info("Received RESEND_LAST_DATA_PACKET GNCS response");
                sendNextMessage();
                break;

            case GncsDataSourceResponseMessage.RESPONSE_ABORT_REQUEST:
                LOG.info("Received RESPONSE_ABORT_REQUEST GNCS response");
                currentPacket = null;
                checkStartUpload();
                break;

            case GncsDataSourceResponseMessage.RESPONSE_ERROR_CRC_MISMATCH:
            case GncsDataSourceResponseMessage.RESPONSE_ERROR_DATA_OFFSET_MISMATCH:
            default:
                LOG.error("Received {} GNCS response", responseMessage.response);
                currentPacket = null;
                checkStartUpload();
                break;
        }
    }

    private void checkStartUpload() {
        if (currentPacket != null) {
            LOG.debug("Another upload is still running");
            return;
        }
        if (queue.isEmpty()) {
            LOG.debug("Nothing in queue");
            return;
        }
        startNextUpload();
    }

    private void startNextUpload() {
        currentPacket = queue.remove();
        currentDataOffset = 0;
        LOG.debug("Sending {}B ANCS data", currentPacket.length);
        sendNextMessage();
    }

    private void sendNextMessage() {
        final int remainingSize = currentPacket.length - currentDataOffset;
        final int availableSize = Math.min(remainingSize, maxPacketSize);
        communicator.sendMessage(new GncsDataSourceMessage(currentPacket, currentDataOffset, Math.min(remainingSize, maxPacketSize)).packet);
        lastSentSize = availableSize;
    }
}
