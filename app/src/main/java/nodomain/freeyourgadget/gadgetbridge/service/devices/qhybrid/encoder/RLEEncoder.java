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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.encoder;

import java.io.ByteArrayOutputStream;

public class RLEEncoder {
    public static byte[] RLEEncode(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 2);

        int lastByte = data[0];
        int count = 1;
        byte currentByte = -1;

        for (int i = 1; i < data.length; i++) {
            currentByte = data[i];

            if (currentByte != lastByte || count >= 255) {
                bos.write(count);
                bos.write(data[i - 1]);

                count = 1;
                lastByte = data[i];
            } else {
                count++;
            }
        }

        bos.write(count);
        bos.write(currentByte);

        byte[] result = bos.toByteArray();

        return result;
    }
}
