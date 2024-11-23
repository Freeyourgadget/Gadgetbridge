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

// Information from:
// https://mmk.pw/en/posts/freebuds-4i-proto/ and
// https://github.com/TheLastGimbus/FreeBuddy/blob/master/notes/mbb-protocol-wiki.md and
// https://codeberg.org/Freeyourgadget/Gadgetbridge/pulls/4325/

public class Earphones {
    public static final byte id = 0x2b;

    public static class InEarStateResponse extends HuaweiPacket {
        public static final byte id = 0x03;

        public byte leftState;
        public byte rightState;

        public InEarStateResponse(ParamsProvider paramsProvider) {
            super(paramsProvider);
            this.serviceId = Earphones.id;
            this.commandId = id;
            this.complete = true;
        }

        @Override
        public void parseTlv() throws ParseException {
            this.leftState = this.tlv.getByte(0x08, (byte) -1);
            this.rightState = this.tlv.getByte(0x09, (byte) -1);
        }
    }

    public static class SetAudioModeRequest extends HuaweiPacket {
        public static final byte id = 0x04;

        // TODO: enum for new state
        public SetAudioModeRequest(ParamsProvider paramsProvider, byte newState) {
            super(paramsProvider);
            this.serviceId = Earphones.id;
            this.commandId = id;

            byte[] data = {newState, newState == 0 ? 0x00 : (byte) 0xff};

            this.tlv = new HuaweiTLV().put(0x01, data);

            this.complete = true;
        }
    }

    public static class SetPauseWhenRemovedFromEar extends HuaweiPacket {
        public static final byte id = 0x10;

        // TODO: enum for new state
        public SetPauseWhenRemovedFromEar(ParamsProvider paramsProvider, boolean newState) {
            super(paramsProvider);
            this.serviceId = Earphones.id;
            this.commandId = id;

            this.tlv = new HuaweiTLV().put(0x01, newState);

            this.complete = true;
        }
    }

    // TODO: get pause when removed from ear 0x11
    // TODO: set long tap action 0x16
    // TODO: get long tap action 0x17
    // TODO: Audio mode cycle 0x19

    public static class GetAudioModeRequest {
        public static final byte id = 0x2a;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Earphones.id;
                this.commandId = id;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public short fullState;

            public byte state; // TODO: enum

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Earphones.id;
                this.commandId = id;
                this.complete = true;
            }

            @Override
            public void parseTlv() throws ParseException {
                this.fullState = this.tlv.getShort(0x01);
                this.state = (byte) this.fullState;
            }
        }
    }
}
