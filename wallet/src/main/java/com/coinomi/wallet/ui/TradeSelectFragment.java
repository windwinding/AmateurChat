
package com.coinomi.wallet.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.PeercoinMain;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftCoins;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.tasks.AddCoinTask;
import com.coinomi.wallet.tasks.ExchangeCheckSupportedCoinsTask;
import com.coinomi.wallet.tasks.MarketInfoPollTask;
import com.coinomi.wallet.ui.adaptors.AvailableAccountsAdaptor;
import com.coinomi.wallet.ui.dialogs.ConfirmAddCoinUnlockWalletDialog;
import com.coinomi.wallet.ui.widget.AmountEditView;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.ThrottlingWalletChangeListener;
import com.coinomi.wallet.util.WeakHandler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.acra.ACRA;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import javax.annotation.Nullable;

import static com.coinomi.core.coins.Value.canCompare;

/**
 * @author John L. Jegutanis
 */
public class TradeSelectFragment extends Fragment implements ExchangeCheckSupportedCoinsTask.Listener, AddCoinTask.Listener {
    private static final Logger log = LoggerFactory.getLogger(TradeSelectFragment.class);

    private static final int UPDATE_MARKET = 0;
    private static final int UPDATE_MARKET_ERROR = 1;
    private static final int UPDATE_WALLET = 2;
    private static final int VALIDATE_AMOUNT = 3;
    private static final int INITIAL_TASK_ERROR = 4;
    private static final int UPDATE_AVAILABLE_COINS = 5;

    private static final String INITIAL_TASK_BUSY_DIALOG_TAG = "initial_task_busy_dialog_tag";
    private static final String ADD_COIN_TASK_BUSY_DIALOG_TAG = "add_coin_task_busy_dialog_tag";
    private static final String ADD_COIN_DIALOG_TAG = "add_coin_dialog_tag";

    // UI & misc
    private WalletApplication application;
    private Wallet wallet;
    private final Handler handler = new MyHandler(this);
    private final AmountListener amountsListener = new AmountListener(handler);
    private final AccountListener sourceAccountListener = new AccountListener(handler);
    @Nullable private Listener listener;
    @Nullable private MenuItem actionSwapMenu;
    private Spinner sourceSpinner;
    private Spinner destinationSpinner;
    private AvailableAccountsAdaptor sourceAdapter;
    private AvailableAccountsAdaptor destinationAdapter;
    private AmountEditView sourceAmountView;
    private AmountEditView destinationAmountView;
    private CurrencyCalculatorLink amountCalculatorLink;
    private TextView amountError;
    private TextView amountWarning;
    private Button nextButton;

    // Tasks
    private MarketInfoTask marketTask;
    private ExchangeCheckSupportedCoinsTask initialTask;
    private Timer timer;
    private MyMarketInfoPollTask pollTask;
    private AddCoinTask addCoinAndProceedTask;

    // State
    private WalletAccount sourceAccount;
    @Nullable private WalletAccount destinationAccount;
    private CoinType destinationType;
    @Nullable private Value sendAmount;
    @Nullable private Value maximumDeposit;
    @Nullable private Value minimumDeposit;
    @Nullable private Value lastBalance;
    @Nullable private ExchangeRate lastRate;


    /** Required empty public constructor */
    public TradeSelectFragment() {}


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Android callback methods

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true); // Retain fragment as we are running async tasks

        // Select some default coins
        sourceAccount = application.getAccount(application.getConfiguration().getLastAccountId());
        if (sourceAccount == null) {
            List<WalletAccount> accounts = application.getAllAccounts();
            sourceAccount = accounts.get(0);
        }

        // Find a destination coin that is different than the source coin
        for (CoinType type : Constants.SUPPORTED_COINS) {
            if (type.equals(sourceAccount.getCoinType())) continue;
            destinationType = type;
            break;
        }

        updateBalance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trade_select, container, false);

        sourceSpinner = (Spinner) view.findViewById(R.id.from_coin);
        sourceSpinner.setAdapter(getSourceSpinnerAdapter());
        sourceSpinner.setOnItemSelectedListener(getSourceSpinnerListener());

        destinationSpinner = (Spinner) view.findViewById(R.id.to_coin);
        destinationSpinner.setAdapter(getDestinationSpinnerAdapter());
        destinationSpinner.setOnItemSelectedListener(getDestinationSpinnerListener());

        sourceAmountView = (AmountEditView) view.findViewById(R.id.trade_coin_amount);
        destinationAmountView = (AmountEditView) view.findViewById(R.id.receive_coin_amount);

        amountCalculatorLink = new CurrencyCalculatorLink(sourceAmountView, destinationAmountView);

