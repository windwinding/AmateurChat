package com.coinomi.wallet.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.SendOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John L. Jegutanis
 */
public class TransactionAmountVisualizerAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;

    private final AbstractWallet pocket;
    private boolean isSending;
    private CoinType type;
    private String symbol;
    private List<AbstractTransaction.AbstractOutput> outputs;
    private boolean hasFee;
    private Value feeAmount;

    private int itemCount;

    public TransactionAmountVisualizerAdapter(final Context context, final AbstractWallet walletPocket) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        pocket = walletPocket;
        type = pocket.getCoinType();
        symbol = type.getSymbol();
        outputs = new ArrayList<>();
    }

    public void setTransaction(AbstractTransaction tx) {
        outputs.clear();
        final Value value = tx.getValue(pocket);
        isSending = value.signum() < 0;
        // if sending and all the outputs point inside the current pocket it is an internal transfer
        boolean isInternalTransfer = isSending;

        for (AbstractOutput output : tx.getSentTo()) {
            if (isSending) {
                // When sending hide change outp