
package com.coinomi.core.coins.nxt;

/* Ported from C to Java by Dmitry Skiba [sahn0], 23/02/08.
 * Original: http://cds.xs4all.nl:8081/ecdh/
 */
/* Generic 64-bit integer implementation of Curve25519 ECDH
 * Written by Matthijs van Duin, 200608242056
 * Public domain.
 *
 * Based on work by Daniel J Bernstein, http://cr.yp.to/ecdh.html
 */
final class Curve25519 {

    /* key size */
    public static final int KEY_SIZE = 32;

    /* 0 */
    public static final byte[] ZERO = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    /* the prime 2^255-19 */
    public static final byte[] PRIME = {
            (byte)237, (byte)255, (byte)255, (byte)255,
            (byte)255, (byte)255, (byte)255, (byte)255,
            (byte)255, (byte)255, (byte)255, (byte)255,
            (byte)255, (byte)255, (byte)255, (byte)255,
            (byte)255, (byte)255, (byte)255, (byte)255,
            (byte)255, (byte)255, (byte)255, (byte)255,
            (byte)255, (byte)255, (byte)255, (byte)255,
            (byte)255, (byte)255, (byte)255, (byte)127
    };

    /* group order (a prime near 2^252+2^124) */
    public static final byte[] ORDER = {
            (byte)237, (byte)211, (byte)245, (byte)92,
            (byte)26,  (byte)99,  (byte)18,  (byte)88,
            (byte)214, (byte)156, (byte)247, (byte)162,
            (byte)222, (byte)249, (byte)222, (byte)20,
            (byte)0,   (byte)0,   (byte)0,   (byte)0,
            (byte)0,   (byte)0,   (byte)0,   (byte)0,
            (byte)0,   (byte)0,   (byte)0,   (byte)0,
            (byte)0,   (byte)0,   (byte)0,   (byte)16
    };

    /********* KEY AGREEMENT *********/

    /* Private key clamping
     *   k [out] your private key for key agreement
     *   k  [in]  32 random bytes
     */
    public static void clamp(byte[] k) {
        k[31] &= 0x7F;
        k[31] |= 0x40;
        k[ 0] &= 0xF8;
    }

    /* Key-pair generation
     *   P  [out] your public key
     *   s  [out] your private key for signing
     *   k  [out] your private key for key agreement
     *   k  [in]  32 random bytes
     * s may be NULL if you don't care
     *
     * WARNING: if s is not NULL, this function has data-dependent timing */
    public static void keygen(byte[] P, byte[] s, byte[] k) {
        clamp(k);
        core(P, s, k, null);
    }

    /* Key agreement
     *   Z  [out] shared secret (needs hashing before use)
     *   k  [in]  your private key for key agreement
     *   P  [in]  peer's public key
     */
    public static void curve(byte[] Z, byte[] k, byte[] P) {
        core(Z, null, k, P);
    }

    /********* DIGITAL SIGNATURES *********/

    /* deterministic EC-KCDSA
     *
     *    s is the private key for signing
     *    P is the corresponding public key
     *    Z is the context data (signer public key or certificate, etc)
     *
     * signing:
     *
     *    m = hash(Z, message)
     *    x = hash(m, s)
     *    keygen25519(Y, NULL, x);
     *    r = hash(Y);
     *    h = m XOR r
     *    sign25519(v, h, x, s);
     *
     *    output (v,r) as the signature
     *
     * verification:
     *
     *    m = hash(Z, message);
     *    h = m XOR r
     *    verify25519(Y, v, h, P)
     *
     *    confirm  r == hash(Y)
     *
     * It would seem to me that it would be simpler to have the signer directly do
     * h = hash(m, Y) and send that to the recipient instead of r, who can verify
     * the signature by checking h == hash(m, Y).  If there are any problems with
     * such a scheme, please let me know.
     *
     * Also, EC-KCDSA (like most DS algorithms) picks x random, which is a waste of
     * perfectly good entropy, but does allow Y to be calculated in advance of (or
     * parallel to) hashing the message.
     */

