
package com.coinomi.wallet.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FiatType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.exceptions.NoSuchPocketException;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.messages.MessageFactory;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.AddressBookProvider;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.tasks.MarketInfoPollTask;
import com.coinomi.wallet.ui.widget.AddressView;
import com.coinomi.wallet.ui.widget.AmountEditView;
import com.coinomi.wallet.util.ThrottlingWalletChangeListener;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;
import com.google.common.base.Charsets;

import org.acra.ACRA;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.annotation.Nullable;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.core.coins.Value.canCompare;
import static com.coinomi.wallet.ExchangeRatesProvider.getRates;
import static com.coinomi.wallet.util.UiUtils.setGone;
import static com.coinomi.wallet.util.UiUtils.setVisible;

/**
 * Fragment that prepares a transaction
 *
 * @author Andreas Schildbach
 * @author John L. Jegutanis
 */
public class SendFragment extends WalletFragment {
    private static final Logger log = LoggerFactory.getLogger(SendFragment.class);

    private enum State {
        INPUT, PREPARATION, SENDING, SENT, FAILED
    }

    // the fragment initialization parameters
    private static final int REQUEST_CODE_SCAN = 0;
    private static final int SIGN_TRANSACTION = 1;

    private static final int UPDATE_VIEW = 0;
    private static final int UPDATE_LOCAL_EXCHANGE_RATES = 1;
    private static final int UPDATE_WALLET_CHANGE = 2;
    private static final int UPDATE_MARKET = 3;
    private static final int SET_ADDRESS = 4;

    // Loader IDs
    private static final int ID_RATE_LOADER = 0;
    private static final int ID_RECEIVING_ADDRESS_LOADER = 1;

    // Saved state
    private static final String STATE_ADDRESS = "address";
    private static final String STATE_ADDRESS_CAN_CHANGE_TYPE = "address_can_change_type";
    private static final String STATE_AMOUNT = "amount";
    private static final String STATE_AMOUNT_TYPE = "amount_type";

    @Nullable private Value lastBalance; // TODO setup wallet watcher for the latest balance
    private State state = State.INPUT;
    private AbstractAddress address;
    private boolean addressTypeCanChange;
    private Value sendAmount;
    private CoinType sendAmountType;
    private MessageFactory messageFactory;
    private boolean isTxMessageAdded;
    private boolean isTxMessageValid;
    private WalletAccount account;

    private MyHandler handler = new MyHandler(this);
    private ContentObserver addressBookObserver = new AddressBookObserver(handler);
    private WalletApplication application;
    private Configuration config;
    private Map<String, ExchangeRate> localRates = new HashMap<>();
    private ShapeShiftMarketInfo marketInfo;

