
package com.coinomi.core.wallet;

import com.coinomi.core.CoreUtils;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.exceptions.UnsupportedCoinTypeException;
import com.coinomi.core.protos.Protos;
import com.coinomi.core.wallet.families.nxt.NxtFamilyWallet;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import static com.coinomi.core.CoreUtils.bytesToMnemonic;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author John L. Jegutanis
 */
final public class Wallet {
    private static final Logger log = LoggerFactory.getLogger(Wallet.class);
    public static int ENTROPY_SIZE_DEBUG = -1;

    private final ReentrantLock lock = Threading.lock("KeyChain");

    @GuardedBy("lock") private final LinkedHashMap<CoinType, ArrayList<WalletAccount>> accountsByType;
    @GuardedBy("lock") private final LinkedHashMap<String, WalletAccount> accounts;

    @Nullable private DeterministicSeed seed;
    private DeterministicKey masterKey;

    protected volatile WalletFiles vFileManager;

    // FIXME, make multi account capable
    private final static int ACCOUNT_ZERO = 0;

    private int version = 2;

    public Wallet(String mnemonic) throws MnemonicException {
        this(CoreUtils.parseMnemonic(mnemonic), null);
    }

    public Wallet(List<String> mnemonic) throws MnemonicException {
        this(mnemonic, null);
    }

    public Wallet(List<String> mnemonic, @Nullable String password) throws MnemonicException {
        MnemonicCode.INSTANCE.check(mnemonic);
        password = password == null ? "" : password;

        seed = new DeterministicSeed(mnemonic, null, password, 0);
        masterKey = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
        accountsByType = new LinkedHashMap<CoinType, ArrayList<WalletAccount>>();
        accounts = new LinkedHashMap<String, WalletAccount>();
    }

    public Wallet(DeterministicKey masterKey, @Nullable DeterministicSeed seed) {
        this.seed = seed;
        this.masterKey = masterKey;
        accountsByType = new LinkedHashMap<CoinType, ArrayList<WalletAccount>>();
        accounts = new LinkedHashMap<String, WalletAccount>();
    }

    public static List<String> generateMnemonic(int entropyBitsSize) {
        byte[] entropy;
        if (ENTROPY_SIZE_DEBUG > 0) {
            entropy = new byte[ENTROPY_SIZE_DEBUG];
        } else {
            entropy = new byte[entropyBitsSize / 8];
        }

        SecureRandom sr = new SecureRandom();
        sr.nextBytes(entropy);

        return bytesToMnemonic(entropy);
    }

    public static String generateMnemonicString(int entropyBitsSize) {
        List<String> mnemonicWords = Wallet.generateMnemonic(entropyBitsSize);
        return mnemonicToString(mnemonicWords);
    }

