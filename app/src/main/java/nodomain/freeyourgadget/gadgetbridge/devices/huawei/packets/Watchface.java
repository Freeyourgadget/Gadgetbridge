/*  Copyright (C) 2024 Vitalii Tomin

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Watchface {
    public static final byte id = 0x27;

    public static class WatchfaceDeviceParams {
        public String maxVersion = "";
        public short width = 0;
        public short height = 0;
        public byte supportFileType = 1;
        public byte sort = 1;
        public String otherWatchfaceVersions = "";
    }

    public static class InstalledWatchfaceInfo {
        public String fileName = "";
        public String version = "";
        public byte type = 0;
        // bit 0 - is current
        // bit 1 - is factory preset
        // bit 2 - ???
        // bit 3 - editable
        // bit 4 - video
        // bit 5 - photo
        // bit 6 - tryout (trial version)
        // bit 7 - kaleidoskop
        public byte expandedtype = 0;

        public InstalledWatchfaceInfo(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
            this.fileName = tlv.getString(0x03);
            this.version = tlv.getString(0x04);
            this.type = tlv.getByte(0x05);
            if (tlv.contains(0x07)) // optional
                this.expandedtype = tlv.getByte(0x07);
        }

        public boolean isCurrent() {
            return (this.type & 1) == 1;
        }
        public boolean isFactory() {
            return ((this.type  >> 1 )& 1) == 1;
        }
        public boolean isEditable() {
            return ((this.type  >> 3 )& 1) == 1;
        }
        public boolean isVideo() {
            return ((this.type  >> 4 )& 1) == 1;
        }
        public boolean isPhoto() {
            return ((this.type  >> 5 )& 1) == 1;
        }
        public boolean isTryout() {
            return ((this.type  >> 6 )& 1) == 1;
        }
        public boolean isKaleidoskop() {
            return ((this.type  >> 7 )& 1) == 1;
        }
    }

    public static class WatchfaceParams {
        public static final byte id = 0x01;
        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Watchface.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01)
                        .put(0x02)
                        .put(0x03)
                        .put(0x04)
                        .put(0x05)
                        .put(0x0e)
                        .put(0x0f);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public WatchfaceDeviceParams params = new WatchfaceDeviceParams();
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                this.params.maxVersion = this.tlv.getString(0x01);
                this.params.width = this.tlv.getShort(0x02);
                this.params.height = this.tlv.getShort(0x03);
                this.params.supportFileType = this.tlv.getByte(0x04);
                this.params.sort = this.tlv.getByte(0x05);
                this.params.otherWatchfaceVersions = this.tlv.getString(0x06);
            }
        }
    }

    public static class DeviceWatchInfo {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Watchface.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x01)
                        .put(0x06, (byte) 0x03); //3 -overseas non-test, 2 - test, 1 -null?
            }
        }

        public static class Response extends HuaweiPacket {

            public List<InstalledWatchfaceInfo> watchfaceInfoList;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                watchfaceInfoList = new ArrayList<>();
                if(this.tlv.contains(0x81)) {
                    for (HuaweiTLV subTlv : this.tlv.getObject(0x81).getObjects(0x82)) {
                        watchfaceInfoList.add(new Watchface.InstalledWatchfaceInfo(subTlv));
                    }
                }
            }
        }
    }

    public static class WatchfaceOperation {
        public static final byte id = 0x03;

        public static final byte  operationActive = 1;
        public static final byte operationDelete = 2;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider,
                           String fileName, byte operation) {
                super(paramsProvider);
                this.serviceId = Watchface.id;
                this.tlv = new HuaweiTLV()
                        .put(0x01, fileName.split("_")[0])
                        .put(0x02, fileName.split("_")[1])
                        .put(0x03, operation);

                this.commandId = id;
            }
        }

        public static class Response extends HuaweiPacket {
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }
        }
    }

    public static class WatchfaceConfirm {
        public static final byte id = 0x05;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider,
                           String fileName) {
                super(paramsProvider);
                this.serviceId = Watchface.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x01, fileName.split("_")[0])
                        .put(0x02, fileName.split("_")[1])
                        .put(0x7f, 0x000186A0);
            }
        }

        public static class Response extends HuaweiPacket {
            public static byte reportType = 0;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x03)) {
                    this.reportType = this.tlv.getByte(0x03);
                }
            }

        }
    }

    public static class WatchfaceNameInfo {
        public static final byte id = 0x06;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider,
                           List<InstalledWatchfaceInfo> watchfaceList) {
                super(paramsProvider);
                this.serviceId = Watchface.id;
                this.commandId = id;

                HuaweiTLV tlvList = new HuaweiTLV();
                for (InstalledWatchfaceInfo watchface : watchfaceList) {
                    //TODO: ask name only for custom watchfaces
                    HuaweiTLV wfTlv = new HuaweiTLV().put(0x04, watchface.fileName);
                    tlvList.put(0x83, wfTlv);
                }

                this.tlv = new HuaweiTLV()
                        .put(0x01, (byte) 0x01)
                        .put(0x82, tlvList);

            }
        }

        public static class Response extends HuaweiPacket {
            public HashMap<String, String> watchFaceNames = new HashMap<String, String>();
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x82)) {
                    for (HuaweiTLV subTlv : this.tlv.getObject(0x82).getObjects(0x83)) {
                        watchFaceNames.put(subTlv.getString(0x04), subTlv.getString(0x05));
                    }
                }
            }
        }
    }
}
