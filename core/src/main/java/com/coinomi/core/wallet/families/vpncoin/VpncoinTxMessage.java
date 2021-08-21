package com.coinomi.core.wallet.families.vpncoin;

import com.coinomi.core.messages.MessageFactory;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.families.bitcoin.BitTransaction;
import com.google.common.base.Charsets;

import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Base64;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkArgument;

/**
 * @author John L. Jegutanis
 */
public class VpncoinTxMessage implements TxMessage {
    private static final Logger log = LoggerFactory.getLogger(VpncoinTxMessage.class);

    static final int SIZE_LENGTH = 4;

    public static final int MAX_TX_DATA      = 65536;
    public static final int MAX_TX_DATA_FROM = 128;
    public static final int MAX_TX_DATA_SUBJ = 128;
    public static final int MAX_TX_DATA_MSG  = MAX_TX_DATA - MAX_TX_DATA_FROM - MAX_TX_DATA_SUBJ;

    static final Pattern MESSAGE_REGEX =
            Pattern.compile("(?s)@(?:FROM|SUBJ|MSG)=.*?(?=@(?:FROM|SUBJ|MSG)=|$)");

    static final String FROM = "@FROM=";
    static final String SUBJ = "@SUBJ=";
    static final String MSG  = "@MSG=";

    private String from;
    private String subject;
    private String message;

    VpncoinTxMessage() { }

    VpncoinTxMessage(String message) {
        setMessage(message);
    }

    VpncoinTxMessage(String from, String subject, String message) {
        setFrom(from);
        setSubject(subject);
        setMessage(message);
    }

    private transient static VpncoinMessageFactory instance = new VpncoinMessageFactory();
    public static MessageFactory getFactory() {
        return instance;
    }

    public static VpncoinTxMessage create(String message) throws IllegalArgumentException {
        return new VpncoinTxMessage(message);
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public void setFrom(String from) {
        checkArgument(from.length() <= MAX_TX_DATA_FROM, "'From' field size exceeded");
        this.from = from;
    }

    public void setSubject(String subject) {
        checkArgument(subject.length() <= MAX_TX_DATA_SUBJ, "'Subject' field size exceeded");
        this.subject = subject;
    }

    public void setMessage(String message) {
        checkArgument(message.length() <= MAX_TX_DATA_MSG, "'Message' field size exceeded");
        this.message = message;
    }

    @Nullable
    public static VpncoinTxMessage parse(AbstractTransaction tx) {
        Transaction rawTx = null;
        byte[] bytes;
        String fullMessage = null;
        try {
            rawTx = ((BitTransaction) tx).getRawTransaction();
            bytes = rawTx.getExtraBytes();
            if (bytes == null || bytes.length == 0) return null;

            fullMessage = new String(bytes, Charsets.UTF_8);
            return parseUnencrypted(fullMessage);
        } catch (Exception e) {
            if (rawTx == null || fullMessage == null) return null;
            try {
                return parseEncrypted(rawTx.getTime(), fullMessage);
            } catch (Exception e1) {
                log.info("Could not parse message: {}", e1.getMessage());
                return null;
            }
        }
    }

    @Nullable
    public static VpncoinTxMessage parse(String fullMessage) {
        if (fullMessage == null || fullMessage.length() == 0) {
            return null;
        }

        try {
            return parseUnencrypted(fullMessage);
        } catch (Exception e) {
            log.info("Could not parse message: {}", e.getLocalizedMessage());
            return null;
        }
    }

    public boolean isEmpty() {
        return isNullOrEmpty(from) && isNullOrEmpty(subject) && isNullOrEmpty(message);
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    @Override
    public Type getType() {
        return Type.PUBLIC; // Only public is supported
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isNullOrEmpty(from)) {
            sb.append(from);
        }
        if (!isNullOrEmpty(subject)) {
            if (sb.length() != 0) sb.append("\n\n");
            sb.append(subject);
        }
        if (!isNullOrEmpty(message)) {
            if (sb.length() != 0) sb.append("\n\n");
            sb.append(message);
        }

        return sb.toString();
    }

    @Override
    public void serializeTo(AbstractTransaction transaction) {
        if (transaction instanceof BitTransaction) {
            Transaction rawTx = ((BitTransaction) transaction).getRawTransaction();
            rawTx.setExtraBytes(serialize());
        }
    }

    byte[] serialize() {
        StringBuilder sb = new StringBuilder();
        if (!isNullOrEmpty(from)) {
            sb.append(FROM);
            sb.append(from);
        }
        if (!isNullOrEmpty(subject)) {
            sb.append(SUBJ);
            sb.append(subject);
        }
        if (!isNullOrEmpty(message)) {
            sb.append(MSG);
            sb.append(message);
        }

        return sb.toString().getBytes(Charsets.UTF_8);
    }

    static VpncoinTxMessage parseEncrypted(long key, String fullMessage) throws Exception {
        return parseUnencrypted(decrypt(key, fullMessage));
    }

    static VpncoinTxMessage parseUnencrypted(String fullMessage) throws Exception {
        checkArgument(fullMessage.length() <= MAX_TX_DATA, "Maximum data size exceeded");

        Matcher 