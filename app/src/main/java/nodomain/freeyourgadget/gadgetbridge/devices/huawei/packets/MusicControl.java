/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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

public class MusicControl {
    public static final byte id = 0x25;

    // TODO: should this be in HuaweiConstants?
    public static final int successValue = 0x000186A0;

    public static class MusicStatusRequest extends HuaweiPacket {
        public MusicStatusRequest(ParamsProvider paramsProvider, byte commandId, int returnValue) {
            super(paramsProvider);

            this.serviceId = MusicControl.id;
            this.commandId = commandId;
            this.tlv = new HuaweiTLV()
                    .put(0x7F, returnValue);
            this.isEncrypted = true;
            this.complete = true;
        }
    }

    public static class MusicStatusResponse extends HuaweiPacket {
        public static final byte id = 0x01;

        public int status = -1;

        public MusicStatusResponse(ParamsProvider paramsProvider) {
            super(paramsProvider);

            this.serviceId = MusicControl.id;
            this.commandId = id;
        }

        @Override
        public void parseTlv() throws ParseException {
            if (this.tlv.contains(0x7F) && this.tlv.getBytes(0x7F).length == 4)
                this.status = this.tlv.getInteger(0x7F);
        }
    }

    public static class MusicInfo {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {
            public Request(
                    ParamsProvider paramsProvider,
                    String artistName,
                    String songName,
                    byte playState,
                    byte maxVolume,
                    byte currentVolume
            ) {
                super(paramsProvider);
                this.serviceId = MusicControl.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x01, artistName)
                        .put(0x02, songName)
                        .put(0x03, playState)
                        .put(0x04, maxVolume)
                        .put(0x05, currentVolume);
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public boolean ok = false;
            public String error = "No input has been parsed yet";

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = MusicControl.id;
                this.commandId = id;

                this.isEncrypted = true;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (this.tlv.contains(0x7F)) {
                    if (this.tlv.getInteger(0x7F) == successValue) {
                        this.ok = true;
                        this.error = "";
                    } else {
                        this.ok = false;
                        this.error = "Music information error code: " + Integer.toHexString(this.tlv.getInteger(0x7F));
                    }
                } else {
                    this.ok = false;
                    this.error = "Music information response no status tag";
                }
            }
        }
    }

    public static class Control {
        public static final byte id = 0x03;

        public static class Response extends HuaweiPacket {
            public enum Button {
                Unknown,
                Play,
                Pause,
                Previous,
                Next,
                Volume_up,
                Volume_down
            }

            public boolean buttonPresent = false;
            public byte rawButton = 0x00;
            public boolean volumePresent = false;
            public byte volume = 0x00;

            public Button button = null;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = MusicControl.id;
                this.commandId = id;

                this.isEncrypted = false;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (this.tlv.contains(0x01)) {
                    this.buttonPresent = true;
                    // Only grab the lowest byte
                    byte[] bytes = this.tlv.getBytes(0x01);
                    this.rawButton = bytes[bytes.length - 1];
                    switch (this.rawButton) {
                        case 1:
                            this.button = Button.Play;
                            break;
                        case 2:
                            this.button = Button.Pause;
                            break;
                        case 3:
                            this.button = Button.Previous;
                            break;
                        case 4:
                            this.button = Button.Next;
                            break;
                        case 5:
                            this.button = Button.Volume_up;
                            break;
                        case 6:
                            this.button = Button.Volume_down;
                            break;
                        case 64:
                            // Unknown button on Huawei Band 4

                        case 100:
                            // Seems like exit from music control screen to other screen
                            this.buttonPresent = false;
                        default:
                            this.button = Button.Unknown;
                    }
                }

                if (this.tlv.contains(0x02)) {
                    this.volumePresent = true;
                    // Only grab the lowest byte
                    byte[] bytes = this.tlv.getBytes(0x02);
                    this.volume = bytes[bytes.length - 1];
                }
            }
        }
    }
}
