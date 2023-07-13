/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class GncsDataSourceResponseMessage {
    public static final int RESPONSE_TRANSFER_SUCCESSFUL = 0;
    public static final int RESPONSE_RESEND_LAST_DATA_PACKET = 1;
    public static final int RESPONSE_ABORT_REQUEST = 2;
    public static final int RESPONSE_ERROR_CRC_MISMATCH = 3;
    public static final int RESPONSE_ERROR_DATA_OFFSET_MISMATCH = 4;

    public final int status;
    public final int response;

    public GncsDataSourceResponseMessage(int status, int response) {
        this.status = status;
        this.response = response;
    }

    public static GncsDataSourceResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestMessageID = reader.readShort();
        final int status = reader.readByte();
        final int response = reader.readByte();

        return new GncsDataSourceResponseMessage(status, response);
    }
}
