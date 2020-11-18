package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.PeerFamily;

public class AsiacoinMain extends PeerFamily {
    private AsiacoinMain() {
        id = "asiacoin.main";

        addressHeader = 23;
        p2shHeader = 8;
        acceptableAddressCodes = new