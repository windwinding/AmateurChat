package com.coinomi.wallet.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.coinomi.wallet.R;

/**
 * @author John L. Jegutanis
 */
public class UnlockWalletDialog extends DialogFragment {
    private TextView passwordView;
    private Listener listener;