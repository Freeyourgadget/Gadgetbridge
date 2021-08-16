/* 

  Crypto using elliptic curves defined over the finite binary field GF(2^m) where m is prime.

  The curves used are the anomalous binary curves (ABC-curves) or also called Koblitz curves.

  This class of curves was chosen because it yields efficient implementation of operations.



  Curves available - their different NIST/SECG names and eqivalent symmetric security level:

      NIST      SEC Group     strength
    ------------------------------------
      K-163     sect163k1      80 bit
      B-163     sect163r2      80 bit
      K-233     sect233k1     112 bit
      B-233     sect233r1     112 bit
      K-283     sect283k1     128 bit
      B-283     sect283r1     128 bit
      K-409     sect409k1     192 bit
      B-409     sect409r1     192 bit
      K-571     sect571k1     256 bit
      B-571     sect571r1     256 bit



  Curve parameters from:

    http://www.secg.org/sec2-v2.pdf
    http://csrc.nist.gov/publications/fips/fips186-3/fips_186-3.pdf


  Reference:

    https://www.ietf.org/rfc/rfc4492.txt 
*/

#include <stdint.h>
#include "ecdh.h"


/* margin for overhead needed in intermediate calculations */
#define BITVEC_MARGIN     3
#define BITVEC_NBITS      (CURVE_DEGREE + BITVEC_MARGIN)
#define BITVEC_NWORDS     ((BITVEC_NBITS + 31) / 32)
#define BITVEC_NBYTES     (sizeof(uint32_t) * BITVEC_NWORDS)


/* Disable assertions? */
#ifndef DISABLE_ASSERT
 #define DISABLE_ASSERT 0
#endif

#if defined(DISABLE_ASSERT) && (DISABLE_ASSERT == 1)
 #define assert(...)
#else
 #include <assert.h>
#endif

/* Default to a (somewhat) constant-time mode?
   NOTE: The library is _not_ capable of operating in constant-time and leaks information via timing.
         Even if all operations are written const-time-style, it requires the hardware is able to multiply in constant time. 
         Multiplication on ARM Cortex-M processors takes a variable number of cycles depending on the operands...
*/
#ifndef CONST_TIME
  #define CONST_TIME 0
#endif

/* Default to using ECC_CDH (cofactor multiplication-variation) ? */
#ifndef ECDH_COFACTOR_VARIANT
  #define ECDH_COFACTOR_VARIANT 0
#endif

/******************************************************************************/


/* the following type will represent bit vectors of length (CURVE_DEGREE+MARGIN) */
typedef uint32_t bitvec_t[BITVEC_NWORDS];
typedef bitvec_t gf2elem_t;           /* this type will represent field elements */
typedef bitvec_t scalar_t;
 

/******************************************************************************/

/* Here the curve parameters are defined. */

#if defined (ECC_CURVE) && (ECC_CURVE != 0)
 #if (ECC_CURVE == NIST_K163)
  #define coeff_a  1
  #define cofactor 2
/* NIST K-163 */
const gf2elem_t polynomial = { 0x000000c9, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000008 }; 
const gf2elem_t coeff_b    = { 0x00000001, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 }; 
const gf2elem_t base_x     = { 0x5c94eee8, 0xde4e6d5e, 0xaa07d793, 0x7bbc11ac, 0xfe13c053, 0x00000002 }; 
const gf2elem_t base_y     = { 0xccdaa3d9, 0x0536d538, 0x321f2e80, 0x5d38ff58, 0x89070fb0, 0x00000002 }; 
const scalar_t  base_order = { 0x99f8a5ef, 0xa2e0cc0d, 0x00020108, 0x00000000, 0x00000000, 0x00000004 }; 
 #endif

 #if (ECC_CURVE == NIST_B163)
  #define coeff_a  1
  #define cofactor 2
/* NIST B-163 */
const gf2elem_t polynomial = { 0x000000c9, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000008 }; 
const gf2elem_t coeff_b    = { 0x4a3205fd, 0x512f7874, 0x1481eb10, 0xb8c953ca, 0x0a601907, 0x00000002 }; 
const gf2elem_t base_x     = { 0xe8343e36, 0xd4994637, 0xa0991168, 0x86a2d57e, 0xf0eba162, 0x00000003 }; 
const gf2elem_t base_y     = { 0x797324f1, 0xb11c5c0c, 0xa2cdd545, 0x71a0094f, 0xd51fbc6c, 0x00000000 }; 
const scalar_t  base_order = { 0xa4234c33, 0x77e70c12, 0x000292fe, 0x00000000, 0x00000000, 0x00000004 }; 
 #endif

 #if (ECC_CURVE == NIST_K233)
  #define coeff_a  0
  #define cofactor 4
