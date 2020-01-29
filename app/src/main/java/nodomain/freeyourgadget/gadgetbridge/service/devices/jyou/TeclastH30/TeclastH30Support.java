/*  Copyright (C) 2017-2020 Andreas Shimokawa, Da Pa, Pavel Elagin, Sami Alaoui

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.TeclastH30;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.jyou.JYouConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.jyou.JYouSupport;

public class TeclastH30Support extends JYouSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TeclastH30Support.class);

    public TeclastH30Support() {
        super(LOG);
        addSupportedService(JYouConstants.UUID_SERVICE_JYOU);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        switch (data[0]) {
            case JYouConstants.RECEIVE_DEVICE_INFO:
                int fwVerNum = data[4] & 0xFF;
                versionCmd.fwVersion = (fwVerNum / 100) + "." + ((fwVerNum % 100) / 10) + "." + ((fwVerNum % 100) % 10);
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case JYouConstants.RECEIVE_BATTERY_LEVEL:
                batteryCmd.level = data[8];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + batteryCmd.level);
                return true;
            case JYouConstants.RECEIVE_STEPS_DATA:
                int steps = ByteBuffer.wrap(data, 5, 4).getInt();
                LOG.info("Number of walked steps: " + steps);
                return true;
            case JYouConstants.RECEIVE_HEARTRATE:
                LOG.info("Current heart rate: " + data[8]);
                return true;
            default:
                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
                return true;
        }
    }

    @Override
    protected void syncSettings(TransactionBuilder builder) {
        syncDateAndTime(builder);

        // TODO: unhardcode and separate stuff
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_HEARTRATE_WARNING_VALUE, 0, 152
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_TARGET_STEPS, 0, 10000
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_GET_STEP_COUNT, 0, 0
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_GET_SLEEP_TIME, 0, 0
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_NOON_TIME, 12 * 60 * 60, 14 * 60 * 60 // 12:00 - 14:00
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_SLEEP_TIME, 21 * 60 * 60, 8 * 60 * 60 // 21:00 - 08:00
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_INACTIVITY_WARNING_TIME, 0, 0
        ));

        // do not disturb and a couple more features
        byte dndStartHour = 22;
        byte dndStartMin = 0;
        byte dndEndHour = 8;
        byte dndEndMin = 0;
        boolean dndToggle = false;
        boolean vibrationToggle = true;
        boolean wakeOnRaiseToggle = true;
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_DND_SETTINGS,
                (dndStartHour << 24) | (dndStartMin << 16) | (dndEndHour << 8) | dndEndMin,
                ((dndToggle ? 0 : 1) << 2) | ((vibrationToggle ? 1 : 0) << 1) | (wakeOnRaiseToggle ? 1 : 0)
        ));
    }

}
