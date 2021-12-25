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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupRequest;

public abstract class FileEncryptedLookupAndGetRequest extends FileLookupRequest {
    public FileEncryptedLookupAndGetRequest(FileHandle fileHandle, FossilHRWatchAdapter adapter) {
        super(fileHandle, adapter);
    }

    @Override
    public void handleFileLookup(short fileHandle){
        getAdapter().queueWrite(new FileEncryptedGetRequest(getHandle(), (FossilHRWatchAdapter) getAdapter()) {
            @Override
            public void handleFileData(byte[] fileData) {
                FileEncryptedLookupAndGetRequest.this.handleFileData(fileData);
            }
        }, true);
    }

    abstract public void handleFileData(byte[] fileData);
}
