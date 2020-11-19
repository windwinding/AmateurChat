package com.coinomi.core.coins;

import com.coinomi.core.coins.families.PeerFamily;

/**
 * @author John L. Jegutanis
 */
public class BlackcoinMain extends PeerFamily {
    private BlackcoinMain() {
        id = "blackcoin.main";

        addressHeader = 25;
        p2shHeader = 85;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 500;
        dumpedPrivateKeyHeader = 153;

        name = "Blackcoin";
        symbol = "BLK";