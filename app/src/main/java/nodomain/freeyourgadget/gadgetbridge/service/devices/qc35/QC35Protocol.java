/*  Copyright (C) 2021-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qc35;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class QC35Protocol extends GBDeviceProtocol {
    Logger logger = LoggerFactory.getLogger(getClass());
    protected QC35Protocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        logger.debug("response: {}", StringUtils.bytesToHex(responseData));

        ArrayList<GBDeviceEvent> events = new ArrayList<>();

        ByteBuffer buffer = ByteBuffer.wrap(responseData);
        while(buffer.remaining() > 0){
            int first = buffer.get();
            int second = buffer.get();
            int third = buffer.get();
            int length = buffer.get();
            byte[] data = new byte[length];
            buffer.get(data);
            if(first == 0x02){
                if(second == 0x02){
                    if(third == 0x03){
                        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                        batteryInfo.level = data[0];
                        batteryInfo.state = BatteryState.BATTERY_NORMAL;
                        events.add(batteryInfo);
                    }
                }
            }
        }

        return events.toArray(new GBDeviceEvent[0]);
    }

    @Override
    public byte[] encodeTestNewFunction() {
        return new byte[]{0x02, 0x02, 0x01, 0x00};
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        if(config.equals(DeviceSettingsPreferenceConst.PREF_QC35_NOISE_CANCELLING_LEVEL)){
            int level = prefs.getInt(config, 0);
            if(level == 2){
                level = 1;
            }else if(level == 1){
                level = 3;
            }
            return new byte[]{0x01, 0x06, 0x02, 0x01, (byte) level};
        }

        return null;
    }
}
