package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetAppNames extends Request{
    public GetAppNames(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = App.id;
        this.commandId = App.AppNames.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new App.AppNames.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof App.AppNames.Response))
            throw new ResponseTypeMismatchException(receivedPacket, App.AppNames.Response.class);

        App.AppNames.Response resp = (App.AppNames.Response)(receivedPacket);
        supportProvider.getHuaweiAppManager().setInstalledAppList(resp.appInfoList);
    }
}
