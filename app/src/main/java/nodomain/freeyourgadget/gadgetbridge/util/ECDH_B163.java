/*  Copyright (C) 2022 Andreas Shimokawa

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */


/*
    This class is a really dumb pure java port of tiny-EDCH from here
    https://github.com/kokke/tiny-ECDH-c/

    What I did:
    - remove all curves except B163 to make porting easier
    - port to java with brain switched off
    - fix the "java has no unsigned" bugs
    - add some helpers to convert int[] to byte[] and back because java has no casts

    The result is ugly, no one would write such crappy code from scratch, but I tried to
    keep it as close to the C code as possible to prevent bugs. Since I did not know what
    I was doing.
 */


package nodomain.freeyourgadget.gadgetbridge.util;

public class ECDH_B163 {

    static final int CURVE_DEGREE = 163;
    static final int ECC_PRV_KEY_SIZE = 24;
    static final int ECC_PUB_KEY_SIZE = 2 * ECC_PRV_KEY_SIZE;

    /* margin for overhead needed in intermediate calculations */
    static final int BITVEC_MARGIN = 3;
    static final int BITVEC_NBITS = (CURVE_DEGREE + BITVEC_MARGIN);
    static final int BITVEC_NWORDS = ((BITVEC_NBITS + 31) / 32);
    static final int BITVEC_NBYTES = (4 * BITVEC_NWORDS);

    /******************************************************************************/

    /* Here the curve parameters are defined. */

    /* NIST B-163 */
    static final int[] polynomial = {0x000000c9, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000008};
    static final int[] coeff_b = {0x4a3205fd, 0x512f7874, 0x1481eb10, 0xb8c953ca, 0x0a601907, 0x00000002};
    static final int[] base_x = {0xe8343e36, 0xd4994637, 0xa0991168, 0x86a2d57e, 0xf0eba162, 0x00000003};
    static final int[] base_y = {0x797324f1, 0xb11c5c0c, 0xa2cdd545, 0x71a0094f, 0xd51fbc6c, 0x00000000};
    static final int[] base_order = {0xa4234c33, 0x77e70c12, 0x000292fe, 0x00000000, 0x00000000, 0x00000004};

    /*************************************************************************************************/

    /* Private / static functions: */

    /* some basic bit-manipulation routines that act on bit-vectors follow */
    static int bitvec_get_bit(final int[] x, final int idx) {
        return (int) ((((((long) x[idx / 32] & 0xffffffffL) >> (idx & 31)) & 1)));
    }

    static void bitvec_clr_bit(final int[] x, final int idx) {
        x[idx / 32] &= ~(1 << (idx & 31));
    }

    static void bitvec_copy(int[] x, int[] y) {
        int i;
        for (i = 0; i < BITVEC_NWORDS; ++i) {
            x[i] = y[i];
        }
    }

    static void bitvec_swap(int[] x, int[] y) {
        int[] tmp = new int[BITVEC_NWORDS];
        bitvec_copy(tmp, x);
        bitvec_copy(x, y);
        bitvec_copy(y, tmp);
    }

    /* fast version of equality test */
    static boolean bitvec_equal(final int[] x, final int[] y) {
        int i;
        for (i = 0; i < BITVEC_NWORDS; ++i) {
            if (x[i] != y[i]) {
                return false;
            }
        }
        return true;
    }

    static void bitvec_set_zero(int[] x) {
        int i;
        for (i = 0; i < BITVEC_NWORDS; ++i) {
            x[i] = 0;
        }
    }

    /* fast implementation */
    static boolean bitvec_is_zero(final int[] x) {
        int i = 0;
        while (i < BITVEC_NWORDS) {
            if (x[i] != 0) {
                break;
            }
            i += 1;
        }
        return (i == BITVEC_NWORDS);
    }

    /* return the number of the highest one-bit + 1 */
    static int bitvec_degree(final int[] x) {
        int i = BITVEC_NWORDS * 32;

        /* Start at the back of the vector (MSB) */
        int y = BITVEC_NWORDS;

        /* Skip empty / zero words */
        while ((i > 0)
                && (x[--y] == 0)) {
            i -= 32;
        }
        /* Run through rest if count is not multiple of bitsize of DTYPE */
        if (i != 0) {
            int u32mask = ((int) 1 << 31);
            while (((x[y]) & u32mask) == 0) {
                u32mask = (int) (((long) u32mask & 0xffffffffL) >> 1);
                i -= 1;
            }
        }
        return i;
    }

