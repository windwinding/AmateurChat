package com.coinomi.core.wallet.families.nxt;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.nxt.Convert;
import com.coinomi.core.coins.nxt.NxtException;
import com.coinomi.core.coins.nxt.Transaction;
import com.coinomi.core.exceptions.TransactionBroadcastException;
import com.coinomi.core.network.AddressStatus;
import com.coinomi.core.network.BlockHeader;
import com.coinomi.core.network.NxtServerClient;
import com.coinomi.core.network.ServerClient;
import com.coinomi.core.network.interfaces.BlockchainConnection;
import com.coinomi.core.network.interfaces.TransactionEventListener;
import com.coinomi.core.protos.Protos;
import com.coinomi.core.util.KeyUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.SignedMessage;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletAccountEventListener;
import com.coinomi.core.wallet.WalletConnectivityStatus;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.utils.ListenerRegistration;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.RedeemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.core.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author John L. Jegutanis
 */
public class NxtFamilyWallet extends AbstractWallet<NxtTransaction, NxtAddress>
        implements TransactionEventListener<NxtTransaction> {
    private static final Logger log = LoggerFactory.getLogger(NxtFamilyWallet.class);
    protected final Map<Sha256Hash, NxtTransaction> rawtransactions;
    @VisibleForTesting
    final HashMap<AbstractAddress, String> addressesStatus;
    @VisibleForTesting final transient ArrayList<AbstractAddress> addressesSubscribed;
    @VisibleForTesting final transient ArrayList<AbstractAddress> addressesPendingSubscription;
    @VisibleForTesting final transient HashMap<AbstractAddress, AddressStatus> statusPendingUpdates;
    //@VisibleForTesting final transient HashSet<Sha256Hash> fetchingTransactions;
    private final NxtAddress address;
    NxtFamilyKey rootKey;
    private Value balance;
    private int lastEcBlockHeight;
    private long lastEcBlockId;
    // Wallet that this account belongs
    @Nullable private transient Wallet wallet = null;
    private NxtServerClient blockchainConnection;
    @Nullable private Sha256Hash lastBlockSeenHash;
    private int lastBlockSeenHeight = -1;
    private long lastBlockSeenTimeSecs = 0;
    private List<ListenerRegistration<WalletAccountEventListener>> listeners;


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

    public NxtFamilyWallet(DeterministicKey entropy, CoinType type) {
        this(entropy, type, null, null);
    }

    public NxtFamilyWallet(DeterministicKey entropy, CoinType type,
                           @Nullable KeyCrypter keyCrypter, @Nullable KeyParameter key) {
        this(new NxtFamilyKey(entropy, keyCrypter, key), type);
    }

    public NxtFamilyWallet(NxtFamilyKey key, CoinType type) {
        this(KeyUtils.getPublicKeyId(type, key.getPublicKey()), key, type);
    }

    public NxtFamilyWallet(String id, NxtFamilyKey key, CoinType type) {
        super(type, id);
        rootKey = key;
        address = new NxtAddress(type, key.getPublicKey());
        balance = type.value(0);
        addressesStatus = new HashMap<>();
        addressesSubscribed = new ArrayList<>();
        addressesPendingSubscription = new ArrayList<>();
        statusPendingUpdates = new HashMap<>();
        //fetchingTransactions = new HashSet<>();
        rawtransactions = new HashMap<>();
        listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public byte[] getPublicKey() {
        return rootKey.getPublicKey();
    }

    @Override
    public String getPublicKeyMnemonic() {
        return address.getRsAccount();
    }

    @Override
    public SendRequest getEmptyWalletRequest(AbstractAddress destination) throws WalletAccountException {
        checkAddress(destination);
        return NxtSendRequest.emptyWallet(this, (NxtAddress) destination);
    }

    @Override
    public SendRequest getSendToRequest(AbstractAddress destination, Value amount) throws WalletAccountException {
        checkAddress(destination);
        return NxtSendRequest.to(this, (NxtAddress) destination, amount);
    }

    private void checkAddress(AbstractAddress destination) throws WalletAccountException {
        if (!(destination instanceof NxtAddress)) {
            throw new WalletAccountException("Incompatible address" +
                    destination.getClass().getName() + ", expected " + NxtAddress.class.getName());
        }
    }


    @Override
    public void completeTransaction(SendRequest request) throws WalletAccountException {
        checkSendRequest(request);
        completeTransaction((NxtSendRequest) request);
    }

    @Override
    public void signTransaction(SendRequest request) throws WalletAccountException {
        checkSendRequest(request);
        signTransaction((NxtSendRequest) request);
    }

    private void checkSendRequest(SendRequest request) throws WalletAccountException {
        if (!(request instanceof NxtSendRequest)) {
            throw new WalletAccountException("Incompatible request " +
                    request.getClass().getName() + ", expected " + NxtSendRequest.class.getName());
        }
    }

    public void completeTransaction(NxtSendRequest request) throws WalletAccountException {
        checkArgument(!request.isCompleted(), "Given SendRequest has already been completed.");

        if (request.type.getTransactionVersion() > 0) {
            request.nxtTxBuilder.ecBlockHeight(lastEcBlockHeight);
            request.nxtTxBuilder.ecBlockId(lastEcBlockId);
        }

        // TODO check if the destination public key was announced and if so, remove it from the tx:
        // request.nxtTxBuilder.publicKeyAnnouncement(null);

        try {
            request.tx = new NxtTransaction(type, request.nxtTxBuilder.build());
            request.setCompleted(true);
        } catch (NxtException.NotValidException e) {
            throw new WalletAccount.WalletAccountException(e);
        }

        if (request.signTransaction) {
            signTransaction(request);
        }
    }

    public void signTransaction(NxtSendRequest request) {
        checkArgument(request.isCompleted(), "Send request is not completed");
        checkArgument(request.tx != null, "No transaction found in send request");
        Transaction tx = request.tx.getRawTransaction();
        byte[] privateKey;
        if (rootKey.isEncrypted()) {
            checkArgument(request.aesKey != null, "Wallet is encrypted but no decryption key provided");
            privateKey = rootKey.toDecrypted(request.aesKey).getPrivateKey();
        } else {
            privateKey = rootKey.getPrivateKey();
        }
        tx.sign(privateKey);
        Arrays.fill(privateKey, (byte) 0); // clear private key
    }

    @Override
    public void signMessage(SignedMessage unsignedMessage, @Nullable KeyParameter aesKey) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void verifyMessage(SignedMessage signedMessage) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getPublicKeySerialized() {
        return Convert.toHexString(getPublicKey());
    }

    @Override
    public boolean isNew() {
        // TODO implement, how can we check if this account is new?
        return true;
    }

    @Override
    public Value getBalance() {
        return balance;
    }

    @Override
    public void refresh() {
        lock.lock();
        try {
            log.info("Refreshing wallet pocket {}", type);
            lastBlockSeenHash = null;
            lastBlockSeenHeight = -1;
            lastBlockSeenTimeSecs = 0;
            lastEcBlockHeight = 0;
            lastEcBlockId = 0;
            rawtransactions.clear();
            addressesStatus.clear();
            clearTransientState();
        } finally {
            lock.unlock();
        }
    }

    private void clearTransientState() {
        addressesSubscribed.clear();
        addressesPendingSubscription.clear();
        statusPendingUpdates.clear();
        //fetchingTransactions.clear();
    }

    @Override
    public boolean isConnected() {
        return blockchainConnection != null;
    }

    @Override
    public boolean isLoading() {
//        TODO implement
        return false;
    }

    @Override
    public void disconnect() {
        if (blockchainConnection != null) {
            blockchainConnection.stopAsync();
        }
    }

    @Override
    public AbstractAddress getChangeAddress() {
        return address;
    }

    @Override
    public AbstractAddress getReceiveAddress() {
        return address;
    }

    @Override
    public NxtAddress getReceiveAddress(boolean isManualAddressManagement) {
        return this.address;
    }


    @Override
    public boolean hasUsedAddresses() {
        return false;
    }

    @Override
    public boolean canCreateNewAddre