package com.coinomi.core.wallet.families.bitcoin;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

import static org.bitcoinj.core.TransactionInput.EMPTY_ARRAY;

/**
 * @author John L. Jegutanis
 */
public class OutPointOutput {
    final CoinType type;
    final TrimmedOutPoint outPoint;
    final TrimmedOutput output;
    final Value value;
    final boolean isGenerated;
    int appearedAtChainHeight = -1;
    int depth = 0;

    private OutPointOutput(CoinType type, TrimmedOutput output, boolean isGenerated) {
        this.type = type;
        this.output = ensureDetached(output);
        this.outPoint = output.getOutPointFor();
        this.value = type.value(output.getValue());
        this.isGenerated = isGenerated;
    }

    private TrimmedOutput ensureDetached(TrimmedOutput output) {
        if (output.isDetached()) {
            return output;
        } else {
            return new TrimmedOutput(output, output.getIndex(), output.getTxHash());
        }
    }

    public OutPointOutput(BitTransaction tx, long index) {
        this(new TrimmedOutput(tx.getOutput((int) index), index, tx.getHash()), tx.isGener