    /* left-shift by 'count' digits */
    static void bitvec_lshift(int[] x, final int[] y, int nbits) {
        int nwords = (nbits / 32);

        /* Shift whole words first if nwords > 0 */
        int i, j;
        for (i = 0; i < nwords; ++i) {
            /* Zero-initialize from least-significant word until offset reached */
            x[i] = 0;
        }
        j = 0;
        /* Copy to x output */
        while (i < BITVEC_NWORDS) {
            x[i] = y[j];
            i += 1;
            j += 1;
        }

        /* Shift the rest if count was not multiple of bitsize of DTYPE */
        nbits &= 31;
        if (nbits != 0) {
            /* Left shift rest */
            for (i = (BITVEC_NWORDS - 1); i > 0; --i) {
                x[i] = (int) (((long) (x[i]) << nbits) | (((long) x[i - 1] & 0xffffffffL) >> (32 - nbits)));
            }
            x[0] = (int) ((long) (x[0]) << nbits);
        }
    }

    /*************************************************************************************************/
    /*
     * Code that does arithmetic on bit-vectors in the Galois Field
     * GF(2^CURVE_DEGREE).
     */

    /*************************************************************************************************/

    static void gf2field_set_one(int[] x) {
        /* Set first word to one */
        x[0] = 1;
        /* .. and the rest to zero */
        int i;
        for (i = 1; i < BITVEC_NWORDS; ++i) {
            x[i] = 0;
        }
    }


    /* fastest check if x == 1 */
    static boolean gf2field_is_one(int[] x) {
        /* Check if first word == 1 */
        if (x[0] != 1) {
            return false;
        }
        /* ...and if rest of words == 0 */
        int i;
        for (i = 1; i < BITVEC_NWORDS; ++i) {
            if (x[i] != 0) {
                break;
            }
        }
        return (i == BITVEC_NWORDS);
    }

    /* galois field(2^m) addition is modulo 2, so XOR is used instead - 'z := a + b' */
    static void gf2field_add(int[] z, final int[] x, final int[] y) {
        int i;
        for (i = 0; i < BITVEC_NWORDS; ++i) {
            z[i] = (x[i] ^ y[i]);
        }
    }

    /* increment element */
    static void gf2field_inc(int[] x) {
        x[0] ^= 1;
    }

    /* field multiplication 'z := (x * y)' */
    static void gf2field_mul(int[] z, final int[] x, final int[] y) {
        int i;
        int[] tmp = new int[BITVEC_NWORDS];
        assert (z != y);

        bitvec_copy(tmp, x);

        /* LSB set? Then start with x */
        if (bitvec_get_bit(y, 0) != 0) {
            bitvec_copy(z, x);
        } else /* .. or else start with zero */ {
            bitvec_set_zero(z);
        }

        /* Then add 2^i * x for the rest */
        for (i = 1; i < CURVE_DEGREE; ++i) {
            /* lshift 1 - doubling the value of tmp */
            bitvec_lshift(tmp, tmp, 1);

            /* Modulo reduction polynomial if degree(tmp) > CURVE_DEGREE */
            if (bitvec_get_bit(tmp, CURVE_DEGREE) != 0) {
                gf2field_add(tmp, tmp, polynomial);
            }

            /* Add 2^i * tmp if this factor in y is non-zero */
            if (bitvec_get_bit(y, i) != 0) {
                gf2field_add(z, z, tmp);
            }
        }
    }

    /* field inversion 'z := 1/x' */
    static void gf2field_inv(int[] z, final int[] x) {
        int[] u = new int[BITVEC_NWORDS];
        int[] v = new int[BITVEC_NWORDS];
        int[] g = new int[BITVEC_NWORDS];
        int[] h = new int[BITVEC_NWORDS];

        int i;

        bitvec_copy(u, x);
        bitvec_copy(v, polynomial);
        bitvec_set_zero(g);
        gf2field_set_one(z);

        while (!gf2field_is_one(u)) {
            i = (bitvec_degree(u) - bitvec_degree(v));

            if (i < 0) {
                bitvec_swap(u, v);
                bitvec_swap(g, z);
                i = -i;
            }
            bitvec_lshift(h, v, i);
            gf2field_add(u, u, h);
            bitvec_lshift(h, g, i);
            gf2field_add(z, z, h);
        }
    }

    /*************************************************************************************************/
    /*
     * The following code takes care of Galois-Field arithmetic.
     * Elliptic curve points are represented by pairs (x,y) of bitvec_t.
     * It is assumed that curve coefficient 'a' is {0,1}
     * This is the case for all NIST binary curves.
     * Coefficient 'b' is given in 'coeff_b'.
     * '(base_x, base_y)' is a point that generates a large prime order group.
     */

