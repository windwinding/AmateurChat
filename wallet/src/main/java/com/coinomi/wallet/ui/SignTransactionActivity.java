package com.coinomi.wallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.coinomi.wallet.Constants;
import com.coinomi.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.coinomi.wallet.R;

import javax.annotation.Nullable;

public class SignTransactionActivity extends AbstractWalletFragmentActivity
        implements MakeTransactionFragment.Listener, TradeStatusFragment.Listener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_wrapper);

        if (savedInstanceState == null) {
            Fragment fragment = MakeTransactionFragment.newInstance(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.co