/* NIST K-233 */
const gf2elem_t polynomial = { 0x00000001, 0x00000000, 0x00000400, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000200 };
const gf2elem_t coeff_b    = { 0x00000001, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 };
const gf2elem_t base_x     = { 0xefad6126, 0x0a4c9d6e, 0x19c26bf5, 0x149563a4, 0x29f22ff4, 0x7e731af1, 0x32ba853a, 0x00000172 };
const gf2elem_t base_y     = { 0x56fae6a3, 0x56e0c110, 0xf18aeb9b, 0x27a8cd9b, 0x555a67c4, 0x19b7f70f, 0x537dece8, 0x000001db };
const scalar_t  base_order = { 0xf173abdf, 0x6efb1ad5, 0xb915bcd4, 0x00069d5b, 0x00000000, 0x00000000, 0x00000000, 0x00000080 };
 #endif

 #if (ECC_CURVE == NIST_B233)
  #define coeff_a  1
  #define cofactor 2
/* NIST B-233 */
const gf2elem_t polynomial = { 0x00000001, 0x00000000, 0x00000400, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000200 }; 
const gf2elem_t coeff_b    = { 0x7d8f90ad, 0x81fe115f, 0x20e9ce42, 0x213b333b, 0x0923bb58, 0x332c7f8c, 0x647ede6c, 0x00000066 }; 
const gf2elem_t base_x     = { 0x71fd558b, 0xf8f8eb73, 0x391f8b36, 0x5fef65bc, 0x39f1bb75, 0x8313bb21, 0xc9dfcbac, 0x000000fa }; 
const gf2elem_t base_y     = { 0x01f81052, 0x36716f7e, 0xf867a7ca, 0xbf8a0bef, 0xe58528be, 0x03350678, 0x6a08a419, 0x00000100 }; 
const scalar_t  base_order = { 0x03cfe0d7, 0x22031d26, 0xe72f8a69, 0x0013e974, 0x00000000, 0x00000000, 0x00000000, 0x00000100 };
 #endif

 #if (ECC_CURVE == NIST_K283)
  #define coeff_a  0
  #define cofactor 4
/* NIST K-283 */
const gf2elem_t polynomial = { 0x000010a1, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x08000000 };
const gf2elem_t coeff_b    = { 0x00000001, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 }; 
const gf2elem_t base_x     = { 0x58492836, 0xb0c2ac24, 0x16876913, 0x23c1567a, 0x53cd265f, 0x62f188e5, 0x3f1a3b81, 0x78ca4488, 0x0503213f }; 
const gf2elem_t base_y     = { 0x77dd2259, 0x4e341161, 0xe4596236, 0xe8184698, 0xe87e45c0, 0x07e5426f, 0x8d90f95d, 0x0f1c9e31, 0x01ccda38 }; 
const scalar_t  base_order = { 0x1e163c61, 0x94451e06, 0x265dff7f, 0x2ed07577, 0xffffe9ae, 0xffffffff, 0xffffffff, 0xffffffff, 0x01ffffff }; 
 #endif

 #if (ECC_CURVE == NIST_B283)
  #define coeff_a  1
  #define cofactor 2
/* NIST B-283 */
const gf2elem_t polynomial = { 0x000010a1, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x08000000 }; 
const gf2elem_t coeff_b    = { 0x3b79a2f5, 0xf6263e31, 0xa581485a, 0x45309fa2, 0xca97fd76, 0x19a0303f, 0xa5a4af8a, 0xc8b8596d, 0x027b680a }; 
const gf2elem_t base_x     = { 0x86b12053, 0xf8cdbecd, 0x80e2e198, 0x557eac9c, 0x2eed25b8, 0x70b0dfec, 0xe1934f8c, 0x8db7dd90, 0x05f93925 }; 
const gf2elem_t base_y     = { 0xbe8112f4, 0x13f0df45, 0x826779c8, 0x350eddb0, 0x516ff702, 0xb20d02b4, 0xb98fe6d4, 0xfe24141c, 0x03676854 }; 
const scalar_t  base_order = { 0xefadb307, 0x5b042a7c, 0x938a9016, 0x399660fc, 0xffffef90, 0xffffffff, 0xffffffff, 0xffffffff, 0x03ffffff }; 
 #endif

 #if (ECC_CURVE == NIST_K409)
  #define coeff_a  0
  #define cofactor 4
