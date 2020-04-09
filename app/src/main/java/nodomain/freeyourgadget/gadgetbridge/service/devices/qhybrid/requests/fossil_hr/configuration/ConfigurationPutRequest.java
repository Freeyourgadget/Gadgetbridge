/*  Copyright (C) 2019 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.configuration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration.ConfigurationPutRequest.ConfigItem;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedPutRequest;

public class ConfigurationPutRequest extends FileEncryptedPutRequest {
    public ConfigurationPutRequest(ConfigItem item, FossilHRWatchAdapter adapter) {
        super((short) 0x0800, createFileContent(new ConfigItem[]{item}), adapter);
    }

    public ConfigurationPutRequest(ConfigItem[] items, FossilHRWatchAdapter adapter) {
        super((short) 0x0800, createFileContent(items), adapter);
    }

    private static byte[] createFileContent(ConfigItem[] items) {
        int overallSize = items.length * 3;
        for(ConfigItem item : items){
            overallSize += item.getItemSize();
        }
        ByteBuffer buffer = ByteBuffer.allocate(overallSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for(ConfigItem item : items){
            buffer.putShort(item.getId());
            buffer.put((byte) item.getItemSize());
            buffer.put(item.getContent());
        }

        return buffer.array();
    }
}

