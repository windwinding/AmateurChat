package com.coinomi.core.coins;

import com.coinomi.core.coins.families.NxtFamily;

/**
 * @author John L. Jegutanis
 */
public class NxtMain extends NxtFamily {

    private NxtMain() {
        id = "nxt.main";

        name = "NXT";
        symbol = "NXT";
        uriScheme = "nxt";
        bip44Index = 29;
        unitExponent = 8;
        addressPrefix = "NXT-";
        feeValue = oneCoin();
        