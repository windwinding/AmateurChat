package com.coinomi.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.coinomi.wallet.R;

/**
 * Activity that displays a list of previously used addresses.
 * @author John L. Jegutanis
 */
public class PreviousAddressesActivity extends BaseWalletActivity implements
        PreviousAddressesFragment.Listener {

    private static final String LIST_ADDRESSES_TAG = "list_addresses_tag";
    private static final String ADDRESS_TAG = "address_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_wrapper);

        if (savedInstanceState == null) {
            PreviousAddressesFragment addressesList = new PreviousAddressesFragment();
            addressesList.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, addressesL