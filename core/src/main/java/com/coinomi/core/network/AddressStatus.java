
package com.coinomi.core.network;

import com.coinomi.core.network.ServerClient.HistoryTx;
import com.coinomi.core.network.ServerClient.UnspentTx;
import com.coinomi.core.wallet.AbstractAddress;
import com.google.common.collect.Sets;

import org.bitcoinj.core.Sha256Hash;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 */
final public class AddressStatus {
    final AbstractAddress address;
    @Nullable final String status;

    HashSet<HistoryTx> historyTransactions;
    HashSet<UnspentTx> unspentTransactions;
    HashSet<Sha256Hash> historyTxHashes = new HashSet<>();
    HashSet<Sha256Hash> unspentTxHashes = new HashSet<>();

    boolean stateMustBeApplied;
    boolean historyTxStateApplied;
    boolean unspentTxStateApplied;

    public AddressStatus(AbstractAddress address, @Nullable String status) {
        this.address = address;
        this.status = status;
    }

    public AbstractAddress getAddress() {
        return address;
    }

    @Nullable public String getStatus() {
        return status;