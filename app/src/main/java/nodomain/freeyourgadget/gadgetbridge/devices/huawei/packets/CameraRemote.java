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

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class CameraRemote {
    public static final byte id = 0x01;

    public static class CameraRemoteSetup {
        public static final byte id = 0x2a;

        public static class Request extends HuaweiPacket {
            public enum Event {
                ENABLE_CAMERA,
                CAMERA_STARTED,
                CAMERA_STOPPED
            }

            public Request(ParamsProvider paramsProvider, Event event) {
                super(paramsProvider);

                this.serviceId = CameraRemote.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV();
                switch (event) {
                    case ENABLE_CAMERA:
                        this.tlv.put(0x01, (byte) 0x00);
                        break;
                    case CAMERA_STARTED:
                        this.tlv.put(0x01, (byte) 0x01);
                        break;
                    case CAMERA_STOPPED:
                        this.tlv.put(0x01, (byte) 0x02);
                        break;
                }

                this.complete = true;
                this.isEncrypted = true;
            }
        }
    }

    public static class CameraRemoteStatus {
        public static final byte id = 0x29;

        public static class Request extends HuaweiPacket {
            // All responses are async, and must be ACK-ed
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = CameraRemote.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x7f, 0x186A0);

                this.complete = true;
                this.isEncrypted = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public enum Event {
                OPEN_CAMERA,
                TAKE_PICTURE,
                CLOSE_CAMERA
            }

            public Event event;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = CameraRemote.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                switch (this.tlv.getByte(0x01)) {
                    case 1:
                        this.event = Event.OPEN_CAMERA;
                        break;
                    case 2:
                        this.event = Event.TAKE_PICTURE;
                        break;
                    case 3:
                        this.event = Event.CLOSE_CAMERA;
                        break;
                }
            }
        }
    }
}