    @Bind(R.id.send_to_address)         AutoCompleteTextView sendToAddressView;
    @Bind(R.id.send_to_address_static)  AddressView sendToStaticAddressView;
    @Bind(R.id.send_coin_amount)        AmountEditView sendCoinAmountView;
    @Bind(R.id.send_local_amount)       AmountEditView sendLocalAmountView;
    @Bind(R.id.address_error_message)   TextView addressError;
    @Bind(R.id.amount_error_message)    TextView amountError;
    @Bind(R.id.amount_warning_message)  TextView amountWarning;
    @Bind(R.id.scan_qr_code)            ImageButton scanQrCodeButton;
    @Bind(R.id.erase_address)           ImageButton eraseAddressButton;
    @Bind(R.id.tx_message_add_remove)   Button txMessageButton;
    @Bind(R.id.tx_message_label)        TextView txMessageLabel;
    @Bind(R.id.tx_message)              EditText txMessageView;
    @Bind(R.id.tx_message_counter)      TextView txMessageCounter;
    @Bind(R.id.send_confirm)            Button sendConfirmButton;
    @Nullable ReceivingAddressViewAdapter sendToAdapter;
    CurrencyCalculatorLink amountCalculatorLink;
    Timer timer;
    MyMarketInfoPollTask pollTask;
    ActionMode actionMode;
    EditViewListener txMessageViewTextChangeListener;
    Listener listener;
    ContentResolver resolver;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the an account id.
     *
     * @param accountId the id of an account
     * @return A new instance of fragment WalletSendCoins.
     */
    public static SendFragment newInstance(String accountId) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.ARG_ACCOUNT_ID, accountId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using a URI.
     *
     * @param uri the payment uri
     * @return A new instance of fragment WalletSendCoins.
     */
    public static SendFragment newInstance(String accountId, CoinURI uri) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(Constants.ARG_URI, uri.toString());
        fragment.setArguments(args);
        return fragment;
    }

    public SendFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The onCreateOptionsMenu is handled in com.coinomi.wallet.ui.AccountFragment
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        WalletAccount a = null;
        if (args != null) {
            if (args.containsKey(Constants.ARG_ACCOUNT_ID)) {
                String accountId = args.getString(Constants.ARG_ACCOUNT_ID);
                a = checkNotNull(application.getAccount(accountId));
            }

            if (args.containsKey(Constants.ARG_URI)) {
                try {
                    processUri(args.getString(Constants.ARG_URI));
                } catch (CoinURIParseException e) {
                    // TODO handle more elegantly
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                    ACRA.getErrorReporter().handleException(e);
                }
            }

            // TODO review the following code. This is used when a user clicks on a URI.

            if (a == null) {
                List<WalletAccount> accounts = application.getAllAccounts();
                if (accounts.size() > 0) a = accounts.get(0);
                if (a == null) {
                    ACRA.getErrorReporter().putCustomData("wallet-exists",
                            application.getWallet() == null ? "no" : "yes");
                    Toast.makeText(getActivity(), R.string.no_such_pocket_error,
                            Toast.LENGTH_LONG).show();
                    getActivity().finish();
                    return;
                }
            }
            checkNotNull(a, "No account selected");
        } else {
            throw new RuntimeException("Must provide account ID or a payment URI");
        }

        sendAmountType = a.getCoinType();
        messageFactory = a.getCoinType().getMessagesFactory();
        account = a;

        if (savedInstanceState != null) {
            address = (AbstractAddress) savedInstanceState.getSerializable(STATE_ADDRESS);
            addressTypeCanChange = savedInstanceState.getBoolean(STATE_ADDRESS_CAN_CHANGE_TYPE);
            sendAmount = (Value) savedInstanceState.getSerializable(STATE_AMOUNT);
            sendAmountType = (CoinType) savedInstanceState.getSerializable(STATE_AMOUNT_TYPE);
        }

        updateBalance();

        String localSymbol = config.getExchangeCurrencyCode();
        for (ExchangeRatesProvider.ExchangeRate rate : getRates(getActivity(), localSymbol).values()) {
            localRates.put(rate.currencyCodeId, rate.rate);
        }
    }

    private void processUri(String uri) throws CoinURIParseException {
        CoinURI coinUri = new CoinURI(uri);
        CoinType scannedType = coinUri.getTypeRequired();

        if (!Constants.SUPPORTED_COINS.contains(scannedType)) {
            String error = getResources().getString(R.string.unsupported_coin, scannedType.getName());
            throw new CoinURIParseException(error);
        }

        if (account == null) {
            List<WalletAccount> allAccounts = application.getAllAccounts();
            List<WalletAccount> sendFromAccounts = application.getAccounts(scannedType);
            if (sendFromAccounts.size() == 1) {
                account = sendFromAccounts.get(0);
            } else if (allAccounts.size() == 1) {
                account = allAccounts.get(0);
            } else {
                throw new CoinURIParseException("No default account found");
            }
        }

        if (coinUri.isAddressRequest()) {
            UiUtils.replyAddressRequest(getActivity(), coinUri, account);
        } else {
            setUri(coinUri);
        }
    }

    private void updateBalance() {
        if (account != null) {
            lastBalance = account.getBalance();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_send, container, false);
        ButterKnife.bind(this, view);

        sendToAdapter = new ReceivingAddressViewAdapter(inflater.getContext());
        sendToAddressView.setAdapter(sendToAdapter);
        sendToAddressView.setOnFocusChangeListener(receivingAddressListener);
        sendToAddressView.addTextChangedListener(receivingAddressListener);

        sendCoinAmountView.resetType(sendAmountType, true);
        if (sendAmount != null) sendCoinAmountView.setAmount(sendAmount, false);
        sendLocalAmountView.setFormat(FiatType.FRIENDLY_FORMAT);
        amountCalculatorLink = new CurrencyCalculatorLink(sendCoinAmountView, sendLocalAmountView);
        amountCalculatorLink.setExchangeDirection(config.getLastExchangeDirection());
        amountCalculatorLink.setExchangeRate(getCurrentRate());

        addressError.setVisibility(View.GONE);
        amountError.setVisibility(View.GONE);
        amountWarning.setVisibility(View.GONE);

        setupTxMessage();

        return view;
    }

    @Override
    public void onDestroyView() {
        config.setLastExchangeDirection(amountCalculatorLink.getExchangeDirection());
        amountCalculatorLink = null;
        sendToAdapter = null;
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        amountCalculatorLink.setListener(amountsListener);

        resolver.registerContentObserver(AddressBookProvider.contentUri(
                getActivity().getPackageName()), true, addressBookObserver);

        addAccountEventListener(account);

        updateBalance();
        updateView();
    }

    @Override
    public void onPause() {
        removeAccountEventListener(account);

        resolver.unregisterContentObserver(addressBookObserver);

        amountCalculatorLink.setListener(null);

        finishActionMode();

        stopPolling();

        super.onPause();
    }

    private void addAccountEventListener(WalletAccount a) {
        if (a != null) {
            a.addEventListener(transactionChangeListener, Threading.SAME_THREAD);
        }
    }

    private void removeAccountEventListener(WalletAccount a) {
        if (a != null) a.removeEventListener(transactionChangeListener);
        transactionChangeListener.removeCallbacks();
    }

    private void setupTxMessage() {
        if (account == null || messageFactory == null) {
            txMessageButton.setVisibility(GONE);
            // Remove old listener if needed
            if (txMessageViewTextChangeListener != null) {
                txMessageView.removeTextChangedListener(txMessageViewTextChangeListener);
            }
            return;
        }

        txMessageButton.setVisibility(View.VISIBLE);
        txMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTxMessageAdded) { // if tx message added, remove it
                    hideTxMessage();
                } else { // else show tx message fields
                    showTxMessage();
                }
            }
        });

        final int maxMessageBytes = messageFactory.maxMessageSizeBytes();
        final int messageLengthThreshold = (int) (maxMessageBytes * .8); // 80% full
        final int txMessageCounterPaddingOriginal = txMessageView.getPaddingBottom();
        final int txMessageCounterPadding =
                getResources().getDimensionPixelSize(R.dimen.tx_message_counter_padding);
        final int colorWarn = getResources().getColor(R.color.fg_warning);
        final int colorError = getResources().getColor(R.color.fg_error);

        // Remove old listener if needed
        if (txMessageViewTextChangeListener != null) {
            txMessageView.removeTextChangedListener(txMessageViewTextChangeListener);
        }
        // This listener checks the length of the message and displays a counter if it passes a
        // threshold or the max size. It also changes the bottom padding of the message field
        // to accommodate the counter.
        txMessageViewTextChangeListener = new EditViewListener() {
            @Override
            public void afterTextChanged(final Editable s) {
                // Not very efficient because it creates a new String object on each key press
                int length = s.toString().getBytes(Charsets.UTF_8).length;
                boolean isTxMessageValidNow = true;
                if (length < messageLengthThreshold) {
                    if (txMessageCounter.getVisibility() != GONE) {
                        txMessageCounter.setVisibility(GONE);
                        txMessageView.setPadding(0, 0, 0, txMessageCounterPaddingOriginal);
                    }
                } else {
                    int remaining = maxMessageBytes - length;
                    if (txMessageCounter.getVisibility() != VISIBLE) {
                        txMessageCounter.setVisibility(VISIBLE);
                        txMessageView.setPadding(0, 0, 0, txMessageCounterPadding);
                    }
                    txMessageCounter.setText(Integer.toString(remaining));
                    if (length <= maxMessageBytes) {
                        txMessageCounter.setTextColor(colorWarn);
                    } else {
                        isTxMessageValidNow = false;
                        txMessageCounter.setTextColor(colorError);
                    }
                }
                // Update view only if the message validity changed
                if (isTxMessageValid != isTxMessageValidNow) {
                    isTxMessageValid = isTxMessageValidNow;
                    updateView();
                }
            }

            @Override
            public void onFocusChange(final View v, final boolean hasFocus) {
                if (!hasFocus) {
                    validateTxMessage();
                }
            }
        };

        txMessageView.addTextChangedListener(txMessageViewTextChangeListener);
    }

    private void showTxMessage() {
        if (messageFactory != null) {
            txMessageButton.setText(R.string.tx_message_public_remove);
            txMessageLabel.setVisibility(View.VISIBLE);
            txMessageView.setVisibility(View.VISIBLE);
            isTxMessageAdded = true;
            isTxMessageValid = true; // Initially the empty message is valid, even if it is ignored
        }
    }

    private void hideTxMessage() {
        if (messageFactory != null) {
            txMessageButton.setText(R.string.tx_message_public_add);
            txMessageLabel.setVisibility(View.GONE);
            txMessageView.setText(null);
            txMessageView.setVisibility(View.GONE);
            isTxMessageAdded = false;
            isTxMessageValid = false;
        }
    }

    @OnClick(R.id.erase_address)
    public void onAddressClearClick() {
        clearAddress(true);
        updateView();
    }

    private void clearAddress(boolean clearTextField) {
        address = null;
        if (clearTextField) setSendToAddressText(null);
        sendAmountType = account.getCoinType();
        addressTypeCanChange = false;
    }

    private void setAddress(AbstractAddress address, boolean typeCanChange) {
        this.address = address;
        this.addressTypeCanChange = typeCanChange;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_ADDRESS, address);
        outState.putBoolean(STATE_ADDRESS_CAN_CHANGE_TYPE, addressTypeCanChange);
        outState.putSerializable(STATE_AMOUNT, sendAmount);
        outState.putSerializable(STATE_AMOUNT_TYPE, sendAmountType);
    }