package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupAndGetRequest;

public class ConfigurationGetRequest extends FileGetRequest {
    public ConfigurationGetRequest(FossilWatchAdapter adapter) {
        super((byte) 8, adapter);
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
            }
        }

        device.sendDeviceUpdateIntent(getAdapter().getContext());

        handleConfigurationLoaded();
    }

    public void handleConfigurationLoaded(){}
}
