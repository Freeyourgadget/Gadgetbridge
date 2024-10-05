package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class EphemerisFileUpload {
    public static final int id = 0x1c;

    public static class FileList {
        public static final int id = 0x01;

        public static class FileListIncomingRequest extends HuaweiPacket {
            public byte fileType;
            public String productId = "";

            public FileListIncomingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

                // 0x1 - filenames
                // 0x2 - fileType
                // 0x3 - productId
                // 0x4 - issuerId
                // 0x5 - cardType
                this.fileType = this.tlv.getByte(0x02);
                if (this.tlv.contains(0x3))
                    productId = this.tlv.getString(0x3);

            }
        }

        public static class FileListResponse extends HuaweiPacket {

            public FileListResponse(ParamsProvider paramsProvider, int responseCode, String files) {
                super(paramsProvider);

                this.serviceId = EphemerisFileUpload.id;
                this.commandId = id;

                if (responseCode != 0) {
                    this.tlv = new HuaweiTLV().put(0x7f, responseCode);
                } else {
                    this.tlv = new HuaweiTLV().put(0x1, files);
                }

                this.complete = true;
            }
        }
    }

    public static class FileConsult {
        public static final int id = 0x02;

        public static class FileConsultIncomingRequest extends HuaweiPacket {
            public int responseCode = 0;
            public String protocolVersion = null;
            public byte bitmapEnable = 0;
            public short transferSize = 0;
            public int maxDataSize = 0;
            public short timeOut = 0;
            public byte fileType = 0;

            public FileConsultIncomingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

                if (this.tlv.contains(0x7f))
                    responseCode = this.tlv.getInteger(0x7f);

                // 0x1 - version
                // 0x2 - bitmapEnable
                // 0x3 - transferSize
                // 0x4 - maxDataSize
                // 0x5 - timeOut
                // 0x6 - fileType

                if (this.tlv.contains(0x1))
                    this.protocolVersion = this.tlv.getString(0x1);
                if (this.tlv.contains(0x2))
                    this.bitmapEnable = this.tlv.getByte(0x2);
                if (this.tlv.contains(0x3))
                    this.transferSize = this.tlv.getShort(0x3);
                if (this.tlv.contains(0x4))
                    this.maxDataSize = this.tlv.getInteger(0x4);
                if (this.tlv.contains(0x5))
                    this.timeOut = this.tlv.getShort(0x5);
                if (this.tlv.contains(0x6))
                    this.fileType = this.tlv.getByte(0x6);
            }
        }

        public static class FileConsultResponse extends HuaweiPacket {

            public FileConsultResponse(ParamsProvider paramsProvider, int responseCode) {
                super(paramsProvider);

                this.serviceId = EphemerisFileUpload.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x7f, responseCode);

                this.complete = true;
            }
        }
    }

    public static class QuerySingleFileInfo {
        public static final int id = 0x03;

        public static class QuerySingleFileInfoIncomingRequest extends HuaweiPacket {
            public int responseCode = 0;
            public String fileName = null;

            public QuerySingleFileInfoIncomingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

                if (this.tlv.contains(0x7f))
                    responseCode = this.tlv.getInteger(0x7f);

                if (this.tlv.contains(0x1))
                    this.fileName = this.tlv.getString(0x1);

            }
        }

        public static class QuerySingleFileInfoResponse extends HuaweiPacket {

            public QuerySingleFileInfoResponse(ParamsProvider paramsProvider, int responseCode, int fileSize, short crc) {
                super(paramsProvider);

                this.serviceId = EphemerisFileUpload.id;
                this.commandId = id;

                if(responseCode != 0) {
                    this.tlv = new HuaweiTLV().put(0x7f, responseCode);
                } else {
                    this.tlv = new HuaweiTLV().put(0x2, fileSize).put(0x3, crc);
                }

                this.complete = true;
            }
        }
    }

    public static class DataRequest {
        public static final int id = 0x04;

        public static class DataRequestIncomingRequest extends HuaweiPacket {
            public int responseCode = 0;
            public String fileName = null;
            public int offset = -1;
            public int len = -1;
            public byte bitmap = (byte) 0xff;


            public DataRequestIncomingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                // 1 - fileName
                // 2 - offset
                // 3 - length
                // 4 - fileBitmap
                // 7f - error

                if (this.tlv.contains(0x7f))
                    responseCode = this.tlv.getInteger(0x7f);

                if (this.tlv.contains(0x1))
                    this.fileName = this.tlv.getString(0x1);
                if (this.tlv.contains(0x2))
                    this.offset = this.tlv.getInteger(0x2);
                if (this.tlv.contains(0x3))
                    this.len = this.tlv.getInteger(0x3);
                if (this.tlv.contains(0x4))
                    this.bitmap = this.tlv.getByte(0x4);
            }
        }

        public static class DataRequestResponse extends HuaweiPacket {
            public DataRequestResponse(ParamsProvider paramsProvider, int responseCode, String filename, int offset) {
                super(paramsProvider);

                this.serviceId = EphemerisFileUpload.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x7f, responseCode);
                if(responseCode == 100000) {
                    this.tlv.put(0x2, filename).put(0x3, offset);
                }

                this.complete = true;
            }
        }
    }

    public static class UploadData {
        public static final int id = 0x05;

        public static class FileNextChunkSend extends HuaweiPacket {
            public FileNextChunkSend(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = EphemerisFileUpload.id;
                this.commandId = id;
                this.complete = true;
            }
        }

        public static class UploadDataResponse extends HuaweiPacket {
            public int responseCode = 0;

            public UploadDataResponse(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                this.tlv = new HuaweiTLV();
                this.tlv.parse(payload, 2, payload.length - 2);
                this.responseCode = this.tlv.getInteger(0x7f);
            }
        }
    }


    public static class UploadDone {
        public static final int id = 0x06;

        public static class UploadDoneIncomingRequest extends HuaweiPacket {
            public byte uploadResult = 0;

            public UploadDoneIncomingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                this.uploadResult = this.tlv.getByte(0x1);
            }
        }

        public static class UploadDoneResponse extends HuaweiPacket {

            public UploadDoneResponse(ParamsProvider paramsProvider, int responseCode) {
                super(paramsProvider);

                this.serviceId = EphemerisFileUpload.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x7f, responseCode);

                this.complete = true;
            }
        }
    }

}
