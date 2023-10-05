package org.bouncycastle.crypto;

/**
 * Ciphers producing a key stream which can be reset to particular points in the stream implement this.
 */
public interface SkippingCipher
{
    /**
     * Skip numberOfBytes forwards, or backwards.
     *
     * @param numberOfBytes the number of bytes to skip (positive forward, negative backwards).
     * @return the number of bytes actually skipped.
     * @throws java.lang.IllegalArgumentException if numberOfBytes is an invalid value.
     */
    long skip(long numberOfBytes);

    /**
     * Reset the cipher and then skip forward to a given position.
     *
     * @param position the number of bytes in to set the cipher state to.
     * @return the byte position moved to.
     */
    long seekTo(long position);

    /**
     * Return the current "position" of the cipher
     *
     * @return the current byte position.
     */
    long getPosition();
}
