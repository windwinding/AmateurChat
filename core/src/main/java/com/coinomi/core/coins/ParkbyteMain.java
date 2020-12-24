package com.coinomi.core.coins;

import com.coinomi.core.coins.families.PeerFamily;

/**
 * @author John L. Jegutanis
 */
public class ParkbyteMain extends PeerFamily {
    private ParkbyteMain() {
        id = "parkbyte.main";

        addressHeader = 55;
        p2shHeader = 28;
        acceptableAddressCodes = new in