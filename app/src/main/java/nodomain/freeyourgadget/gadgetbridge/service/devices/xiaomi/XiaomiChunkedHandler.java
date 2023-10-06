/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class XiaomiChunkedHandler {
    private int numChunks = 0;
    private int currentChunk = 0;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public XiaomiChunkedHandler() {

    }

    public void setNumChunks(final int numChunks) {
        this.numChunks = numChunks;
        this.currentChunk = 0;
        this.baos.reset();
    }

    public void addChunk(final byte[] chunk) {
        try {
            baos.write(chunk);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        currentChunk++;
    }

    public int getNumChunks() {
        return numChunks;
    }

    public int getCurrentChunk() {
        return currentChunk;
    }

    public byte[] getArray() {
        return baos.toByteArray();
    }
}
