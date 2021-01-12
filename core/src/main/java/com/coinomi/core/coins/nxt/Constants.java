package com.coinomi.core.coins.nxt;

/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

import java.util.Calendar;
import java.util.TimeZone;

public final class Constants {

//    public static final boolean isTestnet = Nxt.getBooleanProperty("nxt.isTestnet");
//    public static final boolean isOffline = Nxt.getBooleanProperty("nxt.isOffline");

    public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 176;
    public static final long MAX_BALANCE_NXT = 1000000000;
    public static final long ONE_NXT = 100000000;
    public static final long MAX_BALANCE_NQT = MAX_BALANCE_NXT * ONE_NXT;
    //public static final long INITIAL_BASE_TARGET = 153722867;
    //public static final long MAX_BASE_TARGET = MAX_BALANCE_NXT * INITIAL_BASE_TARGET;
//    public static final int MAX_ROLLBACK = Math.max(Nxt.getIntProperty("nxt.maxRollback"), 720);
//    public static final int GUARANTEED_BALANCE_CONFIRMATIONS = isTestnet ? Nxt.getIntProperty("nxt.testnetGuaranteedBalanceConfirmations", 1440) : 1440;
//    public static final int LEASING_DELAY = isTestnet ? Nxt.getIntProperty("nxt.testnetLeasingDelay", 1440) : 1440;

    //public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference
//    public static final int FORGING_DELAY = Nxt.getIntProperty("nxt.forgingDelay");
//    public static final int FORGING_SPEEDUP = Nxt.getIntProperty("nxt.forgingSpeedup");

    public static final byte MAX_PHASING_VOTE_TRANSACTIONS = 10;
    public static final byte MAX_PHASING_WHITELIST_SIZE = 10;
    public static final byte MAX_PHASING_LINKED_TRANSACTIONS = 10;
    public static final int MAX_PHASING_DURATION = 14 * 1440;
    public static final int MAX_PHASING_REVEALED_SECRET_LENGTH = 100;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 1000;

    public static final int MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024;
    public static final int MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH = 42 * 1024;

//    public static final int MIN_PRUNABLE_LIFETIME = isTestnet ? 1440 * 60 : 14 * 1440 * 60;
//    public static final int MAX_PRUNABLE_LIFETIME;
//    public static final boolean ENABLE_PRUNING;
//    static {
//        int maxPrunableLifetime = Nxt.getIntProperty("nxt.maxPrunableLifetime");
//        ENABLE_PRUNING = maxPrunableLifetime >= 0;
//        MAX_PRUNABLE_LIFETIME = ENABLE_PRUNING ? Math.max(maxPrunableLifetime, MIN_PRUNABLE_LIFETIME) : Integer.MAX_VALUE;
//    }
//    public static final boolean INCLUDE_EXPIRED_PRUNABLE = Nxt.getBooleanProperty("nxt.includeExpiredPrunable");

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;
    public static final int MAX_DIVIDEND_PAYMENT_ROLLBACK = 1441;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;
    public static final int MAX_POLL_DURATION = 14 * 1440;

    public static final byte MIN_VOTE_VALUE = -92;
    public static final byte MAX_VOTE_VALUE = 92;
    public static final byte NO_VOTE_VALUE = Byte.MIN_VALUE;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 1000;

    public static final int MAX_HUB_ANNOUNCEMENT_URIS = 100;
    public static final int MAX_HUB_ANNOUNCEMENT_URI_LENGTH = 1000;
    public static final long MIN_HUB_EFFECTIVE_BALANCE = 100000;

    public static final int MIN_CURRENCY_NAME_LENGTH = 3;
    public static final int MAX_CURRENCY_NAME_LENGTH = 10;
    public static final int MIN_CURRENCY_CODE_LENGTH = 3;
    public static final int MAX_CURRENCY_CODE_LENGTH = 5;
    public static final int MAX_CURRENCY_DESCRIPTION_LENGTH = 1000;
    public static final long MAX_CURRENCY_TOTAL_SUPPLY = 1000000000L * 100000000L;
    public static final int MAX_MINTING_RATIO = 10000; // per mint units not more than 0.01% of total supply
    public static final byte MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS = 3;
    public static final byte MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS = 100;
    public static final short MIN_SHUFFLING_DELAY = 5;
    public static final short MAX_SHUFFLING_DELAY = 1440;
    public static final int MAX_SHUFFLING_RECIPIENTS_LENGTH = 10000;

    public static final int MAX_TAGGED_DATA_NAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAGGED_DATA_TAGS_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_TYPE_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_CHANNEL_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_FILENAME_LENGTH = 100;
    public static final 