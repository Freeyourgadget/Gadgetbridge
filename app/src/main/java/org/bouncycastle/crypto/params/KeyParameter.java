package org.bouncycastle.crypto.params;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.util.Arrays;

public class KeyParameter
    implements CipherParameters
{
    private byte[]  key;

    public KeyParameter(
        byte[]  key)
    {
        this(key, 0, key.length);
    }

    public KeyParameter(
        byte[]  key,
        int     keyOff,
        int     keyLen)
    {
        this(keyLen);

        System.arraycopy(key, keyOff, this.key, 0, keyLen);
    }

    private KeyParameter(int length)
    {
        this.key = new byte[length];
    }

    public void copyTo(byte[] buf, int off, int len)
    {
        if (key.length != len)
            throw new IllegalArgumentException("len");

        System.arraycopy(key, 0, buf, off, len);
    }

    public byte[] getKey()
    {
        return key;
    }

    public int getKeyLength()
    {
        return key.length;
    }

    public KeyParameter reverse()
    {
        KeyParameter reversed = new KeyParameter(key.length);
        Arrays.reverse(this.key, reversed.key);
        return reversed;
    }
}
