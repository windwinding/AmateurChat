package com.coinomi.wallet.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.wallet.R;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 *
 * TODO TransactionAmountVisualizerAdapter does a similar function, keep only one
 */
public class TransactionAmountVisualizer extends LinearLayout {

    private final SendOutput output;
    private final SendOutput fee;
    private final TextView txMessageLabel;
    private final TextView txMessage;
    private Value outputAmount;
    private Value feeAmount;
    private boolean isSending;

    private AbstractAddress address;
    private CoinType type;

    public TransactionAmountVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.transaction_amount_visualizer, this, true);

        output = (SendOutput) findViewById(R.id.transaction_output);
        output.setVisibility(View.GONE);
        fee = (SendOutput) findViewById(R.id.transaction_fee);
        fee.setVisibility(View.GONE);
        txMessageLabel = (TextView) findViewById(R.id.tx_message_label);
        txMessage = (TextView) findViewById(R.id.tx_message);

        if (isInEditMode()) {
            output.setVisibility(View.VISIBLE);
            fee.setVisibility(View.VISIBLE);
        }
    }

    public void setTransaction(@Nullable AbstractWallet pocket, AbstractTransaction tx) {
        type = tx.getType();
        String symbol = type.getSymbol();

        final Value value = pocket != null ? tx.getValue(pocket) : type.value(0);
        isSending = pocket != null ? value.signum() < 0 : true;
        // if sending and all the outputs point inside the current pocket. If received
        boolean isInternalTransfer = isSending;
        output.setVisibility(View.VISIBLE);
        List<AbstractOutput> outputs = tx.getSentTo();
        for (AbstractOutput txo : outputs) {
            if (isSending) {
                if (pocket != null && pocket.is