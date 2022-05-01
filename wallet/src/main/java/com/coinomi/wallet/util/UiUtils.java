package com.coinomi.wallet.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.AddressBookProvider;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.AccountDetailsActivity;
import com.coinomi.wallet.ui.EditAccountFragment;
import com.coinomi.wallet.ui.EditAddressBookEntryFragment;

import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John L. Jegutanis
 */
public class UiUtils {
    private static final Logger log = LoggerFactory.getLogger(UiUtils.class);

    static public void toastGenericError(Context context) {
        Toast.makeText(context, R.string.error