    public static String mnemonicToString(List<String> mnemonicWords) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mnemonicWords.size(); i++) {
            if (i != 0) sb.append(' ');
            sb.append(mnemonicWords.get(i));
        }
        return sb.toString();
    }

    static String generateRandomId() {
        byte[] randomIdBytes = new byte[32];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(randomIdBytes);
        return Hex.toHexString(randomIdBytes);
    }

    public WalletAccount createAccount(CoinType coin, @Nullable KeyParameter key) {
        return createAccount(coin, false, key);
    }

    public WalletAccount createAccount(CoinType coin, boolean generateAllKeys,
                                  @Nullable KeyParameter key) {
        return createAccounts(Lists.newArrayList(coin), generateAllKeys, key).get(0);
    }

    public List<WalletAccount> createAccounts(List<CoinType> coins, boolean generateAllKeys,
                                  @Nullable KeyParameter key) {
        lock.lock();
        try {
            ImmutableList.Builder<WalletAccount> newAccounts = ImmutableList.builder();
            for (CoinType coin : coins) {
                log.info("Creating coin pocket for {}", coin);
                WalletAccount newAccount = createAndAddAccount(coin, key);
                if (generateAllKeys) {
                    newAccount.maybeInitializeAllKeys();
                }
                newAccounts.add(newAccount);
            }
            return newAccounts.build();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if at least one account exists for a specific coin
     */
    public boolean isAccountExists(CoinType coinType) {
        lock.lock();
        try {
            return accountsByType.containsKey(coinType);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if account exists
     */
    public boolean isAccountExists(@Nullable String accountId) {
        if (accountId == null) return false;
        lock.lock();
        try {
            return accounts.containsKey(accountId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get a specific account, null if does not exist
     */
    @Nullable
    public WalletAccount getAccount(@Nullable String accountId) {
        if (accountId == null) return null;
        lock.lock();
        try {
            return accounts.get(accountId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get accounts for a specific coin type. Returns empty list if no account exists
     */
    public List<WalletAccount> getAccounts(CoinType coinType) {
        return getAccounts(Lists.newArrayList(coinType));
    }

    /**
     * Get accounts for a specific coin type. Returns empty list if no account exists
     */
    public List<WalletAccount> getAccounts(List<CoinType> types) {
        lock.lock();
        try {
            ImmutableList.Builder<WalletAccount> builder = ImmutableList.builder();
            for (CoinType type : types) {
                if (isAccountExists(type)) {
                    builder.addAll(accountsByType.get(type));
                }
            }
            return builder.build();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get accounts that watch a specific address. Returns empty list if no account exists
     */
    public List<WalletAccount> getAccounts(final AbstractAddress address) {
        lock.lock();
        try {
            ImmutableList.Builder<WalletAccount> builder = ImmutableList.builder();
            CoinType type = address.getType();
            if (isAccountExists(type)) {
                for (WalletAccount account : accountsByType.get(type)) {
                    if (account.isAddressMine(address)) {
                        builder.add(account);
                    }
                }
            }
            return builder.build();
        } finally {
            lock.unlock();
        }
    }

    public List<WalletAccount> getAllAccounts() {
        lock.lock();
        try {
            return ImmutableList.copyOf(accounts.values());
        } finally {
            lock.unlock();
        }
    }


    public List getAccountIds() {
        lock.lock();
        try {
            return ImmutableList.copyOf(accounts.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Generate and add a new BIP44 account for a specific coin type
     */
    private WalletAccount createAndAddAccount(CoinType coinType, @Nullable KeyParameter key) {
        checkState(lock.isHeldByCurrentThread(), "Lock is held by another thread");
        checkNotNull(coinType, "Attempting to create a pocket for a null coin");

        // TODO, currently we support a single account so return the existing account
        List<WalletAccount> currentAccount = getAccounts(coinType);
        if (currentAccount.size() > 0) {
            return currentAccount.get(0);
        }
        // TODO ///////////////

        DeterministicHierarchy hierarchy;
        if (isEncrypted()) {
            hierarchy = new DeterministicHierarchy(masterKey.decrypt(getKeyCrypter(), key));
        } else {
            hierarchy= new DeterministicHierarchy(masterKey);
        }
        int newIndex = getLastAccountIndex(coinType) + 1;
        DeterministicKey rootKey = hierarchy.get(coinType.getBip44Path(newIndex), false, true);

        WalletAccount newPocket;

        if (coinType instanceof BitFamily) {
            newPocket = new WalletPocketHD(rootKey, coinType, getKeyCrypter(), key);
        } else if (coinType instanceof NxtFamily) {
            newPocket = new NxtFamilyWallet(rootKey, coinType, getKeyCrypter(), key);
        } else {
            throw new UnsupportedCoinTypeException(coinType);
        }

        if (isEncrypted() && !newPocket.isEncrypted()) {
            newPocket.encrypt(getKeyCrypter(), key);
        }
        addAccount(newPocket);
        return newPocket;
    }

    /**
     * Get the last BIP44 account index of an account in this wallet. If no accounts found return -1
     */
    private int getLastAccountIndex(CoinType coinType) {
        if (!isAccountExists(coinType)) {
            return -1;
        }
        int lastIndex = -1;
        for (WalletAccount account : accountsByType.get(coinType)) {
            if (account instanceof WalletPocketHD) {
                int index = ((WalletPocketHD) account).getAccountIndex();
                if (index > lastIndex) {
                    lastIndex = index;
                }
            }
        }
        return lastIndex;
    }

    public void addAccount(WalletAccount pocket) {
        lock.lock();
        try {
            String id = pocket.getId();
            CoinType type = pocket.getCoinType();

            checkState(!accounts.containsKey(id), "Cannot replace an existing wallet pocket");

            if (!accountsByType.containsKey(type)) {
                accountsByType.put(type, new ArrayList<WalletAccount>());
            }
            accountsByType.get(type).add(pocket);
            accounts.put(pocket.getId(), pocket);
            pocket.setWallet(this);
        } finally {
            lock.unlock();
        }
    }

    public WalletAccount deleteAccount(String id) {
        lock.lock();
        try {
            if (!accounts.containsKey(id)) {
                return null;
            }