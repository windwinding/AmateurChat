package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;

public class EguldenMain extends BitFamily {
    private EguldenMain() {
        id = "egulden.main";

        addressHeader = 48;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 100;