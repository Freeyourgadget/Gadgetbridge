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

public class DownloadRequestResponseMessage {
    public final int status;
    public final int response;
    public final int fileSize;

    public static final byte RESPONSE_DOWNLOAD_REQUEST_OKAY = 0;
    public static final byte RESPONSE_DATA_DOES_NOT_EXIST = 1;
    public static final byte RESPONSE_DATA_EXISTS_BUT_IS_NOT_DOWNLOADABLE = 2;
    public static final byte RESPONSE_NOT_READY_TO_DOWNLOAD = 3;
    public static final byte RESPONSE_REQUEST_INVALID = 4;
    public static final byte RESPONSE_CRC_INCORRECT = 5;
    public static final byte RESPONSE_DATA_REQUESTED_EXCEEDS_FILE_SIZE = 6;

    public DownloadRequestResponseMessage(int status, int response, int fileSize) {
        this.status = status;
        this.response = response;
        this.fileSize = fileSize;
    }

    public static DownloadRequestResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();
        final int response = reader.readByte();
        final int fileSize = reader.readInt();

        return new DownloadRequestResponseMessage(status, response, fileSize);
    }
}
