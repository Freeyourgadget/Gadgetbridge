/*  Copyright (C) 2015-2017 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;

public class AppMessageHandlerGBPebble extends AppMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AppMessageHandlerMisfit.class);

    private static final int KEY_FIND_PHONE_START = 1;
    private static final int KEY_FIND_PHONE_STOP = 2;


    AppMessageHandlerGBPebble(UUID uuid, PebbleProtocol pebbleProtocol) {
        super(uuid, pebbleProtocol);
    }

    @Override
    public GBDeviceEvent[] handleMessage(ArrayList<Pair<Integer, Object>> pairs) {
        GBDeviceEventFindPhone gbDeviceEventFindPhone = null;

        for (Pair<Integer, Object> pair : pairs) {
            switch (pair.first) {
                case KEY_FIND_PHONE_START:
                    LOG.info("find phone start");
                    gbDeviceEventFindPhone = new GBDeviceEventFindPhone();
                    gbDeviceEventFindPhone.event = GBDeviceEventFindPhone.Event.START;
                    break;
                case KEY_FIND_PHONE_STOP:
                    LOG.info("find phone stop");
                    gbDeviceEventFindPhone = new GBDeviceEventFindPhone();
                    gbDeviceEventFindPhone.event = GBDeviceEventFindPhone.Event.STOP;
                    break;
                default:
                    LOG.info("unhandled key: " + pair.first);
                    break;
            }
        }

        // always ack
        GBDeviceEventSendBytes sendBytesAck = new GBDeviceEventSendBytes();
        sendBytesAck.encodedBytes = mPebbleProtocol.encodeApplicationMessageAck(mUUID, mPebbleProtocol.last_id);

        return new GBDeviceEvent[]{sendBytesAck, gbDeviceEventFindPhone};
    }
}