/*  Copyright (C) 2019-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration;

import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedInterface;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedLookupAndGetRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ConfigurationGetRequest extends FileEncryptedLookupAndGetRequest implements FileEncryptedInterface {
    public ConfigurationGetRequest(FossilHRWatchAdapter adapter) {
        super(FileHandle.CONFIGURATION, adapter);
    }

    @Override
    public void handleFileData(byte[] fileData) {
        byte[] data = new byte[fileData.length - 12 - 4];

        System.arraycopy(fileData, 12, data, 0, data.length);
        log("config file: " + getAdapter().arrayToString(fileData));
        log("config file: " + getAdapter().arrayToString(data));

        GBDevice device = getAdapter().getDeviceSupport().getDevice();

        ConfigurationPutRequest.ConfigItem[] items = ConfigurationPutRequest.parsePayload(data);

        for(ConfigurationPutRequest.ConfigItem item : items){
            if(item instanceof ConfigurationPutRequest.VibrationStrengthConfigItem){
                device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_VIBRATION_STRENGTH, String.valueOf(((ConfigurationPutRequest.VibrationStrengthConfigItem) item).getValue())));
            }else if(item instanceof ConfigurationPutRequest.DailyStepGoalConfigItem){
                device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_STEP_GOAL, String.valueOf(((ConfigurationPutRequest.DailyStepGoalConfigItem) item).getValue())));
            }else if(item instanceof ConfigurationPutRequest.CurrentStepCountConfigItem){
                device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_STEP_COUNT, String.valueOf(((ConfigurationPutRequest.CurrentStepCountConfigItem) item).getValue())));
            }else if(item instanceof ConfigurationPutRequest.TimezoneOffsetConfigItem) {
                device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_TIMEZONE_OFFSET, String.valueOf(((ConfigurationPutRequest.TimezoneOffsetConfigItem) item).getValue())));
            }else if(item instanceof ConfigurationPutRequest.BatteryConfigItem){
                device.setBatteryLevel((short) ((ConfigurationPutRequest.BatteryConfigItem) item).getBatteryPercentage());
                device.setBatteryVoltage(((ConfigurationPutRequest.BatteryConfigItem) item).getBatteryVoltage() / 1000f);
                device.setBatteryThresholdPercent((short) 10);

                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                batteryInfo.level = (short) ((ConfigurationPutRequest.BatteryConfigItem) item).getBatteryPercentage();
                batteryInfo.state = BatteryState.BATTERY_NORMAL;
                getAdapter().getDeviceSupport().handleGBDeviceEvent(batteryInfo);
            }else if(item instanceof ConfigurationPutRequest.HeartRateMeasurementModeItem){
                device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_HEART_RATE_MEASUREMENT_MODE, String.valueOf(((ConfigurationPutRequest.HeartRateMeasurementModeItem) item).getValue())));
            }
        }

        device.sendDeviceUpdateIntent(getAdapter().getContext());
    }

    @Override
    public void handleFileLookupError(FILE_LOOKUP_ERROR error) {
        if(error == FILE_LOOKUP_ERROR.FILE_EMPTY){
            GB.toast("config file empty", Toast.LENGTH_LONG,  GB.ERROR);
        }else{
            throw new RuntimeException("strange lookup stuff");
        }
    }
}
