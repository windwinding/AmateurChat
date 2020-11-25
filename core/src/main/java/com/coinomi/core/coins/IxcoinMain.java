package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;

/**
 * @author Ahmed Bodiwala
 */
public class IxcoinMain extends BitFamily {
    private IxcoinMain() {
        id = "ixcoin.main";

        addressHeader = 138;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 120; // COINBASE_MATURITY_NEW

        name = "IXCoin";
        symbol = "IXC";
        uriScheme = "ixcoin";
        bip44Index = 86;
        unitExponent = 8;
        feeValue = value(10000);
        minNonDust = value(1);
        s