/* NIST K-409 */
const gf2elem_t polynomial = { 0x00000001, 0x00000000, 0x00800000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x02000000 }; 
const gf2elem_t coeff_b    = { 0x00000001, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 }; 
const gf2elem_t base_x     = { 0xe9023746, 0xb35540cf, 0xee222eb1, 0xb5aaaa62, 0xc460189e, 0xf9f67cc2, 0x27accfb8, 0xe307c84c, 0x0efd0987, 0x0f718421, 0xad3ab189, 0x658f49c1, 0x0060f05f }; 
const gf2elem_t base_y     = { 0xd8e0286b, 0x5863ec48, 0xaa9ca27a, 0xe9c55215, 0xda5f6c42, 0xe9ea10e3, 0xe6325165, 0x918ea427, 0x3460782f, 0xbf04299c, 0xacba1dac, 0x0b7c4e42, 0x01e36905 }; 
const scalar_t  base_order = { 0xe01e5fcf, 0x4b5c83b8, 0xe3e7ca5b, 0x557d5ed3, 0x20400ec4, 0x83b2d4ea, 0xfffffe5f, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x007fffff }; 
 #endif

 #if (ECC_CURVE == NIST_B409)
  #define coeff_a  1
  #define cofactor 2
/* NIST B-409 */
const gf2elem_t polynomial = { 0x00000001, 0x00000000, 0x00800000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x02000000 }; 
const gf2elem_t coeff_b    = { 0x7b13545f, 0x4f50ae31, 0xd57a55aa, 0x72822f6c, 0xa9a197b2, 0xd6ac27c8, 0x4761fa99, 0xf1f3dd67, 0x7fd6422e, 0x3b7b476b, 0x5c4b9a75, 0xc8ee9feb, 0x0021a5c2 }; 
const gf2elem_t base_x     = { 0xbb7996a7, 0x60794e54, 0x5603aeab, 0x8a118051, 0xdc255a86, 0x34e59703, 0xb01ffe5b, 0xf1771d4d, 0x441cde4a, 0x64756260, 0x496b0c60, 0xd088ddb3, 0x015d4860 }; 
const gf2elem_t base_y     = { 0x0273c706, 0x81c364ba, 0xd2181b36, 0xdf4b4f40, 0x38514f1f, 0x5488d08f, 0x0158aa4f, 0xa7bd198d, 0x7636b9c5, 0x24ed106a, 0x2bbfa783, 0xab6be5f3, 0x0061b1cf }; 
const scalar_t  base_order = { 0xd9a21173, 0x8164cd37, 0x9e052f83, 0x5fa47c3c, 0xf33307be, 0xaad6a612, 0x000001e2, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x01000000 }; 
 #endif

 #if (ECC_CURVE == NIST_K571)
  #define coeff_a  0
  #define cofactor 4
/* NIST K-571 */
const gf2elem_t polynomial = { 0x00000425, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x08000000 }; 
const gf2elem_t coeff_b    = { 0x00000001, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000 }; 
const gf2elem_t base_x     = { 0xa01c8972, 0xe2945283, 0x4dca88c7, 0x988b4717, 0x494776fb, 0xbbd1ba39, 0xb4ceb08c, 0x47da304d, 0x93b205e6, 0x43709584, 0x01841ca4, 0x60248048, 0x0012d5d4, 0xac9ca297, 0xf8103fe4, 0x82189631, 0x59923fbc, 0x026eb7a8 }; 
const gf2elem_t base_y     = { 0x3ef1c7a3, 0x01cd4c14, 0x591984f6, 0x320430c8, 0x7ba7af1b, 0xb620b01a, 0xf772aedc, 0x4fbebbb9, 0xac44aea7, 0x9d4979c0, 0x006d8a2c, 0xffc61efc, 0x9f307a54, 0x4dd58cec, 0x3bca9531, 0x4f4aeade, 0x7f4fbf37, 0x0349dc80 }; 
const scalar_t  base_order = { 0x637c1001, 0x5cfe778f, 0x1e91deb4, 0xe5d63938, 0xb630d84b, 0x917f4138, 0xb391a8db, 0xf19a63e4, 0x131850e1, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x02000000 }; 
 #endif

 #if (ECC_CURVE == NIST_B571)
  #define coeff_a  1
  #define cofactor 2
