package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;

/**
 * @author FuzzyHobbit
 */
public class DigitalcoinMain extends BitFamily {
    private DigitalcoinMain() {
        id = "digitalcoin.main";

        addressHeader = 30;
        p2shHeader = 5;
        acceptableAddressC