package com.coinomi.core.wallet.families.bitcoin;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
public class TrimmedOutput extends TransactionOutput {
    final Sha256Hash txHash; // Is null if att