    /* Signature generation primitive, calculates (x-h)s mod q
     *   v  [out] signature value
     *   h  [in]  signature hash (of message, signature pub key, and context data)
     *   x  [in]  signature private key
     *   s  [in]  private key for signing
     * returns true on success, false on failure (use different x or h)
     */
    public static boolean sign(byte[] v, byte[] h, byte[] x, byte[] s) {
        // v = (x - h) s  mod q
        int w, i;
        byte[] h1 = new byte[32], x1 = new byte[32];
        byte[] tmp1 = new byte[64];
        byte[] tmp2 = new byte[64];

        // Don't clobber the arguments, be nice!
        cpy32(h1, h);
        cpy32(x1, x);

        // Reduce modulo group order
        byte[] tmp3=new byte[32];
        divmod(tmp3, h1, 32, ORDER, 32);
        divmod(tmp3, x1, 32, ORDER, 32);

        // v = x1 - h1
        // If v is negative, add the group order to it to become positive.
        // If v was already positive we don't have to worry about overflow
        // when adding the order because v < ORDER and 2*ORDER < 2^256
        mula_small(v, x1, 0, h1, 32, -1);
        mula_small(v, v , 0, ORDER, 32, 1);

        // tmp1 = (x-h)*s mod q
        mula32(tmp1, v, s, 32, 1);
        divmod(tmp2, tmp1, 64, ORDER, 32);

        for (w = 0, i = 0; i < 32; i++)
            w |= v[i] = tmp1[i];
        return w != 0;
    }

    /* Signature verification primitive, calculates Y = vP + hG
     *   Y  [out] signature public key
     *   v  [in]  signature value
     *   h  [in]  signature hash
     *   P  [in]  public key
     */
    public static void verify(byte[] Y, byte[] v, byte[] h, byte[] P) {
        /* Y = v abs(P) + h G  */
        byte[] d=new byte[32];
        long10[]
                p=new long10[]{new long10(),new long10()},
                s=new long10[]{new long10(),new long10()},
                yx=new long10[]{new long10(),new long10(),new long10()},
                yz=new long10[]{new long10(),new long10(),new long10()},
                t1=new long10[]{new long10(),new long10(),new long10()},
                t2=new long10[]{new long10(),new long10(),new long10()};

        int vi = 0, hi = 0, di = 0, nvh=0, i, j, k;

        /* set p[0] to G and p[1] to P  */

        set(p[0], 9);
        unpack(p[1], P);

        /* set s[0] to P+G and s[1] to P-G  */

        /* s[0] = (Py^2 + Gy^2 - 2 Py Gy)/(Px - Gx)^2 - Px - Gx - 486662  */
        /* s[1] = (Py^2 + Gy^2 + 2 Py Gy)/(Px - Gx)^2 - Px - Gx - 486662  */

        x_to_y2(t1[0], t2[0], p[1]);	/* t2[0] = Py^2  */
        sqrt(t1[0], t2[0]);	/* t1[0] = Py or -Py  */
        j = is_negative(t1[0]);		/*      ... check which  */
        t2[0]._0 += 39420360;		/* t2[0] = Py^2 + Gy^2  */
        mul(t2[1], BASE_2Y, t1[0]);/* t2[1] = 2 Py Gy or -2 Py Gy  */
        sub(t1[j], t2[0], t2[1]);	/* t1[0] = Py^2 + Gy^2 - 2 Py Gy  */
        add(t1[1-j], t2[0], t2[1]);/* t1[1] = Py^2 + Gy^2 + 2 Py Gy  */
        cpy(t2[0], p[1]);		/* t2[0] = Px  */
        t2[0]._0 -= 9;			/* t2[0] = Px - Gx  */
        sqr(t2[1], t2[0]);		/* t2[1] = (Px - Gx)^2  */
        recip(t2[0], t2[1], 0);	/* t2[0] = 1/(Px - Gx)^2  */
        mul(s[0], t1[0], t2[0]);	/* s[0] = t1[0]/(Px - Gx)^2  */
        sub(s[0], s[0], p[1]);	/* s[0] = t1[0]/(Px - Gx)^2 - Px  */
        s[0]._0 -= 9 + 486662;		/* s[0] = X(P+G)  */
        mul(s[1], t1[1], t2[0]);	/* s[1] = t1[1]/(Px - Gx)^2  */
        sub(s[1], s[1], p[1]);	/* s[1] = t1[1]/(Px - Gx)^2 - Px  */
        s[1]._0 -= 9 + 486662;		/* s[1] = X(P-G)  */
        mul_small(s[0], s[0], 1);	/* reduce s[0] */
        mul_small(s[1], s[1], 1);	/* reduce s[1] */


        /* prepare the chain  */
        for (i = 0; i < 32; i++) {
            vi = (vi >> 8) ^ (v[i] & 0xFF) ^ ((v[i] & 0xFF) << 1);
            hi = (hi >> 8) ^ (h[i] & 0xFF) ^ ((h[i] & 0xFF) << 1);
            nvh = ~(vi ^ hi);
            di = (nvh & (di & 0x80) >> 7) ^ vi;
            di ^= nvh & (di & 0x01) << 1;
            di ^= nvh & (di & 0x02) << 1;
            di ^= nvh & (di & 0x04) << 1;
            di ^= nvh & (di & 0x08) << 1;
            di ^= nvh & (di & 0x10) << 1;
            di ^= nvh & (di & 0x20) << 1;
            di ^= nvh & (di & 0x40) << 1;
            d[i] = (byte)di;
        }

        di = ((nvh & (di & 0x80) << 1) ^ vi) >> 8;

        /* initialize state */
        set(yx[0], 1);
        cpy(yx[1], p[di]);
        cpy(yx[2], s[0]);
        set(yz[0], 0);
        set(yz[1], 1);
        set(yz[2], 1);

        /* y[0] is (even)P + (even)G
         * y[1] is (even)P + (odd)G  if current d-bit is 0
         * y[1] is (odd)P + (even)G  if current d-bit is 1
         * y[2] is (odd)P + (odd)G
         */

        vi = 0;
        hi = 0;

        /* and go for it! */
        for (i = 32; i--!=0; ) {
            vi = (vi << 8) | (v[i] & 0xFF);
            hi = (hi << 8) | (h[i] & 0xFF);
            di = (di << 8) | (d[i] & 0xFF);

            for (j = 8; j--!=0; ) {
                mont_prep(t1[0], t2[0], yx[0], yz[0]);
                mont_prep(t1[1], t2[1], yx[1], yz[1]);
                mont_prep(t1[2], t2[2], yx[2], yz[2]);

                k = ((vi ^ vi >> 1) >> j & 1)
                        + ((hi ^ hi >> 1) >> j & 1);
                mont_dbl(yx[2], yz[2], t1[k], t2[k], yx[0], yz[0]);

                k = (di >> j & 2) ^ ((di >> j & 1) << 1);
                mont_add(t1[1], t2[1], t1[k], t2[k], yx[1], yz[1],
                        p[di >> j & 1]);

                mont_add(t1[2], t2[2], t1[0], t2[0], yx[2], yz[2],
                        s[((vi ^ hi) >> j & 2) >> 1]);
            }
        }

        k = (vi & 1) + (hi & 1);
        recip(t1[0], yz[k], 0);
        mul(t1[1], yx[k], t1[0]);

        pack(t1[1], Y);
    }

