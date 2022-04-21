package com.coinomi.wallet.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.wallet.ExchangeRatesProvider.ExchangeRate;
import com.coinomi.wallet.R;
import com.coinomi.wallet.util.WalletUtils;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author John L. Jegutanis
 */
public class CoinListItem extends LinearLayout implements Checkable {
    final View view;
    @Bind(R.id.item_icon) ImageView icon;
    @Bind(R.id.item_text) TextView title;
    @Bind(R.id.amount) Amount amount;

    private boolean isChecked = false;
    private CoinType type;

    public CoinListItem(Context context) {
  