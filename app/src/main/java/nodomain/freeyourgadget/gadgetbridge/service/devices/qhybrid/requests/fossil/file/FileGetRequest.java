/*  Copyright (C) 2019-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;

public abstract class FileGetRequest extends FileGetRawRequest {
    public FileGetRequest(short handle, FossilWatchAdapter adapter) {
        super(handle, adapter);
    }

    public FileGetRequest(FileHandle handle, FossilWatchAdapter adapter) {
        super(handle, adapter);
    }

    @Override
    public void handleFileRawData(byte[] fileData) {
        byte[] file = new byte[fileData.length - 12 - 4]; // 12 = header   4 = crc end
        System.arraycopy(fileData, 12, file, 0, file.length);

        this.handleFileData(file);
    }

    public abstract void handleFileData(byte[] fileData);
}
