package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetSkinTemperatureMeasurement extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SetSkinTemperatureMeasurement.class);

    public SetSkinTemperatureMeasurement(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.SkinTemperatureMeasurement.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        boolean temperatureSwitch = GBApplication
                .getDeviceSpecificSharedPrefs(this.getDevice().getAddress())
                .getBoolean(HuaweiConstants.PREF_HUAWEI_CONTINUOUS_SKIN_TEMPERATURE_MEASUREMENT, false);
        try {
            return new FitnessData.SkinTemperatureMeasurement.Request(paramsProvider, temperatureSwitch).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Set SkinTemperatureMeasurement");
    }
}