/* NIST B-571 */
const gf2elem_t polynomial = { 0x00000425, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x08000000 }; 
const gf2elem_t coeff_b    = { 0x2955727a, 0x7ffeff7f, 0x39baca0c, 0x520e4de7, 0x78ff12aa, 0x4afd185a, 0x56a66e29, 0x2be7ad67, 0x8efa5933, 0x84ffabbd, 0x4a9a18ad, 0xcd6ba8ce, 0xcb8ceff1, 0x5c6a97ff, 0xb7f3d62f, 0xde297117, 0x2221f295, 0x02f40e7e }; 
const gf2elem_t base_x     = { 0x8eec2d19, 0xe1e7769c, 0xc850d927, 0x4abfa3b4, 0x8614f139, 0x99ae6003, 0x5b67fb14, 0xcdd711a3, 0xf4c0d293, 0xbde53950, 0xdb7b2abd, 0xa5f40fc8, 0x955fa80a, 0x0a93d1d2, 0x0d3cd775, 0x6c16c0d4, 0x34b85629, 0x0303001d }; 
const gf2elem_t base_y     = { 0x1b8ac15b, 0x1a4827af, 0x6e23dd3c, 0x16e2f151, 0x0485c19b, 0xb3531d2f, 0x461bb2a8, 0x6291af8f, 0xbab08a57, 0x84423e43, 0x3921e8a6, 0x1980f853, 0x009cbbca, 0x8c6c27a6, 0xb73d69d7, 0x6dccfffe, 0x42da639b, 0x037bf273 }; 
const scalar_t  base_order = { 0x2fe84e47, 0x8382e9bb, 0x5174d66e, 0x161de93d, 0xc7dd9ca1, 0x6823851e, 0x08059b18, 0xff559873, 0xe661ce18, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0x03ffffff }; 
 #endif
#endif



/*************************************************************************************************/

/* Private / static functions: */


/* some basic bit-manipulation routines that act on bit-vectors follow */
static int bitvec_get_bit(const bitvec_t x, const uint32_t idx)
{
  return ((x[idx / 32U] >> (idx & 31U) & 1U));
}

static void bitvec_clr_bit(bitvec_t x, const uint32_t idx)
{
  x[idx / 32U] &= ~(1U << (idx & 31U));
}

static void bitvec_copy(bitvec_t x, const bitvec_t y)
{
  int i;
  for (i = 0; i < BITVEC_NWORDS; ++i)
  {
    x[i] = y[i];
  }
}

static void bitvec_swap(bitvec_t x, bitvec_t y)
{
  bitvec_t tmp;
  bitvec_copy(tmp, x);
  bitvec_copy(x, y);
  bitvec_copy(y, tmp);
}

#if defined(CONST_TIME) && (CONST_TIME == 0)
/* fast version of equality test */
static int bitvec_equal(const bitvec_t x, const bitvec_t y)
{
  int i;
  for (i = 0; i < BITVEC_NWORDS; ++i)
  {
    if (x[i] != y[i])
    {
      return 0;
    }
  }
  return 1;
}
#else
/* constant time version of equality test */
static int bitvec_equal(const bitvec_t x, const bitvec_t y)
{
  int ret = 1;
  int i;
  for (i = 0; i < BITVEC_NWORDS; ++i)
  {
    ret &= (x[i] == y[i]);
  }
  return ret;
}
#endif

static void bitvec_set_zero(bitvec_t x)
{
  int i;
  for (i = 0; i < BITVEC_NWORDS; ++i)
  {
    x[i] = 0;
  }
}

#if defined(CONST_TIME) && (CONST_TIME == 0)
/* fast implementation */
static int bitvec_is_zero(const bitvec_t x)
{
  uint32_t i = 0;
  while (i < BITVEC_NWORDS)
  {
    if (x[i] != 0)
    {
      break;
    }
    i += 1;
  }
  return (i == BITVEC_NWORDS);
}
#else
/* constant-time implementation */
static int bitvec_is_zero(const bitvec_t x)
{
  int ret = 1;
  int i = 0;
  for (i = 0; i < BITVEC_NWORDS; ++i)
  {
    ret &= (x[i] == 0);
  }
  return ret;
}
#endif

/* return the number of the highest one-bit + 1 */
static int bitvec_degree(const bitvec_t x)
{
  int i = BITVEC_NWORDS * 32;

  /* Start at the back of the vector (MSB) */
  x += BITVEC_NWORDS;

  /* Skip empty / zero words */
  while (    (i > 0)
          && (*(--x)) == 0)
  {
    i -= 32;
  }
  /* Run through rest if count is not multiple of bitsize of DTYPE */
  if (i != 0)
  {
    uint32_t u32mask = ((uint32_t)1 << 31);
    while (((*x) & u32mask) == 0)
    {
      u32mask >>= 1;
      i -= 1;
    }
  }
  return i;
}

/* left-shift by 'count' digits */
static void bitvec_lshift(bitvec_t x, const bitvec_t y, int nbits)
{
  int nwords = (nbits / 32);

  /* Shift whole words first if nwords > 0 */
  int i,j;
  for (i = 0; i < nwords; ++i)
  {
    /* Zero-initialize from least-significant word until offset reached */
    x[i] = 0;
  }
  j = 0;
  /* Copy to x output */
  while (i < BITVEC_NWORDS)
  {
    x[i] = y[j];
    i += 1;
    j += 1;
  }

  /* Shift the rest if count was not multiple of bitsize of DTYPE */
  nbits &= 31;
  if (nbits != 0)
  {
    /* Left shift rest */
    int i;
    for (i = (BITVEC_NWORDS - 1); i > 0; --i)
    {
      x[i]  = (x[i] << nbits) | (x[i - 1] >> (32 - nbits));
    }
    x[0] <<= nbits;
  }
}


