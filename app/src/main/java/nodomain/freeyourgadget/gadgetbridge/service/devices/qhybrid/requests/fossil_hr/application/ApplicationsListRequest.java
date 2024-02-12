/*  Copyright (C) 2021-2024 Arjan Schrijver, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application;

import android.content.pm.ApplicationInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupAndGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupRequest.FILE_LOOKUP_ERROR;

public class ApplicationsListRequest extends FileLookupAndGetRequest{
    public ApplicationsListRequest(FossilHRWatchAdapter adapter) {
        super(FileHandle.APP_CODE, adapter);
    }

    public void handleFileData(byte[] fileData){
        ArrayList<ApplicationInformation> applicationInfos = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(12);
        while(buffer.remaining() > 4){
            short packetLength = buffer.getShort();
            buffer.get();
            int nameLength = buffer.get() - 1; // cutting off null byte
            byte[] nameBuffer = new byte[nameLength];
            buffer.get(nameBuffer);
            String name = new String(nameBuffer);
            buffer.get(); // null byte
            byte handle = buffer.get();
            int hash = buffer.getInt();
            String version = String.format("%d.%d", buffer.get(), buffer.get());
            buffer.get();  // unknown
            buffer.get();  // unknown

            applicationInfos.add(new ApplicationInformation(
                    name,
                    version,
                    hash,
                    handle
            ));
        }
        Collections.sort(applicationInfos);
        this.handleApplicationsList(applicationInfos);
    }

    public void handleApplicationsList(List<ApplicationInformation> installedApplications){

    }

    public void handleFileLookupError(FILE_LOOKUP_ERROR error){

    }
}
