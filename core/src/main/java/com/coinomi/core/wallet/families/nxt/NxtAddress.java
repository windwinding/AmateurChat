
package com.coinomi.core.wallet.families.nxt;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.nxt.Account;
import com.coinomi.core.coins.nxt.Convert;
import com.coinomi.core.coins.nxt.Crypto;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.exceptions.AddressMalformedException;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 */
final public class NxtAddress implements AbstractAddress {
    private final CoinType type;
    private final byte[] publicKey;
    private final long accountId;
    private final String rsAccount;
