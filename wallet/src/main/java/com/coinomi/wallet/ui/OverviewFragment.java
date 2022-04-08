package com.coinomi.wallet.ui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.coinomi.core.coins.Value;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Configuration;
import com.coinomi.wallet.ExchangeRatesProvider;
import com.coinomi.wallet.ExchangeRatesProvider.ExchangeRate;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.ui.adaptors.AccountListAdapter;
import com.coinomi.wallet.ui.widget.Amount;
import com.coinomi.wallet.ui.widget.SwipeRefreshLayout;
import com.coinomi.wallet.util.ThrottlingWalletChangeListener;
import com.coinomi.wallet.util.UiUtils;
import com.coinomi.wallet.util.WeakHandler;
import com.google.common.collect.ImmutableMap;

import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;

/**
 * @author vbcs
 * @author John L. Jegutanis
 */
public class OverviewFragment extends Fragment{
    private static final Logger log = LoggerFactory.getLogger(OverviewFragment.class);

    private static final int WALLET_CHANGED = 0;
    private static final int UPDATE_VIEW = 1;
    private static final int SET_EXCHANGE_RATES = 2;

    private static final int ID_RATE_LOADER = 0;

    private final Handler handler = new MyHandler(this);

    private static class MyHandler extends WeakHandler<OverviewFragment> {
        public MyHandler(OverviewFragment ref) { super(ref); }

        @Override
        @SuppressWarnings("unchecked")
        protected void weakHandleMessage(OverviewFragment ref, Message msg) {
            switch (msg.what) {
                case WALLET_CHANGED:
                    ref.updateWallet();
                    break;
                case SET_EXCHANGE_RATES:
                    ref.setExchangeRates((Map<String, ExchangeRate>) msg.obj);
                    break;
                case UPDATE_VIEW:
                    ref.updateView();
                    break;
            }
        }
    }

    private Wallet wallet;
    private Value currentBalance;

    private boolean isFullAmount = false;
    private WalletApplication application;
    private Configuration config;

    private 