package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Contacts;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetContactsCount extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetContactsCount.class);

    public GetContactsCount(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Contacts.id;
        this.commandId = Contacts.ContactsCount.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Contacts.ContactsCount.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle contacts count");

        if (!(receivedPacket instanceof Contacts.ContactsCount.Response))
            throw new ResponseTypeMismatchException(receivedPacket, Contacts.ContactsCount.Response.class);

        int count = ((Contacts.ContactsCount.Response) receivedPacket).maxCount;
        this.supportProvider.getHuaweiCoordinator().saveMaxContactsCount(count);
    }
}
