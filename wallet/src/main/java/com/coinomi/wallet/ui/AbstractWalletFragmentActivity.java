package com.coinomi.wallet.ui;

import android.support.v4.app.FragmentActivity;

import com.coinomi.wallet.WalletApplication;

/**
 * @author John L. Jegutanis
 */
abstract public class AbstractWalletFragmentActivity extends FragmentActivity {

    protected WalletApplication getWalletApplication() {
        return (WalletApplication) getApplication();
    }

    @Override
    protecte