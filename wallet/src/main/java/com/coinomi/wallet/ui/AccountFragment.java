package com.coinomi.wallet.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.Constants;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;
import com.coinomi.wallet.util.Keyboard;
import com.coinomi.wallet.util.WeakHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.coinomi.wallet.util.UiUtils.toastGenericError;

/**
 * @author John L. Jegutanis
 */
public class AccountFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(AccountFragment.class);

    private static final String ACCOUNT_CURRENT_SCREEN = "account_current_screen";
    private static final int NUM_OF_SCREENS = 3;
    // Set offscreen page limit to 2 because receive fragment draws a QR code and we don't
    // want to re-render that if we go to the SendFragment and back
    private static final int OFF_SCREEN_LIMIT = 2;

    // Screen ids
    private static final int RECEIVE = 0;
    private static final int BALANCE = 1;
    private static final int SEND = 2;

    // Handler ids
    private static final int SEND_TO_URI = 0;

    private int currentScreen;
    @Bind(R.id.pager) ViewPager viewPager;
    NavigationDrawerFragment mNa