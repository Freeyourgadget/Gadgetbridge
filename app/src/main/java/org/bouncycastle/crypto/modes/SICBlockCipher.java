package org.bouncycastle.crypto.modes;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

/**
 * Implements the Segmented Integer Counter (SIC) mode on top of a simple
 * block cipher. This mode is also known as CTR mode.
 */
public class SICBlockCipher
    extends StreamBlockCipher
    implements CTRModeCipher
{
    private final BlockCipher     cipher;
    private final int             blockSize;

    private byte[]          IV;
    private byte[]          counter;
    private byte[]          counterOut;
    private int             byteCount;

    /**
     * Return a new SIC/CTR mode cipher based on the passed in base cipher
     *
     * @param cipher the base cipher for the SIC/CTR mode.
     */
    public static CTRModeCipher newInstance(BlockCipher cipher)
    {
        return new SICBlockCipher(cipher);
    }

    /**
     * Basic constructor.
     *
     * @param c the block cipher to be used.
     * @deprecated use newInstance() method.
     */
    public SICBlockCipher(BlockCipher c)
    {
        super(c);

        this.cipher = c;
        this.blockSize = cipher.getBlockSize();
        this.IV = new byte[blockSize];
        this.counter = new byte[blockSize];
        this.counterOut = new byte[blockSize];
        this.byteCount = 0;
    }

    public void init(
        boolean             forEncryption, //ignored by this CTR mode
        CipherParameters    params)
        throws IllegalArgumentException
    {
        if (params instanceof ParametersWithIV)
        {
            ParametersWithIV ivParam = (ParametersWithIV)params;
            this.IV = Arrays.clone(ivParam.getIV());

            if (blockSize < IV.length)
            {
                throw new IllegalArgumentException("CTR/SIC mode requires IV no greater than: " + blockSize + " bytes.");
            }

            int maxCounterSize = (8 > blockSize / 2) ? blockSize / 2 : 8;

            if (blockSize - IV.length > maxCounterSize)
            {
                throw new IllegalArgumentException("CTR/SIC mode requires IV of at least: " + (blockSize - maxCounterSize) + " bytes.");
            }

            // if null it's an IV changed only.
            if (ivParam.getParameters() != null)
            {
                cipher.init(true, ivParam.getParameters());
            }

            reset();
        }
        else
        {
            throw new IllegalArgumentException("CTR/SIC mode requires ParametersWithIV");
        }
    }

    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName() + "/SIC";
    }

    public int getBlockSize()
    {
        return cipher.getBlockSize();
    }

    public int processBlock(byte[] in, int inOff, byte[] out, int outOff)
          throws DataLengthException, IllegalStateException
    {
        if (byteCount != 0)
        {
            processBytes(in, inOff, blockSize, out, outOff);
            return blockSize;
        }

        if (inOff + blockSize > in.length)
        {
            throw new DataLengthException("input buffer too small");
        }
        if (outOff + blockSize > out.length)
        {
            throw new OutputLengthException("output buffer too short");
        }

        cipher.processBlock(counter, 0, counterOut, 0);
        for (int i = 0; i < blockSize; ++i)
        {
            out[outOff + i] = (byte)(in[inOff + i] ^ counterOut[i]);
        }
        incrementCounter();
        return blockSize;
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

        for (int i = 0; i < len; ++i)
        {
            byte next;

            if (byteCount == 0)
            {
                checkLastIncrement();

                cipher.processBlock(counter, 0, counterOut, 0);
                next = (byte)(in[inOff + i] ^ counterOut[byteCount++]);
            }
            else
            {
                next = (byte)(in[inOff + i] ^ counterOut[byteCount++]);
                if (byteCount == counter.length)
                {
                    byteCount = 0;
                    incrementCounter();
                }
            }
            out[outOff + i] = next;
        }

        return len;
    }

    protected byte calculateByte(byte in)
          throws DataLengthException, IllegalStateException
    {
        if (byteCount == 0)
        {
            checkLastIncrement();

            cipher.processBlock(counter, 0, counterOut, 0);

            return (byte)(counterOut[byteCount++] ^ in);
        }

        byte rv = (byte)(counterOut[byteCount++] ^ in);

        if (byteCount == counter.length)
        {
            byteCount = 0;
            incrementCounter();
        }

        return rv;
    }

    private void checkCounter()
    {
        // if the IV is the same as the blocksize we assume the user knows what they are doing
        if (IV.length < blockSize)
        {
            for (int i = IV.length - 1; i >= 0; i--)
            {
                if (counter[i] != IV[i])
                {
                    throw new IllegalStateException("Counter in CTR/SIC mode out of range.");
                }
            }
        }
    }

    private void checkLastIncrement()
    {
        // if the IV is the same as the blocksize we assume the user knows what they are doing
        if (IV.length < blockSize)
        {
            if (counter[IV.length - 1] != IV[IV.length - 1])
            {
                throw new IllegalStateException("Counter in CTR/SIC mode out of range.");
            }
        }
    }

    private void incrementCounter()
    {
        int i = counter.length;
        while (--i >= 0)
        {
            if (++counter[i] != 0)
            {
                break;
            }
        }
    }

    private void incrementCounterAt(int pos)
    {
        int i = counter.length - pos;
        while (--i >= 0)
        {
            if (++counter[i] != 0)
            {
                break;
            }
        }
    }

    private void incrementCounter(int offSet)
    {
        byte old = counter[counter.length - 1];

        counter[counter.length - 1] += offSet;

        if (old != 0 && counter[counter.length - 1] < old)
        {
            incrementCounterAt(1);
        }
    }

    private void decrementCounterAt(int pos)
    {
        int i = counter.length - pos;
        while (--i >= 0)
        {
            if (--counter[i] != -1)
            {
                return;
            }
        }
    }

    private void adjustCounter(long n)
    {
        if (n >= 0)
        {
            long numBlocks = (n + byteCount) / blockSize;

            long rem = numBlocks;
            if (rem > 255)
            {
                for (int i = 5; i >= 1; i--)
                {
                    long diff = 1L << (8 * i);
                    while (rem >= diff)
                    {
                        incrementCounterAt(i);
                        rem -= diff;
                    }
                }
            }

            incrementCounter((int)rem);

            byteCount = (int)((n + byteCount) - (blockSize * numBlocks));
        }
        else
        {
            long numBlocks = (-n - byteCount) / blockSize;

            long rem = numBlocks;
            if (rem > 255)
            {
                for (int i = 5; i >= 1; i--)
                {
                    long diff = 1L << (8 * i);
                    while (rem > diff)
                    {
                        decrementCounterAt(i);
                        rem -= diff;
                    }
                }
            }

            for (long i = 0; i != rem; i++)
            {
                decrementCounterAt(0);
            }

            int gap = (int)(byteCount + n + (blockSize * numBlocks));

            if (gap >= 0)
            {
                byteCount = 0;
            }
            else
            {
                decrementCounterAt(0);
                byteCount =  blockSize + gap;
            }
        }
    }

    public void reset()
    {
        Arrays.fill(counter, (byte)0);
        System.arraycopy(IV, 0, counter, 0, IV.length);
        cipher.reset();
        this.byteCount = 0;
    }

    public long skip(long numberOfBytes)
    {
        adjustCounter(numberOfBytes);

        checkCounter();

        cipher.processBlock(counter, 0, counterOut, 0);

        return numberOfBytes;
    }

    public long seekTo(long position)
    {
        reset();

        return skip(position);
    }

    public long getPosition()
    {
        byte[] res = new byte[counter.length];

        System.arraycopy(counter, 0, res, 0, res.length);

        for (int i = res.length - 1; i >= 1; i--)
        {
            int v;
            if (i < IV.length)
            {
                v = (res[i] & 0xff) - (IV[i] & 0xff);
            }
            else
            {
                v = (res[i] & 0xff);
            }

            if (v < 0)
            {
               res[i - 1]--;
               v += 256;
            }

            res[i] = (byte)v;
        }

        return Pack.bigEndianToLong(res, res.length - 8) * blockSize + byteCount;
    }
}