    public static boolean isCanonicalSignature(byte[] v) {
        byte[] vCopy = java.util.Arrays.copyOfRange(v, 0, 32);
        byte[] tmp = new byte[32];
        divmod(tmp, vCopy, 32, ORDER, 32);
        for (int i = 0; i < 32; i++){
            if (v[i] != vCopy[i])
                return false;
        }
        return true;
    }

    public static boolean isCanonicalPublicKey(byte[] publicKey) {
        if ( publicKey.length != 32 ) {
            return false;
        }
        long10 publicKeyUnpacked = new long10();
        unpack(publicKeyUnpacked, publicKey);
        byte[] publicKeyCopy = new byte[32];
        pack(publicKeyUnpacked, publicKeyCopy);
        for (int i = 0; i < 32; i++){
            if (publicKeyCopy[i] != publicKey[i]) {
                return false;
            }
        }
        return true;
    }


    ///////////////////////////////////////////////////////////////////////////

    /* sahn0:
     * Using this class instead of long[10] to avoid bounds checks. */
    private static final class long10 {
        public long10() {}
        public long10(
                long _0, long _1, long _2, long _3, long _4,
                long _5, long _6, long _7, long _8, long _9)
        {
            this._0=_0; this._1=_1; this._2=_2;
            this._3=_3; this._4=_4; this._5=_5;
            this._6=_6; this._7=_7; this._8=_8;
            this._9=_9;
        }
        public long _0,_1,_2,_3,_4,_5,_6,_7,_8,_9;
    }

    /********************* radix 2^8 math *********************/

    private static void cpy32(byte[] d, byte[] s) {
        int i;
        for (i = 0; i < 32; i++)
            d[i] = s[i];
    }

    /* p[m..n+m-1] = q[m..n+m-1] + z * x */
    /* n is the size of x */
    /* n+m is the size of p and q */
    private static int mula_small(byte[] p,byte[] q,int m,byte[] x,int n,int z) {
        int v=0;
        for (int i=0;i<n;++i) {
            v+=(q[i+m] & 0xFF)+z*(x[i] & 0xFF);
            p[i+m]=(byte)v;
            v>>=8;
        }
        return v;
    }

    /* p += x * y * z  where z is a small integer
     * x is size 32, y is size t, p is size 32+t
     * y is allowed to overlap with p+32 if you don't care about the upper half  */
    private static int mula32(byte[] p, byte[] x, byte[] y, int t, int z) {
        final int n = 31;
        int w = 0;
        int i = 0;
        for (; i < t; i++) {
            int zy = z * (y[i] & 0xFF);
            w += mula_small(p, p, i, x, n, zy) +
                    (p[i+n] & 0xFF) + zy * (x[n] & 0xFF);
            p[i+n] = (byte)w;
            w >>= 8;