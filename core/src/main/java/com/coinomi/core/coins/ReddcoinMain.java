package com.coinomi.core.coins;

import com.coinomi.core.coins.families.ReddFamily;

/**
 * @author John L. Jegutanis
 */
public class ReddcoinMain extends ReddFamily {
    private ReddcoinMain() {
        id = "reddcoin.main";

        addressHeader = 61;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2