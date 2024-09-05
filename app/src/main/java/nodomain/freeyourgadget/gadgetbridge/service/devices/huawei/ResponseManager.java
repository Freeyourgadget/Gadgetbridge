/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;

/**
 * Manages all response data.
 */
public class ResponseManager {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseManager.class);

    private final List<Request> handlers = Collections.synchronizedList(new ArrayList<>());
    private HuaweiPacket receivedPacket;
    private final AsynchronousResponse asynchronousResponse;
    private final HuaweiSupportProvider support;

    public ResponseManager(HuaweiSupportProvider support) {
        this.asynchronousResponse = new AsynchronousResponse(support);
        this.support = support;
    }

    /**
     * Add a request to the response handler list
     * @param handler The request to handle responses
     */
    public void addHandler(Request handler) {
        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    /**
     * Remove a request from the response handler list
     * @param handler The request to remove
     */
    public void removeHandler(Request handler) {
        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    /**
     * Remove all requests with specified class from the response handler list
     * @param handlerClass The class of which the requests are removed
     */
    public void removeHandler(Class<?> handlerClass) {
        synchronized (handlers) {
            handlers.removeIf(request -> request.getClass() == handlerClass);
        }
    }

    /**
     * Parses the data into a Huawei Packet.
     * If the packet is complete, it will be handled by the first request that accepts it,
     * or as an asynchronous request otherwise.
     *
     * @param data The received data
     */
    public void handleData(byte[] data) {
        //NOTE: This is a quick fix issue with concatenated packets.
        //TODO: Extract transport related code from packet.
        int left = 0;
        do {
            if(left > 0)
                data = Arrays.copyOfRange(data, data.length - left, data.length);

            try {
                if (receivedPacket == null)
                    receivedPacket = new HuaweiPacket(support.getParamsProvider()).parse(data);
                else
                    receivedPacket = receivedPacket.parse(data);

                left = receivedPacket.getLeft();
            } catch (HuaweiPacket.ParseException e) {
                LOG.error("Packet parse exception", e);

                // Clean up so the next message may be parsed correctly
                this.receivedPacket = null;
                return;
            }

            if (receivedPacket.complete) {
                Request handler = null;
                synchronized (handlers) {
                    for (Request req : handlers) {
                        if (req.handleResponse(receivedPacket)) {
                            handler = req;
                            break;
                        }
                    }
                }

                if (handler == null) {
                    LOG.debug("Service: " + Integer.toHexString(receivedPacket.serviceId & 0xff) + ", command: " + Integer.toHexString(receivedPacket.commandId & 0xff) + ", asynchronous response.");

                    // Asynchronous response
                    asynchronousResponse.handleResponse(receivedPacket);
                } else {
                    LOG.debug("Service: " + Integer.toHexString(receivedPacket.serviceId & 0xff) + ", command: " + Integer.toHexString(receivedPacket.commandId & 0xff) + ", handled by: " + handler.getClass());

                    if (handler.autoRemoveFromResponseHandler()) {
                        synchronized (handlers) {
                            handlers.remove(handler);
                        }
                    }

                    handler.handleResponse();
                }
                receivedPacket = null;
            }
        } while (left > 0);
    }
}
