/*  Copyright (C) 2024 Martin.JM

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

/**
 * File downloading for "older" devices/implementations
 * Newer ones might be using service id 0x2c
 * Which one is used is reported by the band in 0x01 0x31
 */
public class FileDownloadService2C {
    public static final int id = 0x2c;

    public enum FileType {
        SLEEP_STATE,
        SLEEP_DATA,
        UNKNOWN; // Never use this as input

        static byte fileTypeToByte(FileType fileType) {
            switch (fileType) {
                case SLEEP_STATE:
                    return (byte) 0x0e;
                case SLEEP_DATA:
                    return (byte) 0x0f;
                default:
                    throw new RuntimeException();
            }
        }

        static FileType byteToFileType(byte b) {
            switch (b) {
                case 0x0e:
                    return FileType.SLEEP_STATE;
                case 0x0f:
                    return FileType.SLEEP_DATA;
                default:
                    return FileType.UNKNOWN;
            }
        }
    }

    public static class FileDownloadInit {
        public static final int id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, String filename, FileType filetype, int startTime, int endTime) {
                super(paramsProvider);

                this.serviceId = FileDownloadService2C.id;
                this.commandId = id;

                // TODO: start and end time might be optional?
                this.tlv = new HuaweiTLV()
                        .put(0x01, filename)
                        .put(0x02, FileType.fileTypeToByte(filetype))
                        .put(0x05, startTime)
                        .put(0x06, endTime);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public String fileName;
            public FileType fileType;
            public byte fileId;
            public int fileSize;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                fileName = this.tlv.getString(0x01);
                fileType = FileType.byteToFileType(this.tlv.getByte(0x02));
                fileId = this.tlv.getByte(0x03);
                fileSize = this.tlv.getInteger(0x04);
            }
        }
    }

    public static class FileInfo {
        public static final int id = 0x03;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, byte fileId) {
                super(paramsProvider);

                this.serviceId = FileDownloadService2C.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, fileId)
                        .put(0x02)
                        .put(0x03)
                        .put(0x04)
                        .put(0x05);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public byte fileId;
            public byte timeout; // TODO: not sure about unit here - maybe seconds?
            // TODO: following two might not have the best names...
            public short burstSize; // How large each 0x2c 0x05 will be
            public int maxBlockSize; // How much we can ask for before needing another 0x2c 0x04
            public boolean noEncrypt;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                fileId = this.tlv.getByte(0x01);
                timeout = this.tlv.getByte(0x02);
                burstSize = this.tlv.getShort(0x03);
                maxBlockSize = this.tlv.getInteger(0x04);
                noEncrypt = this.tlv.getBoolean(0x05); // True if command 0x04 cannot be encrypted
            }
        }
    }

    public static class RequestBlock extends HuaweiPacket {
        public static final int id = 0x04;

        public RequestBlock(ParamsProvider paramsProvider, byte fileId, int offset, int size, boolean noEncrypt) {
            super(paramsProvider);

            this.serviceId = FileDownloadService2C.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x01, fileId)
                    .put(0x02, offset)
                    .put(0x03, size);

            this.complete = true;
            this.isEncrypted = !noEncrypt;
        }
    }

    public static class BlockResponse extends HuaweiPacket {
        public static final int id = 0x05;

        public byte fileId;
        public int offset;
        public byte unknown;
        public byte[] data;

        public BlockResponse(ParamsProvider paramsProvider) {
            super(paramsProvider);
        }

        @Override
        public void parseTlv() throws ParseException {
            ByteBuffer byteBuffer;
            if (this.payload[2] == 0x7c && this.payload[3] == 0x01 && this.payload[4] == 0x01) {
                // Encrypted TLV, so we decrypt first
                this.tlv = new HuaweiTLV();
                this.tlv.parse(this.payload, 2, this.payload.length - 2);
                try {
                    byteBuffer = ByteBuffer.wrap(this.tlv.decryptRaw(paramsProvider));
                } catch (HuaweiCrypto.CryptoException e) {
                    throw new CryptoException("File download decryption exception", e);
                }
                this.tlv = null; // Prevent using it accidentally
            } else {
                byteBuffer = ByteBuffer.wrap(this.payload, 2, this.payload.length - 2);
            }

            fileId = byteBuffer.get();
            offset = byteBuffer.getInt();
            unknown = byteBuffer.get();
            data = new byte[byteBuffer.remaining()];
            System.arraycopy(byteBuffer.array(), byteBuffer.position(), data, 0, byteBuffer.remaining());
        }
    }

    public static class FileDownloadCompleteRequest extends HuaweiPacket {
        public static final int id = 0x06;

        public FileDownloadCompleteRequest(ParamsProvider paramsProvider, byte fileId) {
            super(paramsProvider);

            this.serviceId = FileDownloadService2C.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV()
                    .put(0x01, fileId)
                    .put(0x02, (byte) 1);

            this.complete = true;
        }
    }
}
