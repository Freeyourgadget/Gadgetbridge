package org.bouncycastle.crypto;

public abstract class DefaultMultiBlockCipher
    implements MultiBlockCipher
{
    protected DefaultMultiBlockCipher()
    {
    }

    public int getMultiBlockSize()
    {
        return this.getBlockSize();
    }

    public int processBlocks(byte[] in, int inOff, int blockCount, byte[] out, int outOff)
        throws DataLengthException, IllegalStateException
    {

        // TODO check if the underlying cipher supports the multiblock interface and call it directly?

        int resultLen = 0;
        int blockSize = this.getMultiBlockSize();
        
        for (int i = 0; i != blockCount; i++)
        {
            resultLen += this.processBlock(in, inOff, out, outOff + resultLen);

            inOff += blockSize;
        }

        return resultLen;
    }
}
