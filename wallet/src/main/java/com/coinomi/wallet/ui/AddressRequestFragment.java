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
    private stati