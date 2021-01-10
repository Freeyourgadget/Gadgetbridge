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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class NotificationImagePutRequest extends AssetFilePutRequest {
    private NotificationImagePutRequest(String packageName, AssetFile file, FossilWatchAdapter adapter) throws IOException {
        super(file, FileHandle.ASSET_NOTIFICATION_IMAGES, adapter);
    }

    private NotificationImagePutRequest(NotificationImage image, FossilWatchAdapter adapter) throws IOException {
        super(image, FileHandle.ASSET_NOTIFICATION_IMAGES, adapter);
    }

    public NotificationImagePutRequest(NotificationImage[] images, FossilWatchAdapter adapter) throws IOException {
        super(images, FileHandle.ASSET_NOTIFICATION_IMAGES, adapter);
    }


    private static byte[][] prepareFileCrc(String[] packageNames){
        byte[][] names = new byte[packageNames.length][];
        for (int i = 0; i < packageNames.length; i++){
            names[i] = prepareFileCrc(packageNames[i]);
        }
        return names;
    }

    private static byte[] prepareFileCrc(String packageName){
        CRC32 crc = new CRC32();
        crc.update(packageName.getBytes());

        String crcString = StringUtils.bytesToHex(
                ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt((int) crc.getValue())
                        .array()
        );

        ByteBuffer buffer = ByteBuffer.allocate(crcString.length() + 1)
                .put(crcString.getBytes())
                .put((byte) 0x00);

        return buffer.array();
    }
}
