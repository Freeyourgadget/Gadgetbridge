package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class P2P {
    public static final byte id = 0x34;

    public static class P2PCommand {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {


            public Request(ParamsProvider paramsProvider,
                           byte cmdId,
                           short sequenceId,
                           String srcPackage,
                           String dstPackage,
                           String srcFingerprint,
                           String dstFingerprint,
                           byte[] sendData,
                           int sendCode) {
                super(paramsProvider);
                this.serviceId = P2P.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                           .put(0x01, cmdId)
                        .put(0x02, sequenceId)
                        .put(0x03, srcPackage)
                        .put(0x04, dstPackage);
                if(cmdId == 0x2) {
                    this.tlv.put(0x05, srcFingerprint);
                    this.tlv.put(0x06, dstFingerprint);
                }
                if(sendData != null && sendData.length > 0)
                    this.tlv.put(0x07, sendData);
                if(cmdId == 0x3) {
                    this.tlv.put(0x08, sendCode);
                }
            }
        }

        public static class Response extends HuaweiPacket {
            public byte cmdId;
            public short sequenceId;
            public String srcPackage;
            public String dstPackage;
            public String srcFingerprint = null;
            public String dstFingerprint = null;
            public byte[] respData = null;
            public int respCode = 0;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                cmdId = this.tlv.getByte(0x01);
                sequenceId = this.tlv.getShort(0x02);
                srcPackage = this.tlv.getString(0x03);
                dstPackage = this.tlv.getString(0x04);
                if(this.tlv.contains(0x05))
                    srcFingerprint = this.tlv.getString(0x05);
                if(this.tlv.contains(0x06))
                    dstFingerprint = this.tlv.getString(0x06);
                if(this.tlv.contains(0x07))
                    respData = this.tlv.getBytes(0x07);
                // NOTE: P2P service uses different data types in responseCode(TLV 0x08).
                // It sends byte from wearable but send integer to wearable
                // So we have a change that other device can send different type.
                if(this.tlv.contains(0x08))
                    respCode = this.tlv.getByte(0x08);
            }

        }
    }

}
