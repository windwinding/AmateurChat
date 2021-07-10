package com.coinomi.core.wallet.families.nxt;

import com.coinomi.core.coins.nxt.Crypto;
import com.coinomi.core.protos.Protos;
import com.coinomi.core.util.KeyUtils;
import com.google.protobuf.ByteString;

import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.wallet.EncryptableKeyChain;
import org.bitcoinj.wallet.KeyBag;
import org.bitcoinj.wallet.KeyChainEventListener;
import org.bitcoinj.wallet.RedeemData;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * @author John L. Jegutanis
 */
final public class NxtFamilyKey implements EncryptableKeyChain, KeyBag, Serializable {
    private final DeterministicKey entropy;
    private final byte[] publicKey;

    public NxtFamilyKey(DeterministicKey entropy, @Nullable KeyCrypter keyCrypter,
                        @Nullable KeyParameter key) {
        checkArgument(!entropy.isEncrypted(), "Entropy must not be encrypted");
        this.publicKey = Crypto.getPublicKey(entropy.getPrivKeyBytes());
        // Encrypt entropy if needed
        if (keyCrypter != null && key != null) {
            this.entropy = entropy.encrypt(keyCrypter, key, entropy.getParent());
        } else {
            this.entropy = entropy;
        }
    }

    private NxtFamilyKey(DeterministicKey entropy, byte[] publicKey) {
        this.entropy = entropy;
        this.publicKey = publicKey;

    }

    public boolean isEncrypted() {
        return entropy.isEncrypted();
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getPrivateKey() {
        return Crypto.convertToPrivateKey(entropy.getPrivKeyBytes());
    }

    @Nullable
    @Override
    public ECKey findKeyFromPubHash(byte[] pubkeyHash) {
        throw new RuntimeException("Not implemented");
    }

    @Nullable
    @Override
    public ECKey findKeyFromPubKey(byte[] pubkey) {
        throw new RuntimeException("Not implemented");
    }

    @Nullable
    @Override
    public RedeemData findRedeemDataFromScriptHash(byte[] scriptHash) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasKey(ECKey key) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<? extends ECKey> getKeys(