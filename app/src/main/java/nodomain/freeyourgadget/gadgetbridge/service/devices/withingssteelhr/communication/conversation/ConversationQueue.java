/*  Copyright (C) 2021 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.conversation;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.WithingsSteelHRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.WithingsUUID;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message.Message;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ConversationQueue implements ConversationObserver
{
    private static final Logger logger = LoggerFactory.getLogger(WithingsSteelHRDeviceSupport.class);
    private final LinkedList<Conversation> queue = new LinkedList<>();
    private WithingsSteelHRDeviceSupport support;

    public ConversationQueue(WithingsSteelHRDeviceSupport support) {
        this.support = support;
    }

    @Override
    public void onConversationCompleted(short conversationType) {
        queue.remove(getConversation(conversationType));
        send();
    }

    public void clear() {
        queue.clear();
    }

    public void send() {
        logger.debug("Sending of queued messages has been requested.");
        if (!queue.isEmpty()) {
            Conversation nextInLine = queue.peek();
            if (nextInLine!= null) {
                logger.debug("Sending next queued message.");
                Message request = nextInLine.getRequest();
                support.sendToDevice(request);
            }
        }
    }

    public void addConversation(Conversation conversation) {
        if (conversation == null) {
            return;
        }

        if (conversation.getRequest().needsResponse() || conversation.getRequest().needsEOT()) {
            queue.add(conversation);
            conversation.registerObserver(this);
        } else {
            support.sendToDevice(conversation.getRequest());
        }
    }

    public void processResponse(Message response) {
        Conversation conversation = getConversation(response.getType());
        if (conversation != null) {
            conversation.handleResponse(response);
        }
    }

    private Conversation getConversation(short requestType) {
        for (Conversation conversation : queue) {
            if (conversation.getRequest() != null && conversation.getRequest().getType() == requestType) {
                return conversation;
            }
        }

        return null;
    }
}