/*************************************************************************************************/
/*
  Code that does arithmetic on bit-vectors in the Galois Field GF(2^CURVE_DEGREE).
*/
/*************************************************************************************************/


static void gf2field_set_one(gf2elem_t x)
{
  /* Set first word to one */
  x[0] = 1;
  /* .. and the rest to zero */
  int i;
  for (i = 1; i < BITVEC_NWORDS; ++i)
  {
    x[i] = 0;
  }
}

#if defined(CONST_TIME) && (CONST_TIME == 0)
/* fastest check if x == 1 */
static int gf2field_is_one(const gf2elem_t x) 
{
  /* Check if first word == 1 */
  if (x[0] != 1)
  {
    return 0;
  }
  /* ...and if rest of words == 0 */
  int i;
  for (i = 1; i < BITVEC_NWORDS; ++i)
  {
    if (x[i] != 0)
    {
      break;
    }
  }
  return (i == BITVEC_NWORDS);
}
#else
/* constant-time check */
static int gf2field_is_one(const gf2elem_t x)
{
  int ret = 0;
  /* Check if first word == 1 */
  if (x[0] == 1)
  {
    ret = 1;
  }
  /* ...and if rest of words == 0 */
  int i;
  for (i = 1; i < BITVEC_NWORDS; ++i)
  {
    ret &= (x[i] == 0);
  }
  return ret; //(i == BITVEC_NWORDS);
}
#endif


/* galois field(2^m) addition is modulo 2, so XOR is used instead - 'z := a + b' */
static void gf2field_add(gf2elem_t z, const gf2elem_t x, const gf2elem_t y)
{
  int i;
  for (i = 0; i < BITVEC_NWORDS; ++i)
  {
    z[i] = (x[i] ^ y[i]);
  }
}

/* increment element */
static void gf2field_inc(gf2elem_t x)
{
  x[0] ^= 1;
}


/* field multiplication 'z := (x * y)' */
static void gf2field_mul(gf2elem_t z, const gf2elem_t x, const gf2elem_t y)
{
  int i;
  gf2elem_t tmp;
#if defined(CONST_TIME) && (CONST_TIME == 1)
  gf2elem_t blind;
  bitvec_set_zero(blind);
#endif
  assert(z != y);

  bitvec_copy(tmp, x);

  /* LSB set? Then start with x */
  if (bitvec_get_bit(y, 0) != 0)
  {
    bitvec_copy(z, x);
  }
  else /* .. or else start with zero */
  {
    bitvec_set_zero(z);
  }

  /* Then add 2^i * x for the rest */
  for (i = 1; i < CURVE_DEGREE; ++i)
  {
    /* lshift 1 - doubling the value of tmp */
    bitvec_lshift(tmp, tmp, 1);

    /* Modulo reduction polynomial if degree(tmp) > CURVE_DEGREE */
    if (bitvec_get_bit(tmp, CURVE_DEGREE))
    {
      gf2field_add(tmp, tmp, polynomial);
    }
#if defined(CONST_TIME) && (CONST_TIME == 1)
    else /* blinding operation */
    {
      gf2field_add(tmp, tmp, blind);
    }
#endif

    /* Add 2^i * tmp if this factor in y is non-zero */
    if (bitvec_get_bit(y, i))
    {
      gf2field_add(z, z, tmp);
    }
#if defined(CONST_TIME) && (CONST_TIME == 1)
    else /* blinding operation */
    {
      gf2field_add(z, z, blind);
    }
#endif
  }
}

/* field inversion 'z := 1/x' */
static void gf2field_inv(gf2elem_t z, const gf2elem_t x)
{
  gf2elem_t u, v, g, h;
  int i;

  bitvec_copy(u, x);
  bitvec_copy(v, polynomial);
  bitvec_set_zero(g);
  gf2field_set_one(z);
  
  while (!gf2field_is_one(u))
  {
    i = (bitvec_degree(u) - bitvec_degree(v));

    if (i < 0)
    {
      bitvec_swap(u, v);
      bitvec_swap(g, z);
      i = -i;
    }
#if defined(CONST_TIME) && (CONST_TIME == 1)
    else
    {
      bitvec_swap(u, v);
      bitvec_swap(v, u);
    }
#endif
    bitvec_lshift(h, v, i);
    gf2field_add(u, u, h);
    bitvec_lshift(h, g, i);
    gf2field_add(z, z, h);
  }
}

