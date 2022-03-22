package com.coinomi.wallet.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;

import butterknife.ButterKnife;

import static com.coinomi.core.Preconditions.checkNotNull;
import static com.coinomi.wallet.Constants.ARG_ACCOUNT_ID;

/**
 * @author John L. Jegutanis
 */
public final class EditAccountF