    /*************************************************************************************************/


    static void gf2point_copy(int[] x1, int[] y1, final int[] x2, final int[] y2) {
        bitvec_copy(x1, x2);
        bitvec_copy(y1, y2);
    }

    static void gf2point_set_zero(int[] x, int[] y) {
        bitvec_set_zero(x);
        bitvec_set_zero(y);
    }

    static boolean gf2point_is_zero(final int[] x, final int[] y) {
        return (bitvec_is_zero(x)
                && bitvec_is_zero(y));
    }

    /* double the point (x,y) */
    static void gf2point_double(int[] x, int[] y) {
        /* iff P = O (zero or infinity): 2 * P = P */
        if (bitvec_is_zero(x)) {
            bitvec_set_zero(y);
        } else {
            int[] l = new int[BITVEC_NWORDS];
            gf2field_inv(l, x);
            gf2field_mul(l, l, y);
            gf2field_add(l, l, x);
            gf2field_mul(y, x, x);
            gf2field_mul(x, l, l);
            gf2field_inc(l);
            gf2field_add(x, x, l);
            gf2field_mul(l, l, x);
            gf2field_add(y, y, l);
        }
    }

    /* add two points together (x1, y1) := (x1, y1) + (x2, y2) */
    static void gf2point_add(int[] x1, int[] y1, final int[] x2, final int[] y2) {
        if (!gf2point_is_zero(x2, y2)) {
            if (gf2point_is_zero(x1, y1)) {
                gf2point_copy(x1, y1, x2, y2);
            } else {
                if (bitvec_equal(x1, x2)) {
                    if (bitvec_equal(y1, y2)) {
                        gf2point_double(x1, y1);
                    } else {
                        gf2point_set_zero(x1, y1);
                    }
                } else {
                    /* Arithmetic with temporary variables */
                    int[] a = new int[BITVEC_NWORDS];
                    int[] b = new int[BITVEC_NWORDS];
                    int[] c = new int[BITVEC_NWORDS];
                    int[] d = new int[BITVEC_NWORDS];

                    gf2field_add(a, y1, y2);
                    gf2field_add(b, x1, x2);
                    gf2field_inv(c, b);
                    gf2field_mul(c, c, a);
                    gf2field_mul(d, c, c);
                    gf2field_add(d, d, c);
                    gf2field_add(d, d, b);
                    gf2field_inc(d);
                    gf2field_add(x1, x1, d);
                    gf2field_mul(a, x1, c);
                    gf2field_add(a, a, d);
                    gf2field_add(y1, y1, a);
                    bitvec_copy(x1, d);
                }
            }
        }
    }


    /* point multiplication via double-and-add algorithm */
    static void gf2point_mul(int[] x, int[] y, final int[] exp) {
        int[] tmpx = new int[BITVEC_NWORDS];
        int[] tmpy = new int[BITVEC_NWORDS];

        int i;
        int nbits = bitvec_degree(exp);
        gf2point_set_zero(tmpx, tmpy);

        for (i = (nbits - 1); i >= 0; --i) {
            gf2point_double(tmpx, tmpy);

            if (bitvec_get_bit(exp, i) != 0) {
                gf2point_add(tmpx, tmpy, x, y);
            }
        }

        gf2point_copy(x, y, tmpx, tmpy);
    }


    /* check if y^2 + x*y = x^3 + a*x^2 + coeff_b holds */
    static boolean gf2point_on_curve(final int[] x, final int[] y) {
        int[] a = new int[BITVEC_NWORDS];
        int[] b = new int[BITVEC_NWORDS];

        if (gf2point_is_zero(x, y)) {
            return false;
        } else {
            gf2field_mul(a, x, x);
            gf2field_mul(b, a, x);
            gf2field_add(a, a, b);
            gf2field_add(a, a, coeff_b);
            gf2field_mul(b, y, y);
            gf2field_add(a, a, b);
            gf2field_mul(b, x, y);

            return bitvec_equal(a, b);
        }
    }

    // helper needed for C->Java conversion (Java cant cast pointers)
    static int[] bytes_to_int(byte[] bytes, int offset) {
        int[] value = new int[BITVEC_NWORDS];
        int byteptr = offset;
        for (int i = 0; i < BITVEC_NWORDS; i++) {
            value[i] = ((bytes[byteptr++] & 0xff)) | ((bytes[byteptr++] & 0xff) << 8) | ((bytes[byteptr++] & 0xff) << 16) | ((bytes[byteptr++] & 0xff) << 24);
        }
        return value;
    }

