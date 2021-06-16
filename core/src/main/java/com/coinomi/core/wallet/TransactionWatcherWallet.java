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
            }
        } else {
            // When sending keep all outputs
            tx.addAllOutputs(outputs);
        }

        // Replace with trimmed transaction
        removeTransaction(hash);

        simpleAddTransaction(txPool,
                BitTransaction.fromTrimmed(hash, tx, valueSent, valueReceived, fee));
        return true;
    }

    private void removeTransaction(Sha256Hash hash) {
        checkState(lock.isHeldByCurrentThread(), "Lock is held by another thread");
        rawTransactions.remove(hash);
        confirmed.remove(hash);
        pending.remove(hash);
    }

    /**
     * Adds the given transaction to the given pools and registers a confidence change listener on it.
     */
    private void addWalletTransaction(@Nullable WalletTransaction.Pool pool, BitTransaction tx,
                                      boolean save) {
        lock.lock();
        try {
            if (pool == null) {
                switch (tx.getConfidenceType()) {
                    case BUILDING:
                        pool = WalletTransaction.Pool.CONFIRMED;
                        break;
                    case PENDING:
                        pool = WalletTransaction.Pool.PENDING;
                        break;
                    case DEAD:
                    case UNKNOWN:
                    default:
                        throw new RuntimeException("Unsupported confidence type: " +
                                tx.getConfidenceType().name());
                }
            }

            guessSource(tx);
            simpleAddTransaction(pool, tx);
            trimTransaction(tx.getHash());
            if (tx.getSource() == Source.SELF) queueOnNewBalance();
        } finally {
            lock.unlock();
        }


        // This is safe even if the listener has been added before, as TransactionConfidence ignores duplicate
        // registration requests. That makes the code in the wallet simpler.
        // TODO add txConfidenceListener
//        tx.getConfidence().addEventListener(txConfidenceListener, Threading.SAME_THREAD);
        if (save) walletSaveLater();
    }

    private void guessSource(BitTransaction tx) {
        checkState(lock.isHeldByCurrentThread(), "Lock is held by another thread");
        if (tx.getSource() == Source.UNKNOWN) {
            boolean isReceiving = tx.getValue(this).isPositive();

            if (isReceiving) {
                tx.setSource(Source.NETWORK);
            } else {
                tx.setSource(Source.SELF);
            }
        }
    }

    /**
     * Returns a transaction object given its hash, if it exists in this wallet, or null otherwise.
     */
    @Nullable
    public Transaction getRawTransaction(Sha256Hash hash) {
        lock.lock();
        try {
            BitTransaction tx = rawTransactions.get(hash);
            if (tx != null) {
                return tx.getRawTransaction();
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns transactions that match the hashes, some transactions could be missing.
     */
    public HashMap<Sha256Hash, BitTransaction> getTransactions(Set<Sha256Hash> hashes) {
        lock.lock();
        try {
            HashMap<Sha256Hash, BitTransaction> txs = new HashMap<>();
            for (Sha256Hash hash : hashes) {
                if (rawTransactions.containsKey(hash)) {
                    txs.put(hash, rawTransactions.get(hash));
                }
            }
            return txs;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Deletes transactions which appeared above the given block height from the wallet, but does not touch the keys.
     * This is useful if you have some keys and wish to replay the block chain into the wallet in order to pick them up.
     * Triggers auto saving.
     */
    @Override
    public void refresh() {
        lock.lock();
        try {
            log.info("Refreshing wallet pocket {}", type);
            lastBlockSeenHash = null;
            lastBlockSeenHeight = -1;
            lastBlockSeenTimeSecs = 0;
            blockTimes.clear();
            missingTimestamps.clear();
            unspentOutputs.clear();
            confirmed.clear();
            pending.clear();
            rawTransactions.clear();
            addressesStatus.clear();
            clearTransientState();
        } finally {
            lock.unlock();
        }
    }

    /** Returns the hash of the last seen best-chain block, or null if the wallet is too old to store this data. */
    @Nullable
    public Sha256Hash getLastBlockSeenHash() {
        lock.lock();
        try {
            return lastBlockSeenHash;
        } finally {
            lock.unlock();
        }
    }

    public void setLastBlockSeenHash(@Nullable Sha256Hash lastBlockSeenHash) {
        lock.lock();
        try {
            this.lastBlockSeenHash = lastBlockSeenHash;
        } finally {
            lock.unlock();
        }
        walletSaveLater();
    }

    public void setLastBlockSeenHeight(int lastBlockSeenHeight) {
        lock.lock();
        try {
            this.lastBlockSeenHeight = lastBlockSeenHeight;
        } finally {
            lock.unlock();
        }
        walletSaveLater();
    }

    public void setLastBlockSeenTimeSecs(long timeSecs) {
        lock.lock();
        try {
            lastBlockSeenTimeSecs = timeSecs;
        } finally {
            lock.unlock();
        }
        walletSaveLater();
    }

    /**
     * Returns the UNIX time in seconds since the epoch extracted from the last best seen block header. This timestamp
     * is <b>not</b> the local time at which the block was first observed by this application but rather what the block
     * (i.e. miner) self declares. It is allowed to have some significant drift from the real time at which the block
     * was found, although most miners do use accurate times. If this wallet is old and does not have a recorded
     * time then this method returns zero.
     */
    public long getLastBlockSeenTimeSecs() {
        lock.lock();
        try {
            return lastBlockSeenTimeSecs;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a {@link java.util.Date} representing the time extracted from the last best seen block header. This timestamp
     * is <b>not</b> the local time at which the block was first observed by this application but rather what the block
     * (i.e. miner) self declares. It is allowed to have some significant drift from the real time at which the block
     * was found, although most miners do use accurate times. If this wallet is old and does not have a recorded
     * time then this method returns null.
     */
    @Nullable
    public Date getLastBlockSeenTime() {
        final long secs = getLastBlockSeenTimeSecs();
        if (secs == 0)
            return null;
        else
            return new Date(secs * 1000);
    }

    /**
     * Returns the height of the last seen best-chain block. Can be 0 if a wallet is brand new or -1 if the wallet
     * is old and doesn't have that data.
     */
    public int getLastBlockSeenHeight() {
        lock.lock();
        try {
            return lastBlockSeenHeight;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Value getBalance() {
        return getBalance(false);
    }

    public Value getBalance(boolean includeReceiving) {
        lock.lock();
        try {
            long value = 0;
            for (OutPointOutput utxo : getUnspentOutputs(includeReceiving).values()) {
                value = LongMath.checkedAdd(value, utxo.getValueLong());
            }
            return type.value(value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets that the specified status is currently updating i.e. getting transactions.
     *
     * Returns true if registered successfully or false if status already updating
     */
    @VisibleForTesting boolean registerStatusForUpdate(AddressStatus status) {
        checkNotNull(status.getStatus());

        lock.lock();
        try {
            // If current address is updating
            if (statusPendingUpdates.containsKey(status.getAddress())) {
                AddressStatus updatingAddressStatus = statusPendingUpdates.get(status.getAddress());
                String updatingStatus = updatingAddressStatus.getStatus();

                // If the same status is updating, don't update again
                if (updatingStatus != null && updatingStatus.equals(status.getStatus())) {
                    return false;
                } else { // Status is newer, so replace the updating status
                    statusPendingUpdates.put(status.getAddress(), status);
                    return true;
                }
            } else { // This status is new
                statusPendingUpdates.put(status.getAddress(), status);
                return true;
            }
        }
        finally {
            lock.unlock();
        }
    }

    void commitAddressStatus(AddressStatus newStatus) {
        if (!newStatus.canCommitStatus()) {
            log.warn("Tried to commit an address status with a non applied state: {}:{}",
                    newStatus.getAddress(), newStatus.getStatus());
            return;
        }

        lock.lock();
        try {
            AddressStatus updatingStatus = statusPendingUpdates.get(newStatus.getAddress());
            if (updatingStatus != null && updatingStatus.equals(newStatus)) {
                statusPendingUpdates.remove(newStatus.getAddress());
            }
            addressesStatus.put(newStatus.getAddress(), newStatus.getStatus());
            queueOnConnectivity();
        }
        finally {
            lock.unlock();
        }
        // Skip saving null statuses
        if (newStatus.getStatus() != null) {
            walletSaveLater();
        }
    }

    private boolean isAddressStatusChanged(AddressStatus addressStatus) {
        lock.lock();
        try {
            AbstractAddress address = addressStatus.getAddress();
            String newStatus = addressStatus.getStatus();
            if (addressesStatus.containsKey(address)) {
                String previousStatus = addressesStatus.get(address);
                if (previousStatus == null) {
                    return newStatus != null; // Status changed if newStatus is not null
                } else {
                    return !previousStatus.equals(newStatus);
                }
            }
            else {
                // Unused address, just mark it that we watch it
                if (newStatus == null) {
                    commitAddressStatus(addressStatus);
                    return false;
                } else {
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public AddressStatus getAddressStatus(AbstractAddress address) {
        lock.lock();
        try {
            if (addressesStatus.containsKey(address)) {
                return new AddressStatus(address, addressesStatus.get(address));
            }
            else {
                return null;
            }
        }
        finally {
            lock.unlock();
        }
    }

    public List<AddressStatus> getAllAddressStatus() {
        lock.lock();
        try {
            ArrayList<AddressStatus> statuses = new ArrayList<>(addressesStatus.size());
            for (Map.Entry<AbstractAddress, String> status : addressesStatus.entrySet()) {
                statuses.add(new AddressStatus(status.getKey(), status.getValue()));
            }
            return statuses;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Returns all the addresses that are not currently watched
     */
    @VisibleForTesting List<AbstractAddress> getAddressesToWatch() {
        lock.lock();
        try {
            ImmutableList.Builder<AbstractAddress> addressesToWatch = ImmutableList.builder();
            for (AbstractAddress address : getActiveAddresses()) {
                // If address not already subscribed or pending subscription
                if (!addressesSubscribed.contains(address) && !addressesPendingSubscription.contains(address)) {
                    addressesToWatch.add(address);
                }
            }
            return addressesToWatch.build();
        }
        finally {
            lock.unlock();
        }
    }

    private void confirmAddressSubscription(AbstractAddress address) {
        checkState(lock.isHeldByCurrentThread(), "Lock is held by another thread");
        if (addressesPendingSubscription.contains(address)) {
            log.debug("Subscribed to {}", address);
            addressesPendingSubscription.remove(address);
            addressesSubscribed.add(address);
        }
    }

    @Override
    public void onNewBlock(BlockHeader header) {
        log.info("Got a {} block: {}", type.getName(), header.getBlockHeight());
        boolean shouldSave = false;
        lock.lock();
        try {
            lastBlockSeenTimeSecs = header.getTimestamp();
            lastBlockSeenHeight = header.getBlockHeight();
            updateTransactionTimes(header);
            for (BitTransaction tx : rawTransactions.values()) {
                // Save wallet when we have new TXs
                if (tx.getDepthInBlocks() < TX_DEPTH_SAVE_THRESHOLD) shouldSave = true;
                maybeUpdateBlockDepth(tx, true);
            }
            queueOnNewBlock();
        } finally {
            lock.unlock();
        }
        if (shouldSave) walletSaveLater();
    }

    private void maybeUpdateBlockDepth(BitTransaction tx, boolean updateUtxoSet) {
        checkState(lock.isHeldByCurrentThread(), "Lock is held by another thread");
        if (tx.getConfidenceType() != BUILDING) return;
        int newDepth = lastBlockSeenHeight - tx.getAppearedAtChainHeight() + 1;
        if (newDepth > 1) {
            tx.setDepthInBlocks(newDepth);

            // Update unspent outputs
            if (updateUtxoSet) {
                for (TransactionOutput output : tx.getOutputs(false)) {
                    OutPointOutput unspentOutput = unspentOutputs.get(TrimmedOutPoint.get(output));
                    if (unspentOutput != null) {
                        unspentOutput.setDepthInBlocks(newDepth);
                    }
                }
            }
        }
    }

    @Override
    public void onBlockUpdate(BlockHeader header) {
        log.info("Got a {} block update: {}", type.getName(), header.getBlockHeight());
        lock.lock();
        try {
            updateTransactionTimes(header);
            queueOnNewBlock();
        }
        finally {
            lock.unlock();
        }
    }

    private void updateTransactionTimes(BlockHeader header) {
        checkState(lock.isHeldByCurrentThread(), "Lock is held by another thread");
        Integer height = header.getBlockHeight();
        Long timestamp = header.getTimestamp();
        boolean mustSave = false;
        blockTimes.put(height, timestamp);
        if (missingTimestamps.containsKey(height)) {
            for (Sha256Hash hash : missingTimestamps.get(height)) {
                if (rawTransactions.containsKey(hash)) {
                    rawTransactions.get(hash).setTimestamp(timestamp);
                    mustSave = true;
                }
            }
        }
        missingTimestamps.remove(height);
        if (mustSave) {
            walletSaveLater();
        }
    }

    @Override
    public void onAddressStatusUpdate(AddressStatus status) {
        log.debug("Got a status {}", status);
        lock.lock();
        try {
            confirmAddressSubscription(status.getAddress());
            if (status.getStatus() != null) {
                markAddressAsUsed(status.getAddress());
                subscribeToAddressesIfNeeded();

                if (isAddressStatusChanged(status)) {
                    // Status changed, time to update
                    if (registerStatusForUpdate(status)) {
                        log.info("Must get transactions for address {}, status {}",
                                status.getAddress(), status.getStatus());

                        if (blockchainConnection != null) {
                            blockchainConnection.getUnspentTx(status, this);
                            blockchainConnection.getHistoryTx(status, this);
                        }
                    } else {
                        log.info("Status {} already updating", status.getStatus());
                    }
                }
            }
            else {
                // Address not used, just update the status
                commitAddressStatus(status);
                tryToApplyState();
            }
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void onUnspentTransactionUpdate(AddressStatus status, List<UnspentTx> unspentTxes) {
        lock.lock();
        try {
            AddressStatus updatingStatus = statusPendingUpdates.get(status.getAddress());
            // Check if this updating status is valid
            if (updatingStatus != null && updatingStatus.equals(status)) {
                updatingStatus.queueUnspentTransactions(unspentTxes);
                fetchTransactionsIfNeeded(unspentTxes);
                tryToApplyState(updatingStatus);
            }
            else {
                log.info("Ignoring unspent tx call because no entry found or newer entry.");
            }
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public void onTransactionHistory(AddressStatus status, List<HistoryTx> historyTxes) {
        lock.lock();
        try {
            AddressStatus updatingStatus = statusPendingUpdates.get(status.getAddress());
            // Check if this updating status is valid
            if (updatingStatus != null && updatingStatus.equals(status)) {
                updatingStatus.queueHistoryTransactions(historyTxes);
                fetchTransactionsIfNeeded(historyTxes);
                tryToApplyState();
            } else {
                log.info("Ignoring history tx call because no entry found or newer entry.");
            }
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Try to apply all address states
     */
    private void tryToApplyState() {
        lock.lock();
        try {
            for (AddressStatus status : Lists.newArrayList(statusPendingUpdates.values())) {
                tryToApplyState(status);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Try to apply the status state
     */
    private void tryToApplyState(AddressStatus status) {
        lock.lock();
        try {
            if (statusPendingUpdates.containsKey(status.getAddress())) {
                if (status.isUnspentTxQueued() && !status.isUnspentTxStateApplied()) {
                    Set<Sha256Hash> txHashes = status.getUnspentTxHashes();
                    HashMap<Sha256Hash, BitTransaction> txs = getTransactions(txHashes);
                    // We have all the transactions, apply state
                    if (txs.size() == txHashes.size()) {
                        applyUnspentState(status, txs);
                    }
                }
                if (status.isHistoryTxQueued() && !status.isHistoryTxStateApplied()) {
                    Set<Sha256Hash> txHashes = status.getHistoryTxHashes();
                    HashMap<Sha25