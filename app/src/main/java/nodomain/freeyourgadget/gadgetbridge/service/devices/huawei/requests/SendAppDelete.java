package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendAppDelete extends Request {
    private final String packageName;
    public SendAppDelete(HuaweiSupportProvider support, String packageName) {
        super(support);
        this.serviceId = App.id;
        this.commandId = App.AppDelete.id;
        this.packageName = packageName;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new App.AppDelete.Request(this.paramsProvider, this.packageName).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

}

