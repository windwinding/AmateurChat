package com.coinomi.core.wallet.families.vpncoin;

import com.coinomi.core.coins.VpncoinMain;
import com.coinomi.core.messages.MessageFactory;
import com.google.common.base.Charsets;

import org.bitcoinj.core.Transaction;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author John L. Jegutanis
 */
public class TransactionTest {
    static byte[] ORIGINAL_BYTES = "hello".getBytes(Charsets.UTF_8);
    static byte[] ENCRYPTED = Base64.decode("AwLL/0IqTyNPIA==");
    static long ENCRYPTION_KEY = 123