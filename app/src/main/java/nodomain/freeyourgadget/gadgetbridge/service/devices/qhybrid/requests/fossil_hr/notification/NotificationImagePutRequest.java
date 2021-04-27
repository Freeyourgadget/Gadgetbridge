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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class NotificationImagePutRequest extends FilePutRequest {
    public NotificationImagePutRequest(NotificationImage[] images, FossilWatchAdapter adapter) throws IOException {
        super(FileHandle.ASSET_NOTIFICATION_IMAGES, prepareFileData(images), adapter);
    }

    public NotificationImagePutRequest(NotificationImage image, FossilWatchAdapter adapter) {
        super(FileHandle.ASSET_REPLY_IMAGES, prepareFileData(image), adapter);
    }

    private static byte[] prepareFileData(NotificationImage[] images) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (NotificationImage image : images) {
            stream.write(
                    prepareFileData(image)
            );
        }

        return stream.toByteArray();
    }

    private static byte[] prepareFileData(NotificationImage image){
        int size = image.getFileName().length() + 3 + image.getFileData().length + 2;
        ByteBuffer buffer = ByteBuffer.allocate(2 + size);

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short)(size));
        buffer.put(image.getFileName().getBytes());
        buffer.put((byte) 0x00);
        buffer.put((byte) image.getWidth());
        buffer.put((byte) image.getHeight());
        buffer.put(image.getImageData());
        buffer.put((byte) 0xff);
        buffer.put((byte) 0xff);

        return buffer.array();
    }
}