    // helper needed for C->Java conversion (Java cant cast pointers)
    static void ints_to_bytes(byte[] bytes, int[] ints, int offset) {
        int byteptr = offset;
        for (int i = 0; i < BITVEC_NWORDS; i++) {
            bytes[byteptr++] = (byte) (ints[i] & 0x000000ff);
            bytes[byteptr++] = (byte) ((ints[i] & 0x0000ff00) >> 8);
            bytes[byteptr++] = (byte) ((ints[i] & 0x00ff0000) >> 16);
            bytes[byteptr++] = (byte) ((ints[i] & 0xff000000) >> 24);
        }
    }

    /*************************************************************************************************/
    /*
     * Elliptic Curve Diffie-Hellman key exchange protocol.
     */

    /*************************************************************************************************/

    /* NOTE: private should contain random data a-priori! */
    static boolean ecdh_generate_keys(byte[] public_key, byte[] private_key) {
        int[] private_key_int32 = bytes_to_int(private_key, 0);
        int[] public_key_int32_1 = bytes_to_int(public_key, 0);
        int[] public_key_int32_2 = bytes_to_int(public_key, BITVEC_NBYTES);
        /* Get copy of "base" point 'G' */
        gf2point_copy(public_key_int32_1, public_key_int32_2, base_x, base_y);

        /* Abort key generation if random number is too small */
        if (bitvec_degree(private_key_int32) < (CURVE_DEGREE / 2)) {
            return false;
        } else {
            /* Clear bits > CURVE_DEGREE in highest word to satisfy constraint 1 <= exp < n. */
            int nbits = bitvec_degree(base_order);
            int i;

            for (i = (nbits - 1); i < (BITVEC_NWORDS * 32); ++i) {
                bitvec_clr_bit(private_key_int32, i);
            }

            /* Multiply base-point with scalar (private-key) */
            gf2point_mul(public_key_int32_1, public_key_int32_2, private_key_int32);

            ints_to_bytes(public_key, public_key_int32_1, 0);
            ints_to_bytes(public_key, public_key_int32_2, BITVEC_NBYTES);

            return true;
        }
    }

    static boolean ecdh_shared_secret(byte[] private_key, byte[] others_pub, byte[] output) {
        int[] private_key_int32 = bytes_to_int(private_key, 0);
        int[] others_pub_int32_1 = bytes_to_int(others_pub, 0);
        int[] others_pub_int32_2 = bytes_to_int(others_pub, BITVEC_NBYTES);

        /* Do some basic validation of other party's public key */

        if (!gf2point_is_zero(others_pub_int32_1, others_pub_int32_2)
                && gf2point_on_curve(others_pub_int32_1, others_pub_int32_2)) {
            /* Copy other side's public key to output */
            int i;
            for (i = 0; i < (BITVEC_NBYTES * 2); ++i) {
                output[i] = others_pub[i];
            }

            /* Clear bits > CURVE_DEGREE in highest word to satisfy constraint 1 <= exp < n. */
            int nbits = bitvec_degree(base_order);

            for (i = (nbits - 1); i < (BITVEC_NWORDS * 32); ++i) {
                bitvec_clr_bit(private_key_int32, i);
            }

            /* Multiply other side's public key with own private key */
            int[] output_int32_1 = bytes_to_int(output, 0);
            int[] output_int32_2 = bytes_to_int(output, BITVEC_NBYTES);

            gf2point_mul(output_int32_1, output_int32_2, private_key_int32);

            ints_to_bytes(output, output_int32_1, 0);
            ints_to_bytes(output, output_int32_2, BITVEC_NBYTES);

            return true;
        } else {
            return false;
        }
    }

    // these are wrappers around the above C-style methods for Gadgetbridge to use
    public static byte[] ecdh_generate_public(byte[] privateEC) {
        byte[] pubKey = new byte[ECC_PUB_KEY_SIZE];
        if (ecdh_generate_keys(pubKey, privateEC)) {
            return pubKey;
        }
        return null;
    }

    public static byte[] ecdh_generate_shared(byte[] privateEC, byte[] remotePublicEC) {
        byte[] sharedKey = new byte[ECC_PUB_KEY_SIZE];
        if (ecdh_shared_secret(privateEC, remotePublicEC, sharedKey)) {
            return sharedKey;
        }
        return null;
    }
}
