
package com.coinomi.wallet.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.network.ConnectivityHelper;
import com.coinomi.core.network.ServerClients;
import com.coinomi.core.wallet.BitWalletSingleKey;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.SerializedKey;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.families.bitcoin.BitTransaction;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.WeakHandler;

import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;
import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.wallet.util.UiUtils.setGone;
import static com.coinomi.wallet.util.UiUtils.setVisible;


/**
 * A simple {@link Fragment} subclass.
 *
 */
public class SweepWalletFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(SweepWalletFragment.class);

    private static final int REQUEST_CODE_SCAN = 0;

    private static final String ERROR = "error";
    private static final String STATUS = "status";

    enum Error {NONE, BAD_FORMAT, BAD_COIN_TYPE, BAD_PASSWORD, ZERO_COINS, NO_CONNECTION, GENERIC_ERROR}
    enum TxStatus {INITIAL, DECODING, LOADING, SIGNING}

    // FIXME: Improve this: a reference to the task even if the fragment is recreated
    static SweepWalletTask sweepWalletTask;

    private final Handler handler = new MyHandler(this);
    private Listener listener;
    private ServerClients serverClients;
    private WalletAccount account;
    private SerializedKey serializedKey;
    private Error error = Error.NONE;
    private TxStatus status = TxStatus.INITIAL;

    @Bind(R.id.private_key_input) View privateKeyInputView;
    @Bind(R.id.sweep_wallet_key) EditText privateKeyText;
    @Bind(R.id.passwordView) View passwordView;
    @Bind(R.id.sweep_error) TextView errorΜessage;
    @Bind(R.id.passwordInput) EditText password;
    @Bind(R.id.sweep_loading) View sweepLoadingView;
    @Bind(R.id.sweeping_status) TextView sweepStatus;
    @Bind(R.id.button_next) Button nextButton;

    public SweepWalletFragment() { }

    public static SweepWalletFragment newInstance() {
        clearTasks();
        return new SweepWalletFragment();
    }

    static void clearTasks() {
        if (sweepWalletTask != null) {
            sweepWalletTask.cancel(true);
            sweepWalletTask = null;
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        checkNotNull(getArguments(), "Must provide arguments with an account id.");

        WalletApplication application = (WalletApplication) getActivity().getApplication();
        account = application.getAccount(getArguments().getString(Constants.ARG_ACCOUNT_ID));
        if (account == null) {
            Toast.makeText(getActivity(), R.string.no_such_pocket_error, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

        if (savedState != null) {
            error = (Error) savedState.getSerializable(ERROR);
            status = (TxStatus) savedState.getSerializable(STATUS);
        }

        if (sweepWalletTask != null) {
            switch (sweepWalletTask.getStatus()) {
                case FINISHED:
                    sweepWalletTask.onPostExecute(null);
                    break;
                case RUNNING:
                case PENDING:
                    sweepWalletTask.handler = handler;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sweep, container, false);
        ButterKnife.bind(this, view);

        if (getArguments().containsKey(Constants.ARG_PRIVATE_KEY)) {
            privateKeyText.setText(getArguments().getString(Constants.ARG_PRIVATE_KEY));
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {