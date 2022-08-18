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

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;

public class Huami2021ChunkedEncoder {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021ChunkedEncoder.class);

    private final BluetoothGattCharacteristic characteristicChunked2021Write;

    private byte writeHandle;

    // These must be volatile, since they are set by a different thread. Sometimes, GB might
    // attempt to encode a payload before they were set, which will make them not be propagated
    // to that thread later.
    private volatile int encryptedSequenceNr;
    private volatile byte[] sharedSessionKey;

    private final boolean force2021Protocol;
    private volatile int mMTU = 23;

    public Huami2021ChunkedEncoder(final BluetoothGattCharacteristic characteristicChunked2021Write,
                                   final boolean force2021Protocol,
                                   final int mMTU) {
        this.characteristicChunked2021Write = characteristicChunked2021Write;
        this.force2021Protocol = force2021Protocol;
        this.mMTU = mMTU;
    }

    public synchronized void setEncryptionParameters(final int encryptedSequenceNr, final byte[] sharedSessionKey) {
        this.encryptedSequenceNr = encryptedSequenceNr;
        this.sharedSessionKey = sharedSessionKey;
    }

    public synchronized void setMTU(int mMTU) {
        this.mMTU = mMTU;
    }

    public synchronized void write(final TransactionBuilder builder,
                                   final short type,
                                   byte[] data,
                                   final boolean extended_flags,
                                   final boolean encrypt) {
        if (encrypt && sharedSessionKey == null) {
            LOG.error("Can't encrypt without the shared session key");
            return;
        }

        writeHandle++;

        int remaining = data.length;
        int length = data.length;
        byte count = 0;
        int header_size = 10;

        if (extended_flags) {
            header_size++;
        }

        if (extended_flags && encrypt) {
            byte[] messagekey = new byte[16];
            for (int i = 0; i < 16; i++) {
                messagekey[i] = (byte) (sharedSessionKey[i] ^ writeHandle);
            }
            int encrypted_length = length + 8;
            int overflow = encrypted_length % 16;
            if (overflow > 0) {
                encrypted_length += (16 - overflow);
            }

            byte[] encryptable_payload = new byte[encrypted_length];
            System.arraycopy(data, 0, encryptable_payload, 0, length);
            encryptable_payload[length] = (byte) (encryptedSequenceNr & 0xff);
            encryptable_payload[length + 1] = (byte) ((encryptedSequenceNr >> 8) & 0xff);
            encryptable_payload[length + 2] = (byte) ((encryptedSequenceNr >> 16) & 0xff);
            encryptable_payload[length + 3] = (byte) ((encryptedSequenceNr >> 24) & 0xff);
            encryptedSequenceNr++;
            int checksum = CheckSums.getCRC32(encryptable_payload, 0, length + 4);
            encryptable_payload[length + 4] = (byte) (checksum & 0xff);
            encryptable_payload[length + 5] = (byte) ((checksum >> 8) & 0xff);
            encryptable_payload[length + 6] = (byte) ((checksum >> 16) & 0xff);
            encryptable_payload[length + 7] = (byte) ((checksum >> 24) & 0xff);
            remaining = encrypted_length;
            try {
                data = CryptoUtils.encryptAES(encryptable_payload, messagekey);
            } catch (Exception e) {
                LOG.error("error while encrypting", e);
                return;
            }

        }

        while (remaining > 0) {
            int MAX_CHUNKLENGTH = mMTU - 3 - header_size;
            int copybytes = Math.min(remaining, MAX_CHUNKLENGTH);
            byte[] chunk = new byte[copybytes + header_size];

            byte flags = 0;
            if (encrypt) {
                flags |= 0x08;
            }
            if (count == 0) {
                flags |= 0x01;
                int i = 4;
                if (extended_flags) {
                    i++;
                }
                chunk[i++] = (byte) (length & 0xff);
                chunk[i++] = (byte) ((length >> 8) & 0xff);
                chunk[i++] = (byte) ((length >> 16) & 0xff);
                chunk[i++] = (byte) ((length >> 24) & 0xff);
                chunk[i++] = (byte) (type & 0xff);
                chunk[i] = (byte) ((type >> 8) & 0xff);
            }
            if (remaining <= MAX_CHUNKLENGTH) {
                flags |= 0x06; // last chunk?
            }
            chunk[0] = 0x03;
            chunk[1] = flags;
            if (extended_flags) {
                chunk[2] = 0;
                chunk[3] = writeHandle;
                chunk[4] = count;
            } else {
                chunk[2] = writeHandle;
                chunk[3] = count;
            }

            System.arraycopy(data, data.length - remaining, chunk, header_size, copybytes);
            builder.write(characteristicChunked2021Write, chunk);
            remaining -= copybytes;
            header_size = 4;

            if (extended_flags) {
                header_size++;
            }

            count++;
        }
    }
}
