/*  Copyright (C) 2020-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.device_info;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;

public class SupportedFileVersionsInfo implements DeviceInfo {
    private HashMap<Byte, Short> supportedFileVersions = new HashMap<>();

    @Override
    public void parsePayload(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        while(buffer.remaining() > 0){
            byte handle = buffer.get();
            short version = buffer.getShort();
            supportedFileVersions.put(handle, version);
        }

        // Add quick replies packet type
        supportedFileVersions.put((byte) 0x13, (short) 0x0002);
        // Add phone app packet type
        supportedFileVersions.put((byte) 0x15, (short) 0x0003);
    }

    public short getSupportedFileVersion(FileHandle fileHandle){
        return getSupportedFileVersion(fileHandle.getMajorHandle());
    }

    public short getSupportedFileVersion(byte fileHandle){
        return supportedFileVersions.get(fileHandle);
    }
}
