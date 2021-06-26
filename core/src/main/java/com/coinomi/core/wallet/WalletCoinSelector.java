package com.coinomi.core.wallet;

import com.coinomi.core.wallet.families.bitcoin.CoinSelection;
import com.coinomi.core.wallet.families.bitcoin.CoinSelector;
import com.coinomi.core.wallet.families.bitcoin.OutPointOutput;
import com.google.common.annotations.VisibleForTesting;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * @author John L. Jegutanis
 */



/**
 * This class implements a {@link CoinSelector} which attempts to get the highest priority
 * possible. This means that the transaction is the most likely to get confirmed. Note that this means we may end up
 * "spending" more priority than would be required to get the transaction we are creating confirmed.
 */
public class WalletCoinSelector implements CoinSelector {

    public CoinSelection select(Coin biTarget, List<OutPointOutput> candidates) {
        long target = biTarget.value;
        HashSet<OutPointOutput> selected = new HashSet<>();
        // Sort the inputs by age*value so we get the highest "coindays" spent.
        // TODO: Consider changing the wallets internal format to track just outputs and keep them ordered.
        ArrayList<OutPointOutput> sortedOutputs = new ArrayList<>(candidates);
        // When calculating the wallet balance, we may be asked to select all possible coins, if so, avoid sorting
        // them in order to improve performance.
        if (!biTarget.equals(NetworkParameters.MAX_MONEY)) {
            sortOutputs(sortedOutputs);
        }
        // Now iterate over the sorted outputs until we have got as close to the target as possible or a little
        // bit over (excessive value will b