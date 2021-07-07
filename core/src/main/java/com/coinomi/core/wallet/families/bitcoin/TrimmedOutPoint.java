package com.coinomi.core.wallet.families.bitcoin;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;

/**
 * @author John L. Jegutanis
 */
public class TrimmedOutPoint extends TransactionOutPoint {
    final TrimmedOutput connectedOutput;

    public TrimmedOutPoint(TrimmedOutput txo, Sha256Hash txHash) {
        super(txo.getParams(), txo.getIndex(), txHash);
        connectedOutp