//        receiveCoinWarning = (TextView) view.findViewById(R.id.warn_no_account_found);
//        receiveCoinWarning.setVisibility(View.GONE);
//        addressError = (TextView) view.findViewById(R.id.address_error_message);
//        addressError.setVisibility(View.GONE);
        amountError = (TextView) view.findViewById(R.id.amount_error_message);
        amountError.setVisibility(View.GONE);
        amountWarning = (TextView) view.findViewById(R.id.amount_warning_message);
        amountWarning.setVisibility(View.GONE);

//        scanQrCodeButton = (ImageButton) view.findViewById(R.id.scan_qr_code);
//        scanQrCodeButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                handleScan();
//            }
//        });

        view.findViewById(R.id.powered_by_shapeshift).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.about_shapeshift_title)
                        .setMessage(R.string.about_shapeshift_message)
                        .setPositiveButton(R.string.button_ok, null)
                        .create().show();
            }
        });

        nextButton = (Button) view.findViewById(R.id.button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                validateAddress();
                validateAmount();
                if (everythingValid()) {
                    onHandleNext();
                } else if (amountCalculatorLink.isEmpty()) {
                    amountError.setText(R.string.amount_error_empty);
                    amountError.setVisibility(View.VISIBLE);
                }
            }
        });

        // Setup the default source & destination views
        setSource(sourceAccount, false);
        if (destinationAccount != null) {
            setDestination(destinationAccount, false);
        } else {
            setDestination(destinationType, false);
        }

        if (!application.isConnected()) {
            showInitialTaskErrorDialog(null);
        } else {
            maybeStartInitialTask();
        }

        return view;
    }

    private AvailableAccountsAdaptor getDestinationSpinnerAdapter() {
        if (destinationAdapter == null) {
            destinationAdapter = new AvailableAccountsAdaptor(getActivity());
        }
        return destinationAdapter;
    }

    private AvailableAccountsAdaptor getSourceSpinnerAdapter() {
        if (sourceAdapter == null) {
            sourceAdapter = new AvailableAccountsAdaptor(getActivity());
        }
        return sourceAdapter;
    }


    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.application = (WalletApplication) context.getApplicationContext();
            this.wallet = application.getWallet();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.trade, menu);
        actionSwapMenu = menu.findItem(R.id.action_swap_coins);
    }

    @Override
    public void onPause() {
        stopPolling();

        removeSourceListener();

        amountCalculatorLink.setListener(null);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();

        amountCalculatorLink.setListener(amountsListener);

        addSourceListener();

        updateNextButtonState();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // TODO allow to trade all coins
//            case R.id.action_empty_wallet:
//                setAmountForEmptyWallet();
//                return true;
            case R.id.action_refresh:
                refreshStartInitialTask();
                return true;
            case R.id.action_swap_coins:
                swapAccounts();
                return true;
            case R.id.action_exchange_history:
                startActivity(new Intent(getActivity(), ExchangeHistoryActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods

    private void onHandleNext() {
        if (listener != null) {
            if (destinationAccount == null) {
                createToAccountAndProceed();
            } else {
                if (everythingValid()) {
                    Keyboard.hideKeyboard(getActivity());
                    listener.onMakeTrade(sourceAccount, destinationAccount, sendAmount);
                } else {
                    Toast.makeText(getActivity(), R.string.amount_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void createToAccountAndProceed() {
        if (destinationType == null) {
            Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        ConfirmAddCoinUnlockWalletDialog.getInstance(destinationType, wallet.isEncrypted())
                .show(getFragmentManager(), ADD_COIN_DIALOG_TAG);
    }

    /**
     * Start account creation task and proceed
     */
    void maybeStartAddCoinAndProceedTask(@Nullable String description, @Nullable CharSequence password) {
        if (addCoinAndProceedTask == null) {
            addCoinAndProceedTask = new AddCoinTask(this, destinationType, wallet, description, password);
            addCoinAndProceedTask.execute();
        }
    }

    @Override
    public void onAddCoinTaskStarted() {
        Dialogs.ProgressDialogFragment.show(getFragmentManager(),
                getResources().getString(R.string.adding_coin_working, destinationType.getName()),
                ADD_COIN_TASK_BUSY_DIALOG_TAG);
    }

    @Override
    public void onAddCoinTaskFinished(Exception error, WalletAccount newAccount) {
        if (Dialogs.dismissAllowingStateLoss(getFragmentManager(), ADD_COIN_TASK_BUSY_DIALOG_TAG)) return;

        if (error != null) {
            if (error instanceof KeyCrypterException) {
                showPasswordRetryDialog();
            } else {
                ACRA.getErrorReporter().handleSilentException(error);
                Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_LONG).show();
            }
        } else {
            destinationAccount = newAccount;
            destinationType = newAccount.getCoinType();
            onHandleNext();
        }
        addCoinAndProceedTask = null;
    }

    private void addSourceListener() {
        sourceAccount.addEventListener(sourceAccountListener, Threading.SAME_THREAD);
        onWalletUpdate();
    }

    private void removeSourceListener() {
        sourceAccount.removeEventListener(sourceAccountListener);
        sourceAccountListener.removeCallbacks();
    }

    /**
     * Start polling for the market information of the current pair, if it is already stated this
     * call does nothing
     */
    private void startPolling() {
        if (timer == null) {
            ShapeShift shapeShift = application.getShapeShift();
            pollTask = new MyMarketInfoPollTask(handler, shapeShift, getPair());
            timer = new Timer();
            timer.schedule(pollTask, 0, Constants.RATE_UPDATE_FREQ_MS);
        }
    }

    /**
     * Stop the polling for the market info, if it is already stop this call does nothing
     */
    private void stopPolling() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
            pollTask.cancel();
            pollTask = null;
        }
    }

    /**
     * Updates the spinners to include only available and supported coins
     */
    private void updateAvailableCoins(ShapeShiftCoins availableCoins) {
        List<CoinType> supportedTypes = getSupportedTypes(availableCoins.availableCoinTypes);
        List<WalletAccount> allAccounts = application.getAllAccounts();

        sourceAdapter.update(allAccounts, supportedTypes, false);
        List<CoinType> sourceTypes = sourceAdapter.getTypes();

        // No supported source accounts found
        if (sourceTypes.size() == 0) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.trade_error)
                    .setMessage(R.string.trade_error_no_supported_source_accounts)
                    .setPositiveButton(R.string.button_ok, null)
                    .create().show();
            return;
        }

        if (sourceSpinner.getSelectedItemPosition() == -1) {
            if (sourceAccount != null && sourceAdapter.getAccountOrTypePosition(sourceAccount) != -1) {
                sourceSpinner.setSelection(sourceAdapter.getAccountOrTypePosition(sourceAccount));
            } else {
                sourceSpinner.setSelection(0);
            }
        }
        CoinType sourceType =
                ((AvailableAccountsAdaptor.Entry) sourceSpinner.getSelectedItem()).getType();

        // If we have only one source type, remove it as a destination
        if (sourceTypes.size() == 1) {
            ArrayList<CoinType> typesWithoutSourceType = Lists.newArrayList(supportedTypes);
            typesWithoutSourceType.remove(sourceType);
            destinationAdapter.update(allAccounts, typesWithoutSourceType, true);
        } else {
            destinationAdapter.update(allAccounts, supportedTypes, true);
        }

        if (destinationSpinner.getSelectedItemPosition() == -1) {
            for (AvailableAccountsAdaptor.Entry entry : destinationAdapter.getEntries()) {
                // Select the first item that is of a different type than the source
                if (!sourceType.equals(entry.getType())) {
                    int selectionIndex = destinationAdapter.getAccountOrTypePosition(
                            entry.accountOrCoinType);
                    destinationSpinner.setSelection(selectionIndex);
                    break;