/*************************************************************************************************/
/*
   The following code takes care of Galois-Field arithmetic. 
   Elliptic curve points are represented  by pairs (x,y) of bitvec_t. 
   It is assumed that curve coefficient 'a' is {0,1}
   This is the case for all NIST binary curves.
   Coefficient 'b' is given in 'coeff_b'.
   '(base_x, base_y)' is a point that generates a large prime order group.
*/
/*************************************************************************************************/


static void gf2point_copy(gf2elem_t x1, gf2elem_t y1, const gf2elem_t x2, const gf2elem_t y2)
{
  bitvec_copy(x1, x2);
  bitvec_copy(y1, y2);
}

static void gf2point_set_zero(gf2elem_t x, gf2elem_t y)
{
  bitvec_set_zero(x);
  bitvec_set_zero(y);
}

static int gf2point_is_zero(const gf2elem_t x, const gf2elem_t y)
{
  return (    bitvec_is_zero(x)
           && bitvec_is_zero(y));
}

/* double the point (x,y) */
static void gf2point_double(gf2elem_t x, gf2elem_t y)
{
  /* iff P = O (zero or infinity): 2 * P = P */
  if (bitvec_is_zero(x))
  {
    bitvec_set_zero(y);
  }
  else
  {
    gf2elem_t l;

    gf2field_inv(l, x);
    gf2field_mul(l, l, y);
    gf2field_add(l, l, x);
    gf2field_mul(y, x, x);
    gf2field_mul(x, l, l);
#if (coeff_a == 1)
    gf2field_inc(l);
#endif
    gf2field_add(x, x, l);
    gf2field_mul(l, l, x);
    gf2field_add(y, y, l);
  }
}


/* add two points together (x1, y1) := (x1, y1) + (x2, y2) */
static void gf2point_add(gf2elem_t x1, gf2elem_t y1, const gf2elem_t x2, const gf2elem_t y2)
{
  if (!gf2point_is_zero(x2, y2))
  {
    if (gf2point_is_zero(x1, y1))
    {
      gf2point_copy(x1, y1, x2, y2);
    }
    else
    {
      if (bitvec_equal(x1, x2))
      {
        if (bitvec_equal(y1, y2))
        {
          gf2point_double(x1, y1);
        }
        else
        {
          gf2point_set_zero(x1, y1);
        }
      }
      else
      {
        /* Arithmetic with temporary variables */
        gf2elem_t a, b, c, d;

        gf2field_add(a, y1, y2);
        gf2field_add(b, x1, x2);
        gf2field_inv(c, b);
        gf2field_mul(c, c, a);
        gf2field_mul(d, c, c);
        gf2field_add(d, d, c);
        gf2field_add(d, d, b);
#if (coeff_a == 1)
        gf2field_inc(d);
#endif
        gf2field_add(x1, x1, d);
        gf2field_mul(a, x1, c);
        gf2field_add(a, a, d);
        gf2field_add(y1, y1, a);
        bitvec_copy(x1, d);
      }
    }
  }
}



#if defined(CONST_TIME) && (CONST_TIME == 0)
/* point multiplication via double-and-add algorithm */
static void gf2point_mul(gf2elem_t x, gf2elem_t y, const scalar_t exp)
{
  gf2elem_t tmpx, tmpy;
  int i;
  int nbits = bitvec_degree(exp);

  gf2point_set_zero(tmpx, tmpy);

  for (i = (nbits - 1); i >= 0; --i)
  {
    gf2point_double(tmpx, tmpy);
    if (bitvec_get_bit(exp, i))
    {
      gf2point_add(tmpx, tmpy, x, y);
    }
  }
  gf2point_copy(x, y, tmpx, tmpy);
}
#else
/* point multiplication via double-and-add-always algorithm using scalar blinding */
static void gf2point_mul(gf2elem_t x, gf2elem_t y, const scalar_t exp)
{
  gf2elem_t tmpx, tmpy;
  gf2elem_t dummyx, dummyy;
  int i;
  int nbits = bitvec_degree(exp);

  gf2point_set_zero(tmpx, tmpy);
  gf2point_set_zero(dummyx, dummyy);

  for (i = (nbits - 1); i >= 0; --i)
  {
    gf2point_double(tmpx, tmpy);

    /* Add point if bit(i) is set in exp */
    if (bitvec_get_bit(exp, i))
    {
      gf2point_add(tmpx, tmpy, x, y);
    }
    /* .. or add the neutral element to keep operation constant-time */
    else
    {
      gf2point_add(tmpx, tmpy, dummyx, dummyy);
    }
  }
  gf2point_copy(x, y, tmpx, tmpy);
}
#endif



