package com.coinomi.wallet;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.text.format.DateUtils;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.wallet.util.WalletUtils;
import com.google.common.collect.ImmutableMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 * @author Andreas Schildbach
 */
public class Configuration {

    public final int lastVersionCode;

    private final SharedPreferences prefs;

    private static final String PREFS_KEY_LAST_VERSION = "last_version";
    private static final String PREFS_KEY_LAST_USED = "last_