package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.coinomi.wallet.R;

/**
 * @author John L. Jegutanis
 */
public class DebuggingActivity extends BaseWalletActivity implements UnlockWalletDialog.Listener {

    private static final String DEBUGGING_TAG = "debugging_tag";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_wrapper);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DebuggingFragmen