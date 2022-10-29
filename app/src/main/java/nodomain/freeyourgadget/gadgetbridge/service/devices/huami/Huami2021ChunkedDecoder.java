/*  Copyright (C) 2022 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class Huami2021ChunkedDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021ChunkedDecoder.class);

    private Byte currentHandle;
    private int currentType;
    private int currentLength;
    ByteBuffer reassemblyBuffer;

    private volatile byte[] sharedSessionKey;

    private Huami2021Handler huami2021Handler;
    private final boolean force2021Protocol;

    public Huami2021ChunkedDecoder(final Huami2021Handler huami2021Handler,
                                   final boolean force2021Protocol) {
        this.huami2021Handler = huami2021Handler;
        this.force2021Protocol = force2021Protocol;
    }

    public void setEncryptionParameters(final byte[] sharedSessionKey) {
        this.sharedSessionKey = sharedSessionKey;
    }

    public void setHuami2021Handler(final Huami2021Handler huami2021Handler) {
        this.huami2021Handler = huami2021Handler;
    }

    public void decode(final byte[] data) {
        int i = 0;
        if (data[i++] != 0x03) {
            //LOG.warn("Ignoring non-chunked payload");
            return;
        }
        final byte flags = data[i++];
        final boolean encrypted = ((flags & 0x08) == 0x08);
        final boolean firstChunk = ((flags & 0x01) == 0x01);
        final boolean lastChunk = ((flags & 0x02) == 0x02);

        if (force2021Protocol) {
            i++; // skip extended header
        }
        final byte handle = data[i++];
        if (currentHandle != null && currentHandle != handle) {
            LOG.warn("ignoring handle {}, expected {}", handle, currentHandle);
            return;
        }
        byte count = data[i++];
        if (firstChunk) { // beginning
            int full_length = (data[i++] & 0xff) | ((data[i++] & 0xff) << 8) | ((data[i++] & 0xff) << 16) | ((data[i++] & 0xff) << 24);
            currentLength = full_length;
            if (encrypted) {
                int encrypted_length = full_length + 8;
                int overflow = encrypted_length % 16;
                if (overflow > 0) {
                    encrypted_length += (16 - overflow);
                }
                full_length = encrypted_length;
            }
            reassemblyBuffer = ByteBuffer.allocate(full_length);
            currentType = (data[i++] & 0xff) | ((data[i++] & 0xff) << 8);
            currentHandle = handle;
        }
        reassemblyBuffer.put(data, i, data.length - i);
        if (lastChunk) { // end
            byte[] buf = reassemblyBuffer.array();
            if (encrypted) {
                if (sharedSessionKey == null) {
                    // Should never happen
                    LOG.warn("Got encrypted message, but there's no shared session key");
                    currentHandle = null;
                    currentType = 0;
                    return;
                }

                byte[] messagekey = new byte[16];
                for (int j = 0; j < 16; j++) {
                    messagekey[j] = (byte) (sharedSessionKey[j] ^ handle);
                }
                try {
                    buf = CryptoUtils.decryptAES(buf, messagekey);
                    buf = ArrayUtils.subarray(buf, 0, currentLength);
                    LOG.debug("decrypted data {}: {}", String.format("0x%04x", currentType), GB.hexdump(buf));
                } catch (Exception e) {
                    LOG.warn("error decrypting " + e);
                    currentHandle = null;
                    currentType = 0;
                    return;
                }
            }
            try {
                huami2021Handler.handle2021Payload((short) currentType, buf);
            } catch (final Exception e) {
                LOG.error("Failed to handle payload", e);
            }
            currentHandle = null;
            currentType = 0;
        }
    }
}
