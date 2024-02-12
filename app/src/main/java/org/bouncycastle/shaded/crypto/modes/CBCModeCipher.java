package org.bouncycastle.shaded.crypto.modes;

import org.bouncycastle.shaded.crypto.BlockCipher;
import org.bouncycastle.shaded.crypto.MultiBlockCipher;

public interface CBCModeCipher
    extends MultiBlockCipher
{
    /**
     * return the underlying block cipher that we are wrapping.
     *
     * @return the underlying block cipher that we are wrapping.
     */
    BlockCipher getUnderlyingCipher();
}
