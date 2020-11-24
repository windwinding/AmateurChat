package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;

/**
 * @author John L. Jegutanis
 */
public class DogecoinMain extends BitFamily {
    private DogecoinMain() {
        id = "dogecoin.main";

        addressHeader = 30;
        p2shHeader = 22;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 240; // COINBASE_MATURITY_NEW
        dumpedPrivateKeyHeader = 158;

        name = "Dogecoin";
        symbol = "DOGE";
        uriScheme = "dogecoin";
        bip44Index = 3;
        unitExponent = 8;
        feeValue = value(100000000L);
        minNonDust = value(1);
        softDustLimit = value(100000000L); // 1 DOGE
        softDustPolicy = SoftDustPolicy.BASE_FEE_FOR_EACH_SOFT_DUST_TXO;
        signedMessageHeader = toB