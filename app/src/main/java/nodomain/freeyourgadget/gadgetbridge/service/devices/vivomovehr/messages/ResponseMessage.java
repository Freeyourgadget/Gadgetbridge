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

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;

import java.util.Locale;

public class ResponseMessage {
    public final int requestID;
    public final int status;

    public ResponseMessage(int requestID, int status) {
        this.requestID = requestID;
        this.status = status;
    }

    public static ResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();

        return new ResponseMessage(requestID, status);
    }

    public String getStatusStr() {
        switch (status) {
            case VivomoveConstants.STATUS_ACK:
                return "ACK";
            case VivomoveConstants.STATUS_NAK:
                return "NAK";
            case VivomoveConstants.STATUS_UNSUPPORTED:
                return "UNSUPPORTED";
            case VivomoveConstants.STATUS_DECODE_ERROR:
                return "DECODE ERROR";
            case VivomoveConstants.STATUS_CRC_ERROR:
                return "CRC ERROR";
            case VivomoveConstants.STATUS_LENGTH_ERROR:
                return "LENGTH ERROR";
            default:
                return String.format(Locale.ROOT, "Unknown status %x", status);
        }
    }
}
