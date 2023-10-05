package org.bouncycastle.crypto;

/**
 * A parent class for block cipher modes that do not require block aligned data to be processed, but can function in
 * a streaming mode.
 */
public abstract class StreamBlockCipher
    extends DefaultMultiBlockCipher
    implements StreamCipher
{
    private final BlockCipher cipher;

    protected StreamBlockCipher(BlockCipher cipher)
    {
        this.cipher = cipher;
    }

    /**
     * return the underlying block cipher that we are wrapping.
     *
     * @return the underlying block cipher that we are wrapping.
     */
    public BlockCipher getUnderlyingCipher()
    {
        return cipher;
    }

    public final byte returnByte(byte in)
    {
        return calculateByte(in);
    }

    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
        throws DataLengthException
    {
        if (inOff + len > in.length)
        {
            throw new DataLengthException("input buffer too small");
        }
        if (outOff + len > out.length)
        {
            throw new OutputLengthException("output buffer too short");
        }

        int inStart = inOff;
        int inEnd = inOff + len;
        int outStart = outOff;

        while (inStart < inEnd)
        {
             out[outStart++] = calculateByte(in[inStart++]);
        }

        return len;
    }

    protected abstract byte calculateByte(byte b);
}