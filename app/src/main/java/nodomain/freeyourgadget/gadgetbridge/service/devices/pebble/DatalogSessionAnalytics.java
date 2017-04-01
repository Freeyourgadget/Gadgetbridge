/*  Copyright (C) 2016-2017 Daniele Gobbetti

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
    private GBDevice mGBDevice;

    DatalogSessionAnalytics(byte id, UUID uuid, int timestamp, int tag, byte itemType, short itemSize, GBDevice device) {
        super(id, uuid, timestamp, tag, itemType, itemSize);
        if (mGBDevice == null || !device.equals(mGBDevice)) { //prevent showing information of other pebble watches when switching devices
            mGBDevice = device;
            mGBDeviceEventBatteryInfo.state = BatteryState.UNKNOWN;
        }

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

        LOG.info("Battery reading for TS " + messageTS + " is: " + reportedMilliVolts + " milliVolts, mapped to percentage: " + milliVoltstoPercentage(reportedMilliVolts));

        if (messageTS > 0 && reportedMilliVolts < 5000) { //some safety checks
            mGBDeviceEventBatteryInfo.state = BatteryState.BATTERY_NORMAL;
            mGBDeviceEventBatteryInfo.level = milliVoltstoPercentage(reportedMilliVolts);

            return new GBDeviceEvent[]{mGBDeviceEventBatteryInfo, null};
        } else { //invalid data, but we ack nevertheless
            return new GBDeviceEvent[]{null};
        }

    }

    private short milliVoltstoPercentage(short batteryMilliVolts) {
        if (batteryMilliVolts > 4145) {        //(4146 is still 100, next reported value is already 90)
            return 100;
        } else if (batteryMilliVolts > 4053) { //(4054 is still 90, next reported value is already 80)
            return 90;
        } else if (batteryMilliVolts > 4000) { //guessed
            return 80;
        } else if (batteryMilliVolts > 3890) { //3890 was already 60
            return 70;
        } else if (batteryMilliVolts > 3855) { //probably
            return 60;
        } else if (batteryMilliVolts > 3780) { //3781 is still 50, next reading is 3776 but percentage on pebble unknown
            return 50;
        } else if (batteryMilliVolts >= 3750) { //3750 is still 40, next reported value is 3746 and already 30
            return 40;
        } else if (batteryMilliVolts > 3720) { //3723 is still 30, next reported value is 3719 and already 20
            return 30;
        } else if (batteryMilliVolts > 3680) { //3683 is still 20, next reported value is 3675 and already 10
            return 20;
        } else if (batteryMilliVolts > 3650) { //3657 is still 10
            return 10;
        } else {
            return 0; //or -1 for invalid?
        }
    }
}
