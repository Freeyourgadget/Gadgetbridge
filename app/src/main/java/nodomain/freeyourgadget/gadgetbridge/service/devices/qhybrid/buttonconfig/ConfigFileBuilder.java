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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

public class ConfigFileBuilder {
    private ConfigPayload[] configs;

    public ConfigFileBuilder(ConfigPayload[] configs) {
        this.configs = configs;
    }

    public byte[] build(boolean appendChecksum) {
        int payloadSize = 0;
        for (ConfigPayload payload : this.configs) {
            payloadSize += payload.getData().length;
        }

        int headerSize = 0;
        for (ConfigPayload payload : this.configs) {
            headerSize += payload.getHeader().length + 3; // button + version + null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(
                3 // version bytes
                        + 1 // header count byte
                        + headerSize
                        + 1 // payload count byte
                        + payloadSize
                        + 1 // customization count byte
                        + (appendChecksum ? 4 : 0) // checksum
        );
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00}); // version
        buffer.put((byte) this.configs.length);
        int buttonIndex = 0x00;
        for (ConfigPayload payload : configs) {
            buffer.put((byte) (buttonIndex += 0x10));
            buffer.put((byte) 0x01);
            buffer.put(payload.getHeader());
            buffer.put((byte) 0x00);
        }

        ArrayList<ConfigPayload> distinctPayloads = new ArrayList<>(3);

        // distinctPayloads.add(configs[0].getData());

        compareLoop:
        for (int payloadIndex = 0; payloadIndex < configs.length; payloadIndex++) {
            for (int compareTo = 0; compareTo < distinctPayloads.size(); compareTo++) {
                if (configs[payloadIndex].equals(distinctPayloads.get(compareTo))) {
                    continue compareLoop;
                }
            }
            distinctPayloads.add(configs[payloadIndex]);
        }

        buffer.put((byte) distinctPayloads.size());
        for (ConfigPayload payload : distinctPayloads) {
            buffer.put(payload.getData());
        }

        buffer.put((byte) 0x00);

        ByteBuffer buffer2 = ByteBuffer.allocate(buffer.position() + (appendChecksum ? 4 : 0));
        buffer2.order(ByteOrder.LITTLE_ENDIAN);
        buffer2.put(buffer.array(), 0, buffer.position());

        if (!appendChecksum) return buffer2.array();

        CRC32 crc = new CRC32();
        crc.update(buffer.array(), 0, buffer.position());

        buffer2.putInt((int) crc.getValue());

        return buffer2.array();
    }
}
