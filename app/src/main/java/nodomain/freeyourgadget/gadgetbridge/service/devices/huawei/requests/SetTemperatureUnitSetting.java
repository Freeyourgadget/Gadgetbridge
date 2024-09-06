package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.LocaleConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetTemperatureUnitSetting extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetLanguageSettingRequest.class);

    public SetTemperatureUnitSetting(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = LocaleConfig.id;
        this.commandId = LocaleConfig.SetTemperatureUnitSetting.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        String temperatureScale = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress()).getString(DeviceSettingsPreferenceConst.PREF_TEMPERATURE_SCALE_CF, "");
        byte isFahrenheit = (byte) ((temperatureScale.equals("f")) ? 1 : 0);
        try {
            return new LocaleConfig.SetTemperatureUnitSetting(paramsProvider, isFahrenheit).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Set Temperature unit");
    }
}
