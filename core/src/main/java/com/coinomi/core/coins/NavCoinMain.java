
package com.coinomi.core.coins;

import com.coinomi.core.coins.families.PeerFamily;

/**
 * @author John L. Jegutanis
 */
public class NavCoinMain extends PeerFamily {
    private NavCoinMain() {
        id = "NavCoin.main";

        addressHeader = 53;
        p2shHeader = 85;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 5;
        dumpedPrivateKeyHeader = 150;

        name = "NavCoin";
        symbol = "NAV";
        uriScheme = "navcoin";
        bip44Index = 130;
        unitExponent = 8;
        feeValue = value(10000); // 0.00001NAV
        minNonDust = value(10000); // 0.01NAV
        softDustLimit = minNonDust;
        softDustPolicy = SoftDustPolicy.NO_POLICY;
        signedMessageHeader = toBytes("NavCoin Signed Message:\n");
    }

    private static NavCoinMain instance = new NavCoinMain();
    public static synchronized NavCoinMain get() {
        return instance;
    }
}