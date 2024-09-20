package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiReportThreshold;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendDeviceReportThreshold extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendDeviceReportThreshold.class);

    public SendDeviceReportThreshold(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.DeviceReportThreshold.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsDeviceReportThreshold();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {

            return new FitnessData.DeviceReportThreshold.Request(paramsProvider, HuaweiReportThreshold.getReportThresholds()).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Send Device Report Threshold");
    }
}
