package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.coinomi.wallet.R;
import com.coinomi.wallet.tasks.AddCoinTask;
import com.coinomi.wallet.ui.dialogs.ConfirmAddCoinUnlockWalletDialog;

import org.bitcoinj.crypto.KeyCrypterException;

import javax.annotation.Nullable;


public class TradeActivity extends BaseWalletActivity implements
        TradeSelectFragment.Listener, MakeTransactionFragment.Listener, TradeStatusFragment.Listener,
        ConfirmAddCoinUnlockWalletDialog.Listener {

    private static final String TRADE_SELECT_FRAGMENT_TAG = "trade_select_fragment_tag";

    private int containerRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_wrapper);

        containerRes = R.id.container;

        if (savedInstanceState == null) {
            getSupportFra