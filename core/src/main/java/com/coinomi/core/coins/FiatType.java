package com.coinomi.core.coins;

import com.coinomi.core.coins.families.Families;
import com.coinomi.core.util.Currencies;
import com.coinomi.core.util.MonetaryFormat;

import org.bitcoinj.core.Coin;

import java.math.BigInteger;
import java.util.HashMap;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 */
public class FiatType implements ValueType {
    public static final int SMALLEST_UNIT_EXPONENT = 8;

    public static final MonetaryFormat PLAIN_FORMAT = new MonetaryFormat().noCode()
            .minDecimals(0).repeatOptionalDecimals(1, SMALLEST_UNIT_EXPONENT);

    public static final MonetaryFormat FRIENDLY_FORMAT = new MonetaryFormat().noCode()
            .minDecimals(2).optionalDecimals(2, 2, 2).postfixCode();

    private static final HashMap<String, FiatType> types = new HashMap<>();

    private final String name;
    private final String currencyCode;
    private transient Value oneCoin;
    private transient MonetaryFormat friendlyFormat;

    public FiatType(final String currencyCode, @Nullable final String name) {
        this.name = name != null ? name : "";
        this.currencyCode = currencyCode;
    }

    public static FiatType get(String currencyCode) {
        if (!types.containsKey(currencyCode)) {
            types.put(currencyCode, new FiatType(currencyCode, Currencies.CURRENCY_NAMES.get(currencyCode)));
        }
        return types.get(curr