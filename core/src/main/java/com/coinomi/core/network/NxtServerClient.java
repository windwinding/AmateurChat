
package com.coinomi.core.network;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.nxt.Convert;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.coins.nxt.NxtException;
import com.coinomi.core.coins.nxt.TransactionImpl;
import com.coinomi.core.network.interfaces.BlockchainConnection;
import com.coinomi.core.network.interfaces.ConnectionEventListener;
import com.coinomi.core.network.interfaces.TransactionEventListener;

import com.coinomi.core.wallet.families.nxt.NxtTransaction;
import com.coinomi.stratumj.ServerAddress;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.FormEncodingBuilder;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.utils.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;


/**
 * @author vbcs
 * @author John L. Jegutanis
 */
public class NxtServerClient implements BlockchainConnection<NxtTransaction> {

    private static final int POLL_INTERVAL_SEC = 30;

    private static final Logger log = LoggerFactory.getLogger(NxtServerClient.class);

    private static final ScheduledThreadPoolExecutor connectionExec;
    static {
        connectionExec = new ScheduledThreadPoolExecutor(1);
        // FIXME, causing a crash in old Androids
//        connectionExec.setRemoveOnCancelPolicy(true);
    }

    private static final Random RANDOM = new Random();

    private static final long MAX_WAIT = 16;
    private final ConnectivityHelper connectivityHelper;

    private CoinType type;
    private long retrySeconds = 0;
    private boolean stopped = false;
    private ServerAddress lastServerAddress;
    private final ImmutableList<ServerAddress> addresses;
    private final HashSet<ServerAddress> failedAddresses;

    private String lastBalance = "";
    private BlockHeader lastBlockHeader = new BlockHeader(type, 0, 0);

    private static final String GET_ACCOUNT = "getAccount";
    private static final String GET_EC_BLOCK = "getECBlock";
    private static final String GET_LAST_BLOCK = "getBlock";
    private static final String GET_REQUEST = "requestType=";
    private static final String GET_TRANSACTION = "getTransaction";
    private static final String GET_TRANSACTION_BYTES = "getTransactionBytes";
    private static final String GET_BLOCKCHAIN_TXS = "getBlockchainTransactions";

    private int ecBlockHeight = 0;
    private String ecBlockId = "";

    private OkHttpClient client;

    // TODO, only one is supported at the moment. Change when accounts are supported.
    private transient CopyOnWriteArrayList<ListenerRegistration<ConnectionEventListener>> eventListeners;
    private ScheduledExecutorService blockchainSubscription;
    private ScheduledExecutorService ecSubscription;
    private ScheduledExecutorService addressSubscription;

    public int getEcBlockHeight() { return ecBlockHeight; }

    public Long getEcBlockId() { return Convert.parseUnsignedLong(ecBlockId); }

    private Runnable reconnectTask = new Runnable() {
        public boolean isPolling = true;
        @Override
        public void run() {
            if (!stopped) {
                if (connectivityHelper.isConnected()) {
                    isPolling = false;
                } else {
                    // Start polling for connection to become available
                    if (!isPolling) log.info("No connectivity, starting polling.");
                    connectionExec.remove(reconnectTask);
                    connectionExec.schedule(reconnectTask, 10, TimeUnit.SECONDS);
                    isPolling = true;
                }
            } else {
                log.info("{} client stopped, aborting reconnect.", type.getName());
                isPolling = false;
            }
        }
    };


    public NxtServerClient(CoinAddress coinAddress, ConnectivityHelper connectivityHelper) {
        this.connectivityHelper = connectivityHelper;
        eventListeners = new CopyOnWriteArrayList<ListenerRegistration<ConnectionEventListener>>();
        failedAddresses = new HashSet<ServerAddress>();
        type = coinAddress.getType();
        addresses = ImmutableList.copyOf(coinAddress.getAddresses());
    }

    private static JSONObject parseReply(Response response) throws IOException, JSONException {
        return new JSONObject(response.body().string());
    }

    private String getBaseUrl() {
        ServerAddress address = getServerAddress();
        StringBuilder builder = new StringBuilder();
        builder.append("http://" + address.getHost()).append(":").append(address.getPort())
                .append("/nxt?");
        return builder.toString();
    }

