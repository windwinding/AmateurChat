package com.coinomi.core.coins;

import com.coinomi.core.coins.families.PeerFamily;

/**
 * @author dasource
 */
public class ShadowCashMain extends PeerFamily {
    private ShadowCashMain() {
        id = "shadowcash.main";

        addressHeader = 63;
        p2shHeader = 125;
        acceptableAddressCodes = new 