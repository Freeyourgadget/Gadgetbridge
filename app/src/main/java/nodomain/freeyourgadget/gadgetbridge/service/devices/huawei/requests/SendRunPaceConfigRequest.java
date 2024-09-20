package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendRunPaceConfigRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendRunPaceConfigRequest.class);

    public SendRunPaceConfigRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = FitnessData.RunPaceConfig.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsRunPaceConfig();
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            //Hardcoded value till interface enable threshold values
            return new FitnessData.RunPaceConfig.Request(paramsProvider,
                    0x1C2,
                    0x1A4,
                    0x186,
                    0x168,
                    0x14A,
                    0x12C
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        if (receivedPacket instanceof FitnessData.RunPaceConfig.Response) {
            if (((FitnessData.RunPaceConfig.Response) receivedPacket).isOk) {
                LOG.debug("RunPace set");
            } else {
                LOG.warn("Error set RunPace");
            }
        } else {
            LOG.error("Set RunPace response is not of type RunPaceConfig response");
        }
    }
}
