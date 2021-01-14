package com.coinomi.core.coins.nxt;

/*
    Reed Solomon Encoding and Decoding for Nxt

    Version: 1.0, license: Public Domain, coder: NxtChg (admin@nxtchg.com)
    Java Version: ChuckOne (ChuckOne@mail.de).
*/

import java.math.BigInteger;

final class ReedSolomon {

    private static final int[] initial_codeword = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] gexp = {1, 2, 4, 8, 16, 5, 10, 20, 13, 26, 17, 7, 14, 28, 29, 31, 27, 19, 3, 6, 12, 24, 21, 15, 30, 25, 23, 11, 22, 9, 18, 1};
    private s