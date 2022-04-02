package com.coinomi.wallet.ui;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exceptions.NoSuchPocketException;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftAmountTx;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftNormalTx;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftTime;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.ExchangeHistoryProvider;
import com.coinomi.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.widget.SendOutput;
import com.coinomi.wallet.ui.widget.TransactionAmountVisualizer;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.WeakHandler;

import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import javax.annotation.Nullable;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.core.Preconditions.checkState;
import static com.coinomi.wallet.Constants.ARG_ACCOUNT_ID;
import static com.coinomi.wallet.Constants.ARG_EMPTY_WALLET;
import static com.coinomi.wallet.Constants.ARG_SEND_REQUEST;
import static com.coinomi.wallet.Constants.ARG_SEND_TO_ACCOUNT_ID;
import static com.coinomi.wallet.Constants.ARG_SEND_TO_ADDRESS;
import static com.coinomi.wallet.Constants.ARG_SEND_VALUE;
import static com.coinomi.wallet.Constants.ARG_TX_MESSAGE;
import static com.coinomi.wallet.ExchangeRatesProvider.getRates;

/**
 * This fragment displays a busy message and makes the transaction in the background
 *
 */
public class MakeTransactionFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(MakeTransactionFragment.class);

    private static final int START_TRADE_TIMEOUT = 0;
    private static final int UPDATE_TRADE_TIMEOUT = 1;
    private static final int TRADE_EXPIRED = 2;
    private static final int STOP_TRADE_TIMEOUT = 3;

    private static final int SAFE_TIMEOUT_MARGIN_SEC = 60;

    // Loader IDs
    private static final int ID_RATE_LOADER = 0;

    private static final String TRANSACTION_BROADCAST = "transaction_broadcast";
    private static final String ERROR = "error";
    private static final String EXCHANGE_ENTRY = "exchange_entry";
    private static final String DEPOSIT_ADDRESS = "deposit_address";
    private static final String DEPOSIT_AMOUNT = "deposit_amount";
    private static final String WITHDRAW_ADDRESS = "withdraw_address";
    private static final String WITHDRAW_AMOUNT = "withdraw_amount";

    private static final String PREPARE_TRANSACTION_BUSY_DIALOG_TAG = "prepare_transaction_busy_dialog_tag";
    private static final String SIGNING_TRANSACTION_BUSY_DIALOG_TAG = "signing_transaction_busy_dialog_tag";

    private Handler handler = new MyHandler(this);
    @Nullable private String password;
    private Listener listener;
    private ContentResolver contentResolver;
    private SignAndBroadcastTask signAndBroadcastTask;
    private CreateTransactionTask createTransactionTask;
    private WalletApplication application;
    private Configuration config;

    @Nullable AbstractAddress sendToAddress;
    boolean sendingToAccount;
    @Nullable private Value sendAmount;
    boolean emptyWallet;
    private CoinType sourceType;
    private SendRequest request;
    @Nullable private AbstractWallet sourceAccount;
    @Nullable private ExchangeEntry exchangeEntry;
    @Nullable private AbstractAddress tradeDepositAddress;
    @Nullable private Value tradeDepositAmount;
    @Nullable private AbstractAddress tradeWithdrawAddress;
    @Nullable private Value tradeWithdrawAmount;
    @Nullable private TxMessage txMessage;
    private boolean transactionBroadcast = false;
    @Nullable private Exception error;
    private HashMap<String, ExchangeRate> localRates = new HashMap<>();
    private CountDownTimer countDownTimer;

    @Bind(R.id.transaction_info) TextView transactionInfo;
    @Bind(R.id.password) EditText passwordView;
    @Bind(R.id.transaction_amount_visualizer) TransactionAmountVisualizer txVisualizer;
    @Bind(R.id.transaction_trade_withdraw) SendOutput tradeWithdrawSendOutput;

    public static MakeTransactionFragment newInstance(Bundle args) {
        MakeTransactionFragment fragment = new MakeTransactionFragment();
        fragment.setArguments(args);
        return fragment;
    }
    public MakeTransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        signAndBroadcastTask = null;

        setRetainInstance(true); // To handle async tasks

        Bundle args = getArguments();
        checkNotNull(args, "Must provide arguments");

        try {
            if (args.containsKey(ARG_SEND_REQUEST)) {
                request = (SendRequest) checkNotNull(args.getSerializable(ARG_SEND_REQUEST));
                checkState(request.isCompleted(), "Only completed requests are currently supported.");
                checkState(request.tx.getSentTo().size() == 1, "Only one output is currently supported");
