/*  Copyright (C) 2024 Yoran Vulker

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiChannelHandler.Channel;

public abstract class AbstractXiaomiSppProtocol {

    public static class ParseResult {
        public enum Status {
            Invalid,
            Incomplete,
            Complete,
        };

        final Status status;
        int packetSize;

        public ParseResult(final Status status) {
            this.status = status;
        }

        public ParseResult(final Status status, final int packetSize) {
            this(status);
            this.packetSize = packetSize;
        }
    };

    public abstract int findNextPacketOffset(final byte[] buffer);
    public abstract ParseResult processPacket(final byte[] buffer);
    public abstract byte[] encodePacket(Channel channel, byte[] chunk);
    public boolean initializeSession() {
        return true;
    }
}
