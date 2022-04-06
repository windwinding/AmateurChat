package com.coinomi.wallet.ui;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coinomi.wallet.R;
import com.coinomi.wallet.ui.widget.NavDrawerItemView;

import java.util.ArrayList;
import java.util.List;

import static com.coinomi.wallet.ui.NavDrawerItemType.ITEM_SECTION_TITLE;
import static com.coinomi.wallet.ui.NavDrawerItemType.ITEM_SEPARATOR;

/**
 * @author John L. Jegutanis
 */
public class NavDrawerListAdapter extends BaseAdapter {
    private final Context context;
    private f