/* check if y^2 + x*y = x^3 + a*x^2 + coeff_b holds */
static int gf2point_on_curve(const gf2elem_t x, const gf2elem_t y)
{
  gf2elem_t a, b;

  if (gf2point_is_zero(x, y))
  {
    return 1;
  }
  else
  {
    gf2field_mul(a, x, x);
#if (coeff_a == 0)
    gf2field_mul(a, a, x);
#else
    gf2field_mul(b, a, x);
    gf2field_add(a, a, b);
#endif
    gf2field_add(a, a, coeff_b);
    gf2field_mul(b, y, y);
    gf2field_add(a, a, b);
    gf2field_mul(b, x, y);

    return bitvec_equal(a, b);
  }
}


/*************************************************************************************************/
/*
  Elliptic Curve Diffie-Hellman key exchange protocol.
*/
/*************************************************************************************************/



/* NOTE: private should contain random data a-priori! */
int ecdh_generate_keys(uint8_t* public_key, uint8_t* private_key)
{
  /* Get copy of "base" point 'G' */
  gf2point_copy((uint32_t*)public_key, (uint32_t*)(public_key + BITVEC_NBYTES), base_x, base_y);

  /* Abort key generation if random number is too small */
  if (bitvec_degree((uint32_t*)private_key) < (CURVE_DEGREE / 2))
  {
    return 0;
  }
  else
  {
    /* Clear bits > CURVE_DEGREE in highest word to satisfy constraint 1 <= exp < n. */
    int nbits = bitvec_degree(base_order);
    int i;

    for (i = (nbits - 1); i < (BITVEC_NWORDS * 32); ++i)
    {
      bitvec_clr_bit((uint32_t*)private_key, i);
    }

    /* Multiply base-point with scalar (private-key) */
    gf2point_mul((uint32_t*)public_key, (uint32_t*)(public_key + BITVEC_NBYTES), (uint32_t*)private_key);

    return 1;
  }
}



int ecdh_shared_secret(const uint8_t* private_key, const uint8_t* others_pub, uint8_t* output)
{
  /* Do some basic validation of other party's public key */
  if (    !gf2point_is_zero ((uint32_t*)others_pub, (uint32_t*)(others_pub + BITVEC_NBYTES))
       &&  gf2point_on_curve((uint32_t*)others_pub, (uint32_t*)(others_pub + BITVEC_NBYTES)) )
  {
    /* Copy other side's public key to output */
    unsigned int i;
    for (i = 0; i < (BITVEC_NBYTES * 2); ++i)
    {
      output[i] = others_pub[i];
    }

    /* Multiply other side's public key with own private key */
    gf2point_mul((uint32_t*)output,(uint32_t*)(output + BITVEC_NBYTES), (const uint32_t*)private_key);

    /* Multiply outcome by cofactor if using ECC CDH-variant: */
#if defined(ECDH_COFACTOR_VARIANT) && (ECDH_COFACTOR_VARIANT == 1)
 #if   (cofactor == 2)
    gf2point_double((uint32_t*)output, (uint32_t*)(output + BITVEC_NBYTES));
 #elif (cofactor == 4)
    gf2point_double((uint32_t*)output, (uint32_t*)(output + BITVEC_NBYTES));
    gf2point_double((uint32_t*)output, (uint32_t*)(output + BITVEC_NBYTES));
 #endif
#endif
    
    return 1;
  }
  else
  {
    return 0;
  }
}


/* ECDSA is broken :( ... */
int ecdsa_sign(const uint8_t* private_key, uint8_t* hash, uint8_t* random_k, uint8_t* signature)
{
  /*
     1) calculate e = HASH(m)
     2) let z be the Ln leftmost bits of e, where Ln is the bit length of the group order n
     3) Select a cryptographically secure random integer k from [1, n-1]
     4) Calculate the curve point (x1, y1) = k * G
     5) Calculate r = x1 mod n - if (r == 0) goto 3
     6) Calculate s = inv(k) * (z + r * d) mod n - if (s == 0) goto 3
     7) The signature is the pair (r, s)
  */
  assert(private_key != 0);
  assert(hash != 0);
  assert(random_k != 0);
  assert(signature != 0);

  int success = 0;

  if (    (bitvec_degree((uint32_t*)private_key) >= (CURVE_DEGREE / 2))
       && !bitvec_is_zero((uint32_t*)random_k) )
  {
    gf2elem_t r, s, z, k;

    bitvec_set_zero(r);
    bitvec_set_zero(s);
    bitvec_copy(z, (uint32_t*)hash);

    /* 1 + 2 */
    int nbits = bitvec_degree(base_order);
    int i;
    for (i = (nbits - 1); i < BITVEC_NBITS; ++i)
    {
      bitvec_clr_bit(z, i);
    }

    /* 3 */
    bitvec_copy(k, (uint32_t*)random_k);

    /* 4 */
    gf2point_copy(r, s, base_x, base_y);
    gf2point_mul(r, s, k);

    /* 5 */
    if (!bitvec_is_zero(r))
    {
      /* 6) s = inv(k) * (z + (r * d)) mod n ==> if (s == 0) goto 3 **/
      gf2field_inv(s, k);                     /* s = inv(k) */
      gf2field_mul(r, r, (uint32_t*)private_key); /* r = (r * d) */
      gf2field_add(r, r, z);                  /* r = z + (r * d) */

      nbits = bitvec_degree(r); /* r = r mod n */
      for (i = (nbits - 1); i < BITVEC_NBITS; ++i)
      {
        printf("reduction r\n");
        bitvec_clr_bit(r, i);
      }
      
      gf2field_mul(s, s, r);                  /* s = inv(k) * (z * (r * d)) */

      nbits = bitvec_degree(s); /* s = s mod n */
      for (i = (nbits - 1); i < BITVEC_NBITS; ++i)
      {
        printf("reduction s\n");
        bitvec_clr_bit(s, i);
      }

      if (!bitvec_is_zero(s))
      {
        bitvec_copy((uint32_t*)signature, r);
        bitvec_copy((uint32_t*)(signature + ECC_PRV_KEY_SIZE), s);
        success = 1;
      }
    }
  }
  return success;
}