    private String getAccountInfo(AbstractAddress address) {
        StringBuilder builder = new StringBuilder();
        builder.append(getBaseUrl()).append(GET_REQUEST).append(GET_ACCOUNT)
        .append("&account=").append(address.toString());
        return builder.toString();
    }

    private String getBlockchainStatusUrl() {
        ServerAddress address = getServerAddress();
        StringBuilder builder = new StringBuilder();
        builder.append("http://" + address.getHost()).append(":").append(address.getPort())
                .append("/nxt?").append(GET_REQUEST).append(GET_LAST_BLOCK);
        return builder.toString();
    }

    private ServerAddress getServerAddress() {
        // If we blacklisted all servers, reset and increase back-off time
        if (failedAddresses.size() == addresses.size()) {
            failedAddresses.clear();
            retrySeconds = Math.min(Math.max(1, retrySeconds * 2), MAX_WAIT);
        }

        ServerAddress address;
        // Not the most efficient, but does the job
        while (true) {
            address = addresses.get(RANDOM.nextInt(addresses.size()));
            if (!failedAddresses.contains(address)) break;
        }
        return address;
    }

    private OkHttpClient getHttpClient(){
        if (client == null) {
            client = new OkHttpClient();
        }
        return client;
    }

    private String getEcUrl() {
        return getBaseUrl() + GET_REQUEST + GET_EC_BLOCK;
    }

    private String getBlockChainTxsUrl(String address) {
        StringBuilder builder = new StringBuilder();
        builder.append(getBaseUrl()).append(GET_REQUEST).append(GET_BLOCKCHAIN_TXS)
                .append("&account=").append(address);
        return builder.toString();
    }

    private String getTransactionUrl(String txHash) {
        StringBuilder builder = new StringBuilder();
        builder.append(getBaseUrl()).append(GET_REQUEST).append(GET_TRANSACTION)
                .append("&fullHash=").append(txHash);
        return builder.toString();
    }

    private String getTransactionBytesUrl(String txId) {
        StringBuilder builder = new StringBuilder();
        builder.append(getBaseUrl()).append(GET_REQUEST).append(GET_TRANSACTION_BYTES)
                .append("&transaction=").append(txId);
        return builder.toString();
    }

    @Override
    public void subscribeToBlockchain(final TransactionEventListener listener) {

        log.info("Going to subscribe to block chain headers");
        if (blockchainSubscription != null) {
            blockchainSubscription.shutdownNow();
        }

        blockchainSubscription = Executors.newSingleThreadScheduledExecutor();
        blockchainSubscription.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                Request request = new Request.Builder().url(getBlockchainStatusUrl()).build();
                getHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        log.info("Failed to communicate with server:  " + request.toString());

                    }
                    @Override
                    public void onResponse(Response response) throws IOException {
                        try {
                            if (!response.isSuccessful()) {
                                log.info("Unable to fetch blockchain status.");
                                log.info("[Error code] = " + response.code() );
                            }
                            JSONObject reply = parseReply(response);
                            long timestamp = reply.getLong("timestamp");
                            int height = reply.getInt("height");
                            BlockHeader blockheader = new BlockHeader(type, timestamp, height);

                            if (!lastBlockHeader.equals(blockheader)) {
                                lastBlockHeader = blockheader;
                                listener.onNewBlock(blockheader);
                            }

                        } catch (IOException e) {
                            log.info("IOException: " + e.getMessage());
                        } catch (JSONException e) {
                            log.info("Could not parse JSON: " + e.getMessage());
                        }
                    }
                });

            }
        }, 0, POLL_INTERVAL_SEC, TimeUnit.SECONDS);
        subscribeToEc();
    }

    /*
    Method to keep up to date ecBlockId and ecBlockHeight parameters ( runs every 15seconds )
     */
    private void subscribeToEc() {
        if (ecSubscription != null) {
            ecSubscription.shutdownNow();
        }

        ecSubscription = Executors.newSingleThreadScheduledExecutor();
        ecSubscription.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

                Request request = new Request.Builder().url(getEcUrl()).build();
                getHttpClient().newCall(request).enqueue(new Callback() {
                    @Override