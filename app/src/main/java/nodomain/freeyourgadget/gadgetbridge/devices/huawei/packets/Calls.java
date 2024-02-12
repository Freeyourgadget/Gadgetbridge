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

public class Calls {

    // This doesn't include the initial calling notification, as that is handled
    // by the Notifications class.

    public static final byte id = 0x04;

    // TODO: tests

    public static class AnswerCallResponse extends HuaweiPacket {
        public static final byte id = 0x01;

        public enum Action {
            CALL_ACCEPT,
            CALL_REJECT,
            UNKNOWN
        }

        public Action action = Action.UNKNOWN;

        public AnswerCallResponse(ParamsProvider paramsProvider) {
            super(paramsProvider);

            this.serviceId = Calls.id;
            this.commandId = id;

            this.isEncrypted = false;
        }

        @Override
        public void parseTlv() throws MissingTagException {
            if (this.tlv.getByte(0x01) == 0x01) {
                this.action = Action.CALL_REJECT;
            } else if (this.tlv.getByte(0x01) == 0x02) {
                this.action = Action.CALL_ACCEPT;
            }
            // TODO: find more values, if there are any
        }
    }
}
