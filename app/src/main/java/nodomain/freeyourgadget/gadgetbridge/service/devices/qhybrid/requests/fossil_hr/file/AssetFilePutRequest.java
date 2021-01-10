/*  Copyright (C) 2019-2021 Andreas Shimokawa, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class AssetFilePutRequest extends FilePutRequest {
    public AssetFilePutRequest(AssetFile[] files, FileHandle handle, FossilWatchAdapter adapter) throws IOException {
        super(handle, prepareFileData(files), adapter);
    }
    public AssetFilePutRequest(AssetFile file, FileHandle handle, FossilWatchAdapter adapter) {
        super(handle, prepareFileData(file), adapter);
    }

    private static byte[] prepareFileData(AssetFile[] files) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (AssetFile file : files) {
            stream.write(
                    prepareFileData(file)
            );
        }

        return stream.toByteArray();
    }

    private static byte[] prepareFileData(AssetFile file){
        int size = file.getFileName().length() + file.getFileData().length + 1; // null byte
        ByteBuffer buffer = ByteBuffer.allocate(size + 2);

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short)(size));
        buffer.put(file.getFileName().getBytes());
        buffer.put((byte) 0x00);
        buffer.put(file.getFileData());

        return buffer.array();
    }
}