int ecdsa_verify(const uint8_t* public_key, uint8_t* hash, const uint8_t* signature)
{
  /*
    1) Verify that (r,s) are in [1, n-1]
    2) e = HASH(m)
    3) z = Ln leftmost bits of e
    4) w = inv(s) mod n
    5) u1 = (z * w) mod n
       u2 = (r * w) mod n
    6) (x,y) = (u1 * G) + (u2 * public)
    7) Signature is valid if r == x mod n && (x,y) != (0,0)
  */
  assert(public_key != 0);
  assert(hash != 0);
  assert(signature != 0);

  int success = 0;

  gf2elem_t r, s;
  bitvec_copy(r, (uint32_t*)(signature));
  bitvec_copy(s, (uint32_t*)(signature + ECC_PRV_KEY_SIZE));

  if (    !bitvec_is_zero(s)
       && !bitvec_is_zero(r))
  {
    gf2elem_t x1, y1, u1, u2, w, z;

    /* 3) z = Ln leftmost bits of e */
    bitvec_copy(z, (uint32_t*)hash); /* r,s,z are set */
    uint32_t nbits = bitvec_degree(base_order);
    uint32_t i;
    for (i = (nbits - 1); i < BITVEC_NBITS; ++i)
    {
      bitvec_clr_bit(z, i);
    }
    
    /* 4) w = inv(s) mod n */
    gf2field_inv(w, s); /* w = inv(s) */
    /* Modulo reduction polynomial if degree(tmp) > CURVE_DEGREE */
    if (bitvec_get_bit(w, CURVE_DEGREE))
    {
      printf("reduction on w\n");
      gf2field_add(w, w, polynomial);
    }

    /* 5) u1 = zw mod n, u2 = rw mod n*/
    gf2field_mul(u1, z, w); /* u1 = z * w */
    /* Modulo reduction polynomial if degree(tmp) > CURVE_DEGREE */
    if (bitvec_get_bit(u1, CURVE_DEGREE))
    {
      printf("reduction on u1\n");
      gf2field_add(u1, u1, polynomial);
    }
    gf2field_mul(u2, r, w); /* u2 = r * w */
    /* Modulo reduction polynomial if degree(tmp) > CURVE_DEGREE */
    if (bitvec_get_bit(u2, CURVE_DEGREE))
    {
      printf("reduction on u2\n");
      gf2field_add(u2, u2, polynomial);
    }

    /* 6) (x,y) = (u1 * G) + (u2 * public) */
    bitvec_copy(x1, base_x);
    bitvec_copy(y1, base_y);
    gf2field_mul(u1, x1, y1);  /* u1 * G */

    bitvec_copy(w, (uint32_t*)(public_key));
    bitvec_copy(z, (uint32_t*)(public_key + ECC_PRV_KEY_SIZE));
    gf2field_mul(u2, w, z); /* u2 * Q */

    
    gf2point_add(x1, y1, w, z);
    if (bitvec_get_bit(x1, CURVE_DEGREE))
    {
      printf("reduction on x1\n");
      gf2field_add(x1, x1, polynomial);
    }

    success = bitvec_equal(r, x1);

    if (!success)
    {
      printf("x = '");
      for (i = 0; i < BITVEC_NWORDS; ++i)
      {
        printf("%.08x", x1[i]);
      }
      printf("' [%u]\n", i);
      printf("r = '");
      for (i = 0; i < BITVEC_NWORDS; ++i)
      {
        printf("%.08x", r[i]);
      }
      printf("' [%u]\n", i);
    }
  }
  else
  {
    printf("(s or r) == zero\n");
  }

  return success;
}



