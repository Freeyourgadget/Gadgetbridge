package nodomain.freeyourgadget.gadgetbridge.devices.huawei.freebuds5i;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiFreebudsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFreebudsSupport;

public class HuaweiFreebuds5iCoordinator extends HuaweiFreebudsCoordinator {

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("huawei freebuds 5i.*", Pattern.CASE_INSENSITIVE);
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return HuaweiFreebudsSupport.class;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HUAWEI_FREEBUDS5I;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_huawei_freebuds_5i;
    }
}
