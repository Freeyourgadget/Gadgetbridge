package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;

public class Contacts {
    public static final byte id = 0x03;

    public static class ContactsSet {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, ArrayList<? extends Contact> contacts, int maxCount) {
                super(paramsProvider);

                this.serviceId = Contacts.id;
                this.commandId = id;

                HuaweiTLV contacts_tlv = new HuaweiTLV();
                for(int i = 0; i < maxCount; i++) {
                    HuaweiTLV contact= new HuaweiTLV()
                            .put(0x03, (byte) (i + 1));
                    if(i < contacts.size()) {
                        contact.put(0x4, contacts.get(i).getName())
                                .put(0x85, new HuaweiTLV().put(0x86, new HuaweiTLV().put(0x7, "Mobile").put(0x8, contacts.get(i).getNumber())));
                    }
                    contacts_tlv.put(0x82, contact);
                }

                this.tlv = new HuaweiTLV()
                        .put(0x81, contacts_tlv);

                this.complete = true;
                this.isEncrypted = true;
            }
        }

        public static class Response extends HuaweiPacket {

            public boolean isOk;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Contacts.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                isOk = this.tlv.getInteger(0x7f) == 0x000186A0;
            }
        }
    }

    public static class ContactsCount {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Contacts.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x1)
                        .put(0x2);

                this.complete = true;
                this.isEncrypted = true;
            }
        }

        public static class Response extends HuaweiPacket {

            public int maxCount;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Contacts.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                maxCount = this.tlv.getByte(0x01);
            }
        }
    }
}
