package com.coinomi.core.coins.nxt;


//import org.json.simple.JSONObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class TransactionType {

    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;
    private static final byte TYPE_DIGITAL_GOODS = 3;
    private static final byte TYPE_ACCOUNT_CONTROL = 4;
    
    private static final byte TYPE_BURST_MINING = 20; // jump some for easier nxt updating
    private static final byte TYPE_ADVANCED_PAYMENT = 21;
  