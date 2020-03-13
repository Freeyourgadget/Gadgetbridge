/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class SettingsFilePutRequest extends Request {
    public int fileLength;
    public byte[] file;

    public SettingsFilePutRequest(byte[] file){
        this.fileLength = file.length;
        this.file = file;
        ByteBuffer buffer = this.createBuffer();
        buffer.putShort(1, (short)0x0800);
        buffer.putInt(3, 0);
        buffer.putInt(7, fileLength - 10);
        buffer.putInt(11, fileLength - 10);

        this.data = buffer.array();
    }

    @Override
    public int getPayloadLength() {
        return 15;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{17};
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0007-957f-7d4a-34a6-74696673696d");
    }
}
