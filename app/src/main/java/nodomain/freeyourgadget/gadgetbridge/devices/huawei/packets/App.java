package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class App {
    public static final byte id = 0x2a;

    public static class AppDeviceParams {
        public byte unknown1 = 0;
        public int unknown2 = 0;
        public String osVersion = "";
        public String screenShape = "";
        public int width = 0;
        public int height = 0;
        public int unknown3 = 0;
        public String buildType = "";
    }

    public static class InstalledAppInfo {
        public String packageName;
        public String version;
        public int unknown1;
        public String appName;
        public int unknown2;
        public int versionCode;
        public byte unknown4;
        public byte unknown6;

        public InstalledAppInfo(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
            this.packageName = tlv.getString(0x03);
            this.version = tlv.getString(0x04);
            this.unknown1 = tlv.getInteger(0x05);
            this.appName =  tlv.getString(0x06);
            this.unknown2 = tlv.getInteger(0x07);
            this.versionCode = tlv.getInteger(0x09);
            this.unknown4 = tlv.getByte(0x0a);
            //{tag: b - Value: } -
            this.unknown6 = tlv.getByte(0x0d);
        }
    }

    public static class AppDelete {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider,
                           String packageName) {
                super(paramsProvider);
                this.serviceId = App.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x01, (byte)1)
                        .put(0x02, packageName);
            }
        }

        public static class Response extends HuaweiPacket {
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }
        }
    }

    public static class AppNames {
        public static final byte id = 0x03;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = App.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x81);
            }
        }

        public static class Response extends HuaweiPacket {

            public List<App.InstalledAppInfo> appInfoList;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                appInfoList = new ArrayList<>();
                if(this.tlv.contains(0x81)) {
                    for (HuaweiTLV subTlv : this.tlv.getObject(0x81).getObjects(0x82)) {
                        appInfoList.add(new InstalledAppInfo(subTlv));
                    }
                }
            }
        }
    }

    public static class AppInfoParams {
        public static final byte id = 0x06;
        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = App.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x81);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public AppDeviceParams params = new AppDeviceParams();
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                if(this.tlv.contains(0x81)) {
                    HuaweiTLV subTlv = this.tlv.getObject(0x81).getObject(0x82);
                    this.params.unknown1 = subTlv.getByte(0x03);
                    this.params.unknown2 = subTlv.getInteger(0x04);
                    this.params.osVersion = subTlv.getString(0x05);
                    this.params.screenShape = subTlv.getString(0x06);
                    this.params.width = subTlv.getShort(0x07);
                    this.params.height = subTlv.getShort(0x08);
                    if (subTlv.contains(0x09))
                        this.params.unknown3 = subTlv.getInteger(0x09);
                    if(subTlv.contains(0x0a))
                        this.params.buildType = subTlv.getString(0x0a);
                }
            }
        }
    }

}
