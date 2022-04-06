package com.coinomi.wallet.ui;

import java.util.List;

/**
 * @author John L. Jegutanis
 */
public class NavDrawerItem {
    NavDrawerItemType itemType;
    String title;
    int iconRes;
    Object itemData;

    public NavDrawerItem(NavDrawerItemType itemType, String title, int iconRes, Object itemData) {
        this.itemType = ite