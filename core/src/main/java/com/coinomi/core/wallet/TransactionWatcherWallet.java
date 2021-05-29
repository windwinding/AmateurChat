package com.coinomi.core.wallet;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exceptions.TransactionBroadcastException;
import com.coinomi.core.network.AddressStatus;
import com.coinomi.core.network.BlockHeader;
import com.coinomi.core.network.ServerClient.HistoryTx;
import com.coinomi.core.network.ServerClient.UnspentTx;
import com.coinomi.core.network.interfaces.BlockchainConnection;
import com.coinomi.core.network.interfaces.TransactionEventListener;
import com.coinomi.core.util.BitAddressUtils;
import com.coinomi.core.wallet.families.bitcoin.BitAddress;
import com.coinomi.core.wallet.families.bitcoin.BitBlockchainConnection;
import com.coinomi.core.wallet.families.bitcoin.BitTransaction;
import com.coinomi.core.wallet.families.bitcoin.BitTransactionEventListener;
import com.coinomi.core.wallet.families.bitcoin.BitWalletTransaction;
import com.coinomi.core.wallet.families.bitcoin.OutPointOutput;
import com.coinomi.core.wallet.families.bitcoin.TrimmedOutPoint;
import com.coinomi.core.wallet.families.bitcoin.TrimmedTransaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.math.LongMath;

import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionBag;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionConfidence.Source;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.ListenerRegistration;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.core.Preconditions.checkState;
import static org.bitcoinj.core.TransactionConfidence.ConfidenceType.BUILDING;
import static org.bitcoinj.core.TransactionConfidence.ConfidenceType.PENDING;
import static org.bitcoinj.core.TransactionConfidence.ConfidenceType.UNKNOWN;

/**
 * @author John L. Jegutanis
 */
abstract public class TransactionWatcherWallet extends AbstractWallet<BitTransaction, BitAddress>
        implements TransactionBag, BitTransactionEventListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionWatcherWallet.class);

    private final static int TX_DEPTH_SAVE_THRESHOLD = 4;

    boolean DISABLE_TX_TRIMMING = false;

    @Nullable private Sha256Hash lastBlockSeenHash;
    private int lastBlockSeenHeight = -1;
    private long lastBlockSeenTimeSecs = 0;

    @VisibleForTesting
    final Map<TrimmedOutPoint, OutPointOutput> unspentOutputs;

    // Holds the status of every address we are watching. When connecting to the server, if we get a
    // different status for a particular address this means that there are new transactions for that
    // address and we have to fetch them. The status String could be null when an address is unused.
    @VisibleForTesting
    final Map<AbstractAddress, String> addressesStatus;

    @VisibleForTesting final transient ArrayList<AbstractAddress> addressesSubscribed;
    @VisibleForTesting final transient ArrayList<AbstractAddress> addressesPendingSubscription;
    @VisibleForTesting final transient Map<AbstractAddress, AddressStatus> statusPendingUpdates;
    @VisibleForTesting final transient Map<Sha256Hash, Integer> fetchingTransactions;
    @VisibleForTesting final transient Map<Integer, Long> blockTimes;
    @VisibleForTesting final transient Map<Integer, Set<Sha256Hash>> missingTimestamps;
    // Transactions that are waiting to be added once transactions that they depend on are added
    final transient Map<Sha256Hash, Map.Entry<BitTransaction, Set<Sha256Hash>>> outOfOrderTransactions;

    // The various pools below give quick access to wallet-relevant transactions by the state they're in:
    //
    // Pending:  Transactions that didn't make it into the best chain yet.
    // Confirmed:Transactions that appeared in the best chain.

    @VisibleForTesting final Map<Sha256Hash, BitTransaction> pending;
    @VisibleForTesting final Map<Sha256Hash, BitTransaction> confirmed;

    // All transactions together.
    final Map<Sha256Hash, BitTransaction> rawTransactions;
    private BitBlockchainConnection blockchainConnection;
    private List<ListenerRegistration<WalletAccountEventListener>> listeners;

    // Wallet that this account belongs
    @Nullable private transient Wallet wallet = null;

    @VisibleForTesting transient Value lastBalance;
    transient WalletConnectivityStatus lastConnectivity = WalletConnectivityStatus.DISCONNECTED;

    private Runnable saveLaterRunnable = new Runnable() {
        @Override
        public void run() {
            if (wallet != null) wallet.saveLater();
        }
    };

    private Runnable saveNowRunnable = new Runnable() {
        @Override
        public void run() {
            if (wallet != null) wallet.saveNow();
        }
    };

    // Constructor
    public TransactionWatcherWallet(CoinType coinType, String id) {
        super(coinType, id);
        unspentOutputs = new HashMap<>();
        addressesStatus = new HashMap<>();
        addressesSubscribed = new ArrayList<>();
        addressesPendingSubscription = new ArrayList<>();
        statusPendingUpdates = new HashMap<>();
        fetchingTransactions = new HashMap<>();
        blockTimes = new HashMap<>();
        missingTimestamps = new HashMap<>();
        confirmed = new HashMap<>();
        pending = new HashMap<>();
        rawTransactions = new HashMap<>();
        outOfOrderTransactions = new HashMap<>();
        listeners = new CopyOnWriteArrayList<>();
        lastBalance = type.value(0);
    }


    @Override
    public CoinType getCoinType() {
        return type;
    }

    @Override
    public boolean isNew() {
        return rawTransactions.size() == 0;
    }

    @Override
    public void setWallet(@Nullable Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    @Nullable
    public Wallet getWallet() {
        return wallet;
    }

    // Util
    @Override
    public void walletSaveLater() {
        // Save in another thread to avoid cyclic locking of Wallet and WalletPocket
        Threading.USER_THREAD.execute(saveLaterRunnable);
    }

    @Override
    public void walletSaveNow() {
        // Save in another thread to avoid cyclic locking of Wallet and WalletPocket
        Threading.USER_THREAD.execute(saveNowRunnable);
    }

    /**
     * Returns a set of all WalletTransactions in the wallet.
     */
    public Iterable<BitWalletTransaction> getWalletTransactions() {
        lock.lock();
        try {
            Set<BitWalletTransaction> all = new HashSet<>();
            addWalletTransactionsToSet(all, WalletTransaction.Pool.CONFIRMED, confirmed.values());
            addWalletTransactionsToSet(all, WalletTransaction.Pool.PENDING, pending.values());
            return all;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Just adds the transaction to a pool without doing anything else
     */
    private void simpleAddTransaction(WalletTransaction.Pool pool, BitTransaction tx) {
        lock.lock();
        try {
            if (rawTransactions.containsKey(tx.getHash())) return;
            rawTransactions.put(tx.getHash(), tx);
            switch (pool) {
                case CONFIRMED:
                    checkState(confirmed.put(tx.getHash(), tx) == null);
                    break;
                case PENDING:
                    checkState(pending.put(tx.getHash(), tx) == null);
                    break;
                default:
                    throw new RuntimeException("Unknown wallet transaction type " + pool);
            }
        } finally {
            lock.unlock();
        }
    }

    private static void addWalletTransactionsToSet(Set<BitWalletTransaction> txs,
                                                   WalletTransaction.Pool poolType, Collection<BitTransaction> pool) {
        for (BitTransaction tx : pool) {
            txs.add(new BitWalletTransaction(poolType, tx));
        }
    }

    /**
     * Adds a transaction that has been associated with a particular wallet pool. This is intended for usage by
     * deserialization code, such as the {@link WalletPocketProtobufSerializer} class. It isn't normally useful for
     * applications. It does not trigger auto saving.
     */
    public void addWalletTransaction(BitWalletTransaction wtx) {
        lock.lock();
        try {
            addWalletTransaction(wtx.getPool(), wtx.getTransaction(), true);
        } finally {
            lock.unlock();
        }
    }

    boolean trimTransactionIfNeeded(Sha256Hash hash) {
        lock.lock();
        try {
            return trimTransaction(hash);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove irrelevant inputs and outputs. Returns true if transaction trimmed.
     */
    private boolean trimTransaction(Sha256Hash hash) {
        if (DISABLE_TX_TRIMMING) return false;

        checkState(lock.isHeldByCurrentThread(), "Lock is held by another thread");

        BitTransaction transaction = rawTransactions.get(hash);

        if (transaction == null || transaction.isTrimmed()) return false;

        for (TransactionInput input : transaction.getInputs()) {
            // If this transaction depends on a previous transaction that is yet fetched
            if (isInputMine(input) && !rawTransactions.containsKey(input.getOutpoint().getHash())) {
                log.warn("Tried to trim transaction with unmet dependencies. Tx {} depends on {}.",
                        hash, input.getOutpoint().getHash());
                return false;
            }
        }

        final Value valueSent = transaction.getValueSent(this);
        final Value valueReceived = transaction.getValueReceived(this);
        boolean isReceiving = valueReceived.compareTo(valueSent) > 0;

        // Remove fee when receiving
        final Value fee = isReceiving ? null : transaction.getRawTxFee(this);

        WalletTransaction.Pool txPool;
        if (confirmed.containsKey(hash)) {
            txPool = WalletTransaction.Pool.CONFIRMED;
        } else if (pending.containsKey(hash)) {
            txPool = WalletTransaction.Pool.PENDING;
        } else {
            throw new RuntimeException("Transaction is not found in any pool");
        }

        // Do not trim pending sending transactions as we need their inputs to correctly calculate
        // the UTXO set
        if (txPool == WalletTransaction.Pool.PENDING && !isReceiving) {
            return false;
        }

        Transaction txFull = transaction.getRawTransaction();
        List<TransactionOutput> outputs = txFull.getOutputs();

        TrimmedTransaction tx = new TrimmedTransaction(type, hash, outputs.size());

        // Copy confidence
        TransactionConfidence fullTxConf = txFull.getConfidence();
        TransactionConfidence txConf = tx.getConfidence();
        txConf.setSource(fullTxConf.getSource());
        txConf.setConfidenceType(fullTxConf.getConfidenceType());
        if (txConf.getConfidenceType() == BUILDING) {
            txConf.setAppearedAtChainHeight(fullTxConf.getAppearedAtChainHeight());
            txConf.setDepthInBlocks(fullTxConf.getDepthInBlocks());
        }
        // Copy other fields
        tx.setTime(txFull.getTime());
        tx.setTokenId(txFull.getTokenId());
        tx.setExtraBytes(txFull.getExtraBytes());
        tx.setUpdateTime(txFull.getUpdateTime());
        tx.setLockTime(txFull.getLockTime());

        if (txFull.getAppearsInHashes() != null) {
            for (Map.Entry<Sha256Hash, Integer> appears : txFull.getAppearsInHashes().entrySet()) {
                tx.addBlockAppearance(appears.getKey(), appears.getValue());
            }
        }

        tx.setPurpose(txFull.getPurpose());

        // Remove unrelated outputs when receiving coins
        if (isReceiving) {
            int outputIndex = 0;
            for (TransactionOutput output : outputs) {
                if (output.isMineOrWatched(this)) {
                    tx.addOutput(outputIndex, output);
                }
                outputIndex++;
