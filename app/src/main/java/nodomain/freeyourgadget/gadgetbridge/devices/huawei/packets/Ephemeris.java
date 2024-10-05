package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Ephemeris {
    public static final int id = 0x1f;

    public static class OperatorData {
        public static final int id = 0x01;

        public static class OperatorIncomingRequest extends HuaweiPacket {
            public byte operationInfo;
            public int operationTime;

            public OperatorIncomingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

                //TODO: can contain many elements.
                //List<HuaweiTLV> subContainers = container.getObjects(0x81);
                HuaweiTLV subTlv = this.tlv.getObject(0x81);

                this.operationInfo = subTlv.getByte(0x02);
                this.operationTime = subTlv.getInteger(0x03);
            }
        }

        public static class OperatorResponse extends HuaweiPacket {

            public OperatorResponse(ParamsProvider paramsProvider, int responseCode) {
                super(paramsProvider);

                this.serviceId = Ephemeris.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x7f, responseCode);

                this.complete = true;
            }
        }
    }

    public static class ParameterConsult {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {


            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Ephemeris.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x81);
            }
        }

        public static class Response extends HuaweiPacket {
            public int consultDeviceTime;
            public byte downloadVersion;
            public String downloadTag;

            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {

                //TODO: can contain many elements.
                //List<HuaweiTLV> subContainers = container.getObjects(0x81);
                HuaweiTLV subTlv = this.tlv.getObject(0x81);

                this.consultDeviceTime = subTlv.getByte(0x04) * 1000;
                this.downloadVersion = subTlv.getByte(0x05);
                this.downloadTag = subTlv.getString(0x06);
            }

        }
    }

    public static class FileStatus {
        public static final byte id = 0x03;

        public static class Request extends HuaweiPacket {


            public Request(ParamsProvider paramsProvider, byte status) {
                super(paramsProvider);
                this.serviceId = Ephemeris.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x1, status);
            }
        }

        public static class Response extends HuaweiPacket {
            public int responseCode;

            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                this.responseCode = this.tlv.getInteger(0x7f);
            }

        }
    }

}
