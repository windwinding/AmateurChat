
package com.coinomi.wallet.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.wallet.AddressBookProvider;
import com.coinomi.wallet.R;

/**
 * @author John L. Jegutanis
 */
public class SendOutput extends LinearLayout {
    private final Context context;
    private TextView sendTypeText;
    private TextView amount;
    private TextView symbol;
    private TextView amountLocal;
    private TextView symbolLocal;
    private TextView addressLabelView;
    private TextView addressView;

    private AbstractAddress address;
    private String label;
    private boolean isSending;
    private String sendLabel;
    private String receiveLabel;
    private String feeLabel;

    public SendOutput(Context context) {
        super(context);
        this.context = context;

        inflateView(context);
    }

    public SendOutput(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        inflateView(context);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SendOutput, 0, 0);
        try {
            setIsFee(a.getBoolean(R.styleable.SendOutput_is_fee, false));
        } finally {
            a.recycle();
        }
    }

    private void inflateView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.transaction_output, this, true);

        sendTypeText = (TextView) findViewById(R.id.send_output_type_text);
        amount = (TextView) findViewById(R.id.amount);
        symbol = (TextView) findViewById(R.id.symbol);
        amountLocal = (TextView) findViewById(R.id.local_amount);
        symbolLocal = (TextView) findViewById(R.id.local_symbol);
        addressLabelView = (TextView) findViewById(R.id.output_label);
        addressView = (TextView) findViewById(R.id.output_address);

        amountLocal.setVisibility(GONE);
        symbolLocal.setVisibility(GONE);
        addressLabelView.setVisibility(View.GONE);
        addressView.setVisibility(View.GONE);
    }

    public void setAmount(String amount) {
        this.amount.setText(amount);
    }

    public void setSymbol(String symbol) {
        this.symbol.setText(symbol);
    }

    public void setAmountLocal(String amount) {
        this.amountLocal.setText(amount);
        this.amountLocal.setVisibility(VISIBLE);
    }

    public void setSymbolLocal(String symbol) {
        this.symbolLocal.setText(symbol);
        this.symbolLocal.setVisibility(VISIBLE);
    }

    public void setAddress(AbstractAddress address) {
        this.address = address;
        updateView();
    }