package org.bouncycastle.crypto;

/**
 * General interface for a stream cipher that supports skipping.
 */
public interface SkippingStreamCipher
    extends StreamCipher, SkippingCipher
{
}
