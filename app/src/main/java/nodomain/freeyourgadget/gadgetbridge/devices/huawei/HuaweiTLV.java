/*  Copyright (C) 2024 Damien Gaignon, Martin.JM, Vitalii Tomin

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

/* TLV parsing and serialisation thanks to https://github.com/yihleego/tlv */
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import static nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants.CryptoTags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiCrypto.CryptoException;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket.ParamsProvider;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class HuaweiTLV {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HuaweiTLV huaweiTLV = (HuaweiTLV) o;
        return Objects.equals(valueMap, huaweiTLV.valueMap);
    }

    public static class TLV {
        private final byte tag;
        private final byte[] value;

        public TLV(byte tag, byte[] value) {
            this.tag = tag;
            this.value = value;
        }

        public byte getTag() {
            return tag;
        }

        public byte[] getValue() {
            return value;
        }

        public int length() {
            return 1 + VarInt.getVarIntSize(value.length) + value.length;
        }

        public byte[] serialize() {
            return ByteBuffer.allocate(this.length())
                    .put(tag)
                    .put(VarInt.putVarIntValue(value.length))
                    .put(value)
                    .array();
        }

        public String toString() {
            return "{tag: " + Integer.toHexString(tag & 0xFF) + " - Value: " + StringUtils.bytesToHex(value) + "} - ";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TLV tlv = (TLV) o;
            return tag == tlv.tag && Arrays.equals(value, tlv.value);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(HuaweiTLV.class);

    protected List<TLV> valueMap;

    public HuaweiTLV() {
        this.valueMap = new ArrayList<>();
    }

    public int length() {
        int length = 0;
        for (TLV tlv : valueMap)
            length += tlv.length();
        return length;
    }

    /**
     * Parse byte buffer into this HuaweiTLV
     * @param buffer The buffer to parse
     * @param offset The offset to start parsing at
     * @param length The length to parse
     * @return The HuaweiTLV object itself
     * @throws ArrayIndexOutOfBoundsException There are two general cases in which this exception
     *  can be thrown:
     *    1. offset + length is greater than the buffer length
     *    2. The buffer is malformed which causes an element size to be larger than the remaining
     *       buffer length
     */
    public HuaweiTLV parse(byte[] buffer, int offset, int length)  {
        if (buffer == null)
            return null;
        int parsed = 0;
        while (parsed < length) {
            // Tag is 1 byte
            byte tag = buffer[offset + parsed];
            parsed += 1;
            // It seems that there can be an extra null byte at the end of something encrypted
            // If that happens we ignore it
            if (parsed == length && tag == 0)
                break;
            // Size is a VarInt >= 1 byte
            VarInt varInt = new VarInt(buffer, offset + parsed);
            int size = varInt.dValue;
            parsed += varInt.size;
            byte[] value = new byte[size];
            System.arraycopy(buffer, offset + parsed, value, 0, size);
            put(tag, value);
            parsed += size;
        }
        LOG.debug("Parsed TLV: " + this);
        return this;
    }

    public HuaweiTLV parse(byte[] buffer) {
        if (buffer == null) {
            return null;
        }
        return parse(buffer, 0, buffer.length);
    }

    public byte[] serialize() {
        int length = this.length();
        if (length == 0)
            return new byte[0];
        ByteBuffer buffer = ByteBuffer.allocate(length);
        for (TLV entry : valueMap)
            buffer.put(entry.serialize());
        LOG.debug("Serialized TLV: " + this);
        return buffer.array();
    }

    public HuaweiTLV put(int tag) {
        byte[] value = new byte[0];
        valueMap.add(new TLV((byte)tag, value));
        return this;
    }

    public HuaweiTLV put(int tag, byte[] value) {
        if (value == null) {
            return this;
        }
        valueMap.add(new TLV((byte)tag, value));
        return this;
    }

    public HuaweiTLV put(int tag, byte value) {
        return put(tag, new byte[]{value});
    }

    public HuaweiTLV put(int tag, boolean value) {
        return put(tag, new byte[]{value ? (byte) 1 : (byte) 0});
    }

    public HuaweiTLV put(int tag, Long value) {
        return put(tag, ByteBuffer.allocate(8).putLong(value).array());
    }

    public HuaweiTLV put(int tag, Double value) {
        return put(tag, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array());
    }

    public HuaweiTLV put(int tag, int value) {
        return put(tag, ByteBuffer.allocate(4).putInt(value).array());
    }

    public HuaweiTLV put(int tag, short value) {
        return put(tag, ByteBuffer.allocate(2).putShort(value).array());
    }

    public HuaweiTLV put(int tag, String value) {
        if (value == null) {
            return this;
        }
        return put(tag, value.getBytes(StandardCharsets.UTF_8));
    }

    public HuaweiTLV put(int tag, HuaweiTLV value) {
        if (value == null) {
            return this;
        }
        return put(tag, value.serialize());
    }

    public List<TLV> get() {
        return this.valueMap;
    }

    public byte[] getBytes(int tag) throws HuaweiPacket.MissingTagException {
        for (TLV item : valueMap)
            if (item.getTag() == (byte) tag)
                return item.getValue();
        throw new HuaweiPacket.MissingTagException(tag);
    }

    public byte[] getBytes(int tag, byte[] defaultValue) {
        try {
            return getBytes(tag);
        } catch (HuaweiPacket.MissingTagException e) {
            return defaultValue;
        }
    }

    public Byte getByte(int tag) throws HuaweiPacket.MissingTagException {
        return getBytes(tag)[0];
    }

    public Byte getByte(int tag, Byte defaultValue) {
        try {
            return getByte(tag);
        } catch (HuaweiPacket.MissingTagException e) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(int tag) throws HuaweiPacket.MissingTagException {
        return getBytes(tag)[0] == 1;
    }

    public Boolean getBoolean(int tag, Boolean defaultValue) {
        try {
            return getBoolean(tag);
        } catch (HuaweiPacket.MissingTagException e) {
            return defaultValue;
        }
    }

    public Integer getInteger(int tag) throws HuaweiPacket.MissingTagException {
        return ByteBuffer.wrap(getBytes(tag)).getInt();
    }

    public Integer getInteger(int tag, Integer defaultResult) {
        try {
            return getInteger(tag);
        } catch (HuaweiPacket.MissingTagException e) {
            return defaultResult;
        }
    }

    public Short getShort(int tag) throws HuaweiPacket.MissingTagException {
        return ByteBuffer.wrap(getBytes(tag)).getShort();
    }

    public Short getShort(int tag, Short defaultValue) {
        try {
            return getShort(tag);
        } catch (HuaweiPacket.MissingTagException e) {
            return defaultValue;
        }
    }

    public Long getLong(int tag) throws HuaweiPacket.MissingTagException {
        return ByteBuffer.wrap(getBytes(tag)).getLong();
    }

    public Integer getAsInteger(int tag) throws HuaweiPacket.MissingTagException {
        byte[] bytes = getBytes(tag);
        if(bytes.length == 1) {
            return bytes[0] & 0xFF;
        } else if(bytes.length == 2) {
            return ByteBuffer.wrap(getBytes(tag)).getShort() & 0xFFFF;
        }
        return ByteBuffer.wrap(getBytes(tag)).getInt();
    }

    public String getString(int tag) throws HuaweiPacket.MissingTagException {
        return new String(getBytes(tag), StandardCharsets.UTF_8);
    }

    public HuaweiTLV getObject(int tag) throws HuaweiPacket.MissingTagException {
        byte[] bytes = getBytes(tag);
        return new HuaweiTLV().parse(bytes, 0, bytes.length);
    }

    public List<HuaweiTLV> getObjects(int tag) {
        List<HuaweiTLV> returnValue = new ArrayList<>();
        for (TLV tlv : valueMap) {
            if (tlv.getTag() == (byte) tag)
                returnValue.add(new HuaweiTLV().parse(tlv.getValue()));
        }
        return returnValue;
    }

    public boolean contains(int tag) {
        for (TLV item : valueMap)
            if (item.getTag() == (byte) tag)
                return true;
        return false;
    }

    /**
     * Removes the last element that was added with the specified tag
     * @param tag The tag of the element that should be removed
     * @return The value contained in the removed tag
     */
    public byte[] remove(int tag) {
        TLV foundItem = null;
        for (TLV item : valueMap)
            if (item.getTag() == (byte) tag)
                foundItem = item;
        if (foundItem != null) {
            valueMap.remove(foundItem);
            return foundItem.getValue();
        } else {
            return null;
        }
    }

    /**
     * Get string representation of HuaweiTLV, "Empty" when no elements are present
     * @return String
     */
    public String toString() {
        if (valueMap.isEmpty())
            return "Empty";

        StringBuilder msg = new StringBuilder();
        for (TLV entry : valueMap)
            msg.append(entry.toString());
        return msg.substring(0, msg.length() - 3);
    }

    public static HuaweiTLV encryptRaw(ParamsProvider paramsProvider, byte[] data) throws CryptoException {
        byte[] key = paramsProvider.getSecretKey();
        byte[] nonce = paramsProvider.getIv();
        byte[] encryptedTLV = HuaweiCrypto.encrypt(
                paramsProvider.getEncryptMethod() == 0x01 || paramsProvider.getDeviceSupportType() == 0x04,
                data,
                key,
                nonce);
        return new HuaweiTLV()
                .put(CryptoTags.encryption, (byte) 0x01)
                .put(CryptoTags.initVector, nonce)
                .put(CryptoTags.cipherText, encryptedTLV);
    }

    public HuaweiTLV encrypt(ParamsProvider paramsProvider) throws CryptoException {
        byte[] serializedTLV = serialize();
        return encryptRaw(paramsProvider, serializedTLV);
    }

    public byte[] decryptRaw(ParamsProvider paramsProvider) throws CryptoException, HuaweiPacket.MissingTagException {
        byte[] key = paramsProvider.getSecretKey();
        return HuaweiCrypto.decrypt(
                paramsProvider.getEncryptMethod() == 0x01 || paramsProvider.getDeviceSupportType() == 0x04,
                getBytes(CryptoTags.cipherText),
                key,
                getBytes(CryptoTags.initVector));
    }

    public void decrypt(ParamsProvider paramsProvider) throws CryptoException, HuaweiPacket.MissingTagException {
        byte[] decryptedTLV = decryptRaw(paramsProvider);
        this.valueMap = new ArrayList<>();
        parse(decryptedTLV);
    }
}

final class VarInt {
    int dValue; // Decoded value of the VarInt
    int size; // Size of the encoded value
    byte[] eValue; // Encoded value of the VarInt

    public VarInt(byte[] src, int offset) {
        this.dValue = getVarIntValue(src, offset);
        this.eValue = putVarIntValue(this.dValue);
        this.size = this.eValue.length;
    }

    public String toString() {
        return "VarInt(dValue: " + this.dValue + ", size: " + this.size + ", eValue: " + StringUtils.bytesToHex(this.eValue) + ")";
    }
    
    /**
    * Returns the size of the encoded input value.
    *
    * @param value the integer to be measured
    * @return the encoding size of the input value
    */
    public static int getVarIntSize(int value) {
        int result = 0;
        do {
        result++;
        value >>>= 7;
        } while (value != 0);
        return result;
    }

    /**
    * Decode a byte array of a variable-length encoding from start,
    * 7 bits per byte.
    * Return the decoded value in int.
    *
    * @param src the byte array to get the var int from
    * @return the decoded value in int
    */
    public static int getVarIntValue(byte[] src, int offset) {
        int result = 0;
        int b;
        while (true) {
            b = src[offset];
            result += (b & 0x7F);
            if ((b & 0x80) == 0) { break; }
            result <<= 7;
            offset++;
        }
        return result;
    }

    /**
    * Encode an integer in a variable-length encoding, 7 bits per byte.
    * Return the encoded value in byte[]
    *
    * @param value the int value to encode
    * @return the encoded value in byte[]
    */
    public static byte[] putVarIntValue(int value) {
        int size = getVarIntSize(value);
        byte[] result = new byte[size];
        result[size - 1] = (byte)(value & 0x7F);
        for (int offset = size - 2; offset >= 0; offset--) {
            value >>>= 7;
            result[offset] = (byte)((value & 0x7F) | 0x80);
        }
        return result;
    }
}
