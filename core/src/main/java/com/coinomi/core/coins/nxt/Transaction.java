package com.coinomi.core.coins.nxt;

import java.util.List;

public interface Transaction extends Comparable<Transaction> {



    public static interface Builder {

        Builder recipientId(long recipientId);

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder message(Appendix.M