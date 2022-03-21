package com.coinomi.wallet.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.coinomi.core.wallet.Wallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.WalletApplication;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.wallet.Protos;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.Arrays;

import javax.annotation.Nullable;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.coinomi.core.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
public class DebuggingFragment extends Fragment {
    private static final String PROCESSING_DIALOG_TAG = "processing_dialog_tag";
    private static final String PASSWORD_DIALOG_TAG = "password_dialog_tag";

    private CharSequence password;
    private PasswordTestTask passwordTestTask;
    private Wallet wallet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // for the async task
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_debugging, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        WalletApplication application = (WalletApplication) context.getApplicationContext();
        wallet = application.getWallet();
    }

    @OnClick(R.id.button_execute_password_test)
    void onExecutePasswordTest() {
        if (wallet.isEncrypted()) {
            showUnlockDialog();
        } else {
            DialogBuilder.warn(getActivity(), R.string.wallet_is_not_locked_message)
                    .setPositiveButton(R.string.button_ok, null)
                    .create().show();
        }
    }

    private void showUnlo