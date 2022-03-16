package com.coinomi.wallet.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FiatType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.exceptions.UnsupportedCoinTypeException;
import com.coinomi.core.uri.CoinURI;
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
import com.coinomi.wallet.ui.dialogs.CreateNewAddressDialog;
import com.coinomi.wallet.ui.widget.AmountEditView;
import com.coinomi.wallet.util.QrUtils;
import com.coinomi.wallet.util.ThrottlingWalletChangeListener;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.wallet.ExchangeRatesProvider.getRate;

/**
 *
 */
public class AddressRequestFragment extends WalletFragment {
    private static final Logger log = LoggerFactory.getLogger(AddressRequestFragment.class);

    private static final int UPDATE_VIEW = 0;
    private static final int UPDATE_EXCHANGE_RATE = 1;

    // Loader IDs
    private static final int ID_RATE_LOADER = 0;

    // Fragment tags
    private static final String NEW_ADDRESS_TAG = "new_address_tag";

    private CoinType type;
    @Nullable private AbstractAddress showAddress;
    private AbstractAddress receiveAddress;
    private Value amount;
    private String label;
    private String accountId;
    private WalletAccount account;
    private String message;

    @Bind(R.id.request_address_label) TextView addressLabelView;
    @Bind(R.id.request_address) TextView addressView;
    @Bind(R.id.request_coin_amount) AmountEditView sendCoinAmountView;
    @Bind(R.id.view_previous_addresses) View previousAddressesLink;
    @Bind(R.id.qr_code) ImageView qrView;
    String lastQrContent;
    CurrencyCalculatorLink amountCalculatorLink;
    ContentResolver resolver;

    private final MyHandler handler = new MyHandler(this);
    private final ContentObserver addressBookObserver = new AddressBookObserver(handler);
    private Configuration config;

    private static class MyHandler extends WeakHandler<AddressRequestFragment> {
        public MyHandler(AddressRequestFragment ref) { super(ref); }

        @Override
        protected void weakHandleMessage(AddressRequestFragment ref, Message msg) {
            switch (msg.what) {
                case UPDATE_VIEW:
                    ref.updateView();
                    break;
                case UPDATE_EXCHANGE_RATE:
                    ref.updateExchangeRate((ExchangeRate) msg.obj);
                    break;
            }
        }
    }

    static class AddressBookObserver extends ContentObserver {
        private final MyHandler handler;

        public AddressBookObserver(MyHandler handler) {
            super(handler);
            this.handler = handler;
        }

        @Override
        public void onChange(final boolean selfChange) {
            handler.sendEmptyMessage(UPDATE_VIEW);
        }
    }

    public static AddressRequestFragment newInstance(Bundle args) {
        AddressRequestFragment fragment = new AddressRequestFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddressRequestFragment newInstance(String accountId) {
        return newInstance(accountId, null);
    }

    public static AddressRequestFragment newInstance(String accountId,
                                                     @Nullable AbstractAddress showAddress) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_ACCOUNT_ID, accountId);
        if (showAddress != null) {
            args.putSerializable(Constants.ARG_ADDRESS, showAddress);
        }
        return newInstance(args);
    }
    public AddressRequestFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The onCreateOptionsMenu is handled in com.coinomi.