
/**
 * Copyright 2012 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2014 John L. Jegutanis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.coinomi.core.wallet;

import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.network.AddressStatus;
import com.coinomi.core.protos.Protos;
import com.coinomi.core.wallet.families.bitcoin.BitTransaction;
import com.coinomi.core.wallet.families.bitcoin.BitWalletTransaction;
import com.coinomi.core.wallet.families.bitcoin.EmptyTransactionOutput;
import com.coinomi.core.wallet.families.bitcoin.TrimmedTransaction;
import com.coinomi.core.wallet.families.bitcoin.TrimmedOutput;
import com.coinomi.core.wallet.families.bitcoin.OutPointOutput;
import com.google.protobuf.ByteString;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.store.UnreadableWalletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.core.Preconditions.checkState;
import static org.bitcoinj.params.Networks.Family.CLAMS;
import static org.bitcoinj.params.Networks.Family.NUBITS;
import static org.bitcoinj.params.Networks.Family.PEERCOIN;
import static org.bitcoinj.params.Networks.Family.REDDCOIN;
import static org.bitcoinj.params.Networks.Family.VPNCOIN;
import static org.bitcoinj.params.Networks.isFamily;


/**
 * @author John L. Jegutanis
 */
public class WalletPocketProtobufSerializer {
    private static final Logger log = LoggerFactory.getLogger(WalletPocketProtobufSerializer.class);

    // Used for de-serialization
    protected Map<ByteString, BitTransaction> txMap = new HashMap<>();

    public static Protos.WalletPocket toProtobuf(WalletPocketHD pocket) {

        Protos.WalletPocket.Builder walletBuilder = Protos.WalletPocket.newBuilder();
        walletBuilder.setNetworkIdentifier(pocket.getCoinType().getId());