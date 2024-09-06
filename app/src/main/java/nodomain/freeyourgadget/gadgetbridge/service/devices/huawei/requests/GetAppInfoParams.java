package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetAppInfoParams extends Request{

    public GetAppInfoParams(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = App.id;
        this.commandId = App.AppInfoParams.id;

    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsAppParams();
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new App.AppInfoParams.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof App.AppInfoParams.Response))
            throw new ResponseTypeMismatchException(receivedPacket, App.AppInfoParams.Response.class);

        App.AppInfoParams.Response resp = (App.AppInfoParams.Response)(receivedPacket);
        supportProvider.getHuaweiCoordinator().setAppDeviceParams(resp.params);
    }
}
