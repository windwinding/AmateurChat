package com.coinomi.wallet.ui;

/*
 * Copyright 2011-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.BitcoinMain;
import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.ExchangeRatesProvider.ExchangeRate;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.widget.Amount;
import com.coinomi.wallet.util.WalletUtils;

import org.bitcoinj.core.Coin;

import javax.annotation.CheckForNull;


/**
 * @author Andreas Schildbach
 * @author John L. Jegutanis
 */
public final class ExchangeRatesFragment extends ListFragment implements OnSharedPreferenceChangeListener {
    private Context context;
    private WalletApplication application;
    private Configuration config;
    private com.coinomi.core.wallet.Wallet wallet;
    private Uri contentUri;
    private LoaderManager loaderManager;

    private ExchangeRatesAdapter adapter;
    private String query = null;

    private Coin balance = null;
    @CheckForNull
    private String defaultCurrency = null;

    private static final int ID_BALANCE_LOADER = 0;
    private static final int ID_RATE_LOADER = 1;
    private static final int ID_BLOCKCHAIN_STATE_LOADER = 2;
    private CoinType type;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        this.context = context;
        this.application = (WalletApplication) context.getApplicationContext();
        this.config = application.getConfiguration();
        this.wallet = application.getWallet();

        this.loaderManager = getLoaderManager();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(Constants.ARG_COIN_ID)) {
            type = CoinID.typeFromId(getArguments().getString(Constants.ARG_COIN_ID));
        } else {
            type = BitcoinMain.get();
        }
        contentUri = ExchangeRatesProvider.contentUriToLocal(context.getPackageName(),
                type.getSymbol(), false);

        defaultCurrency = config.getExchangeCurrencyCode();
        config.registerOnSharedPreferenceChangeListener(this);

        adapter = new ExchangeRatesAdapter(context);
        setListAdapter(adapter);

        loaderManager.initLoader(ID_RATE_LOADER, null, rateLoaderCallbacks);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange_rates, container, false);
    }

    @Override
    public void setEmptyText(final CharSequence text) {
        final TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
        emptyView.setText(text);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setFastScrollEnabled(true);
        setEmptyText(getString(R.string.exchange_rates_loading));
    }

    @Ov