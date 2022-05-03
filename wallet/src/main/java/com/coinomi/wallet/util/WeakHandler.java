package com.coinomi.wallet.util;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;

/**
 * @author John L. Jegutanis
 */
public abstract class WeakHandler<T> extends Handler {
    private final WeakReference<T> reference;
    public WeakHandler(T ref) {
        this.reference = new WeakReference<T>(ref);
    }

    @Override
    public void handleMessage(Message msg) {
        T ref = reference.get();
        if