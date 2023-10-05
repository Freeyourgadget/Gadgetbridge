package org.bouncycastle.crypto;

/**
 * Base interface for a cipher engine capable of processing multiple blocks at a time.
 */
public interface MultiBlockCipher
    extends BlockCipher
{
    /**
     * Return the multi-block size for this cipher (in bytes).
     *
     * @return the multi-block size for this cipher in bytes.
     */
    int getMultiBlockSize();

    /**
     * Process blockCount blocks from input in offset inOff and place the output in
     * out from offset outOff.
     *
     * @param in input data array.
     * @param inOff start of input data in in.
     * @param blockCount number of blocks to be processed.
     * @param out output data array.
     * @param outOff start position for output data.
     * @return number of bytes written to out.
     * @throws DataLengthException
     * @throws IllegalStateException
     */
    int processBlocks(byte[] in, int inOff, int blockCount, byte[] out, int outOff)
        throws DataLengthException, IllegalStateException;
}
