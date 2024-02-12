package org.bouncycastle.shaded.crypto.modes;

import org.bouncycastle.shaded.crypto.BlockCipher;
import org.bouncycastle.shaded.crypto.MultiBlockCipher;
import org.bouncycastle.shaded.crypto.SkippingStreamCipher;

public interface CTRModeCipher
    extends MultiBlockCipher, SkippingStreamCipher
{
    /**
     * return the underlying block cipher that we are wrapping.
     *
     * @return the underlying block cipher that we are wrapping.
     */
    BlockCipher getUnderlyingCipher();
}
