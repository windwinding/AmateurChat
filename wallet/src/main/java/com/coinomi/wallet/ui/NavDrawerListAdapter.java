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
    private final LayoutInflater inflater;
    private List<NavDrawerItem> items = new ArrayList<>();

    public NavDrawerListAdapter(final Context context, List<NavDrawerItem> items) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.items = items;
    }

    public void setItems(List<NavDrawerItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).itemType.ordinal();
    }

    @Override
    public int getViewTypeCount() {
        return NavDrawerItemType.values().length;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public NavDrawerItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View row, ViewGroup parent) {
        NavDrawerItem item = getItem(position);

        if (row == null) {
            switch (item.itemType) {
                case ITEM_SEPARATOR:
                    row = inflater.inflate(R.layout.nav_drawer_separator, null);
                    break;
                case ITEM_SECTION_TITLE:
                    row = inflater.inflate(R.layout.nav_drawer_section_title, null);
                    break;
                case ITEM_COIN:
                case ITEM_OVERVIE