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
    @