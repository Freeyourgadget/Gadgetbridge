/*  Copyright (C) 2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele Gobbetti

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class DatalogSessionAnalytics extends DatalogSession {
    private static final Logger LOG = LoggerFactory.getLogger(DatalogSessionAnalytics.class);
    private GBDeviceEventBatteryInfo mGBDeviceEventBatteryInfo = new GBDeviceEventBatteryInfo();

    DatalogSessionAnalytics(byte id, UUID uuid, int timestamp, int tag, byte itemType, short itemSize, GBDevice device) {
        super(id, uuid, timestamp, tag, itemType, itemSize);
        // The default notification should not be too bad (one per hour) but we can override this if needed
        //mGBDevice.setBatteryThresholdPercent((short) 5);

        taginfo = "(analytics - " + tag + ")";
    }

    @Override
    GBDeviceEvent[] handleMessage(ByteBuffer datalogMessage, int length) {
        LOG.info("DATALOG " + taginfo + GB.hexdump(datalogMessage.array(), datalogMessage.position(), length));

        datalogMessage.position(datalogMessage.position() + 3);
        int messageTS = datalogMessage.getInt();

        datalogMessage.position(datalogMessage.position() + 12);
        short reportedMilliVolts = datalogMessage.getShort();

        datalogMessage.position(datalogMessage.position() + 2);
        byte reportedPercentage = datalogMessage.get();

        LOG.info("Battery reading for TS " + messageTS + " is: " + reportedMilliVolts + " milliVolts, percentage: " + reportedPercentage);
        if (messageTS > 0 && reportedMilliVolts < 5000) { //some safety checks
            mGBDeviceEventBatteryInfo.state = BatteryState.BATTERY_NORMAL;
            mGBDeviceEventBatteryInfo.level = reportedPercentage;

            return new GBDeviceEvent[]{mGBDeviceEventBatteryInfo, null};
        } else { //invalid data, but we ack nevertheless
            return new GBDeviceEvent[]{null};
        }

    }
}
