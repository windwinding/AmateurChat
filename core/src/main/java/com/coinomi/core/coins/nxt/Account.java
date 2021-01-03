
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

//import nxt.crypto.Crypto;
//import nxt.crypto.EncryptedData;
//import nxt.db.DbClause;
//import nxt.db.DbIterator;
//import nxt.db.DbKey;
//import nxt.db.DbUtils;
//import nxt.db.DerivedDbTable;
//import nxt.db.PersistentDbTable;
//import nxt.db.VersionedEntityDbTable;
//import nxt.util.Convert;
//import nxt.util.Listener;
//import nxt.util.Listeners;
//import nxt.util.Logger;

@SuppressWarnings({"UnusedDeclaration", "SuspiciousNameCombination"})
public final class Account {

//    public enum Event {
//        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE, CURRENCY_BALANCE, UNCONFIRMED_CURRENCY_BALANCE,
//        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED
//    }
//
//    public static final class AccountAsset {
//
//        private final long accountId;
//        private final long assetId;
//        private final DbKey dbKey;
//        private long quantityQNT;
//        private long unconfirmedQuantityQNT;
//
//        private AccountAsset(long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT) {
//            this.accountId = accountId;
//            this.assetId = assetId;
//            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
//            this.quantityQNT = quantityQNT;
//            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
//        }
//
//        private AccountAsset(ResultSet rs) throws SQLException {
//            this.accountId = rs.getLong("account_id");
//            this.assetId = rs.getLong("asset_id");
//            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.assetId);
//            this.quantityQNT = rs.getLong("quantity");
//            this.unconfirmedQuantityQNT = rs.getLong("unconfirmed_quantity");
//        }
//
//        private void save(Connection con) throws SQLException {
//            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
//                    + "(account_id, asset_id, quantity, unconfirmed_quantity, height, latest) "
//                    + "KEY (account_id, asset_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
//                int i = 0;
//                pstmt.setLong(++i, this.accountId);
//                pstmt.setLong(++i, this.assetId);
//                pstmt.setLong(++i, this.quantityQNT);
//                pstmt.setLong(++i, this.unconfirmedQuantityQNT);
//                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
//                pstmt.executeUpdate();
//            }
//        }
//
//        public long getAccountId() {
//            return accountId;
//        }
//
//        public long getAssetId() {
//            return assetId;
//        }
//
//        public long getQuantityQNT() {
//            return quantityQNT;
//        }
//
//        public long getUnconfirmedQuantityQNT() {
//            return unconfirmedQuantityQNT;
//        }
//
//        private void save() {
//            checkBalance(this.accountId, this.quantityQNT, this.unconfirmedQuantityQNT);
//            if (this.quantityQNT > 0 || this.unconfirmedQuantityQNT > 0) {
//                accountAssetTable.insert(this);
//            } else {
//                accountAssetTable.delete(this);
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "AccountAsset account_id: " + Long.toUnsignedString(accountId) + " asset_id: " + Long.toUnsignedString(assetId)
//                    + " quantity: " + quantityQNT + " unconfirmedQuantity: " + unconfirmedQuantityQNT;
//        }
//
//    }
//
//    @SuppressWarnings("UnusedDeclaration")
//    public static final class AccountCurrency {
//
//        private final long accountId;
//        private final long currencyId;
//        private final DbKey dbKey;
//        private long units;
//        private long unconfirmedUnits;
//
//        private AccountCurrency(long accountId, long currencyId, long quantityQNT, long unconfirmedQuantityQNT) {
//            this.accountId = accountId;
//            this.currencyId = currencyId;
//            this.dbKey = accountCurrencyDbKeyFactory.newKey(this.accountId, this.currencyId);
//            this.units = quantityQNT;
//            this.unconfirmedUnits = unconfirmedQuantityQNT;
//        }
//
//        private AccountCurrency(ResultSet rs) throws SQLException {
//            this.accountId = rs.getLong("account_id");
//            this.currencyId = rs.getLong("currency_id");
//            this.dbKey = accountAssetDbKeyFactory.newKey(this.accountId, this.currencyId);
//            this.units = rs.getLong("units");
//            this.unconfirmedUnits = rs.getLong("unconfirmed_units");
//        }
//
//        private void save(Connection con) throws SQLException {
//            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_currency "
//                    + "(account_id, currency_id, units, unconfirmed_units, height, latest) "
//                    + "KEY (account_id, currency_id, height) VALUES (?, ?, ?, ?, ?, TRUE)")) {
//                int i = 0;
//                pstmt.setLong(++i, this.accountId);
//                pstmt.setLong(++i, this.currencyId);
//                pstmt.setLong(++i, this.units);
//                pstmt.setLong(++i, this.unconfirmedUnits);
//                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
//                pstmt.executeUpdate();
//            }
//        }
//
//        public long getAccountId() {
//            return accountId;
//        }
//
//        public long getCurrencyId() {
//            return currencyId;
//        }
//
//        public long getUnits() {
//            return units;
//        }
//
//        public long getUnconfirmedUnits() {
//            return unconfirmedUnits;
//        }
//
//        private void save() {
//            checkBalance(this.accountId, this.units, this.unconfirmedUnits);
//            if (this.units > 0 || this.unconfirmedUnits > 0) {
//                accountCurrencyTable.insert(this);
//            } else if (this.units == 0 && this.unconfirmedUnits == 0) {
//                accountCurrencyTable.delete(this);
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "AccountCurrency account_id: " + Long.toUnsignedString(accountId) + " currency_id: " + Long.toUnsignedString(currencyId)
//                    + " quantity: " + units + " unconfirmedQuantity: " + unconfirmedUnits;
//        }
//
//    }
//
//    public static final class AccountLease {
//
//        private final long lessorId;
//        private final DbKey dbKey;
//        private long currentLesseeId;
//        private int currentLeasingHeightFrom;
//        private int currentLeasingHeightTo;
//        private long nextLesseeId;
//        private int nextLeasingHeightFrom;
//        private int nextLeasingHeightTo;
//
//        private AccountLease(long lessorId,
//                             int currentLeasingHeightFrom, int currentLeasingHeightTo, long currentLesseeId) {
//            this.lessorId = lessorId;
//            this.dbKey = accountLeaseDbKeyFactory.newKey(this.lessorId);
//            this.currentLeasingHeightFrom = currentLeasingHeightFrom;
//            this.currentLeasingHeightTo = currentLeasingHeightTo;
//            this.currentLesseeId = currentLesseeId;
//        }
//
//        private AccountLease(ResultSet rs) throws SQLException {
//            this.lessorId = rs.getLong("lessor_id");
//            this.dbKey = accountLeaseDbKeyFactory.newKey(this.lessorId);
//            this.currentLeasingHeightFrom = rs.getInt("current_leasing_height_from");
//            this.currentLeasingHeightTo = rs.getInt("current_leasing_height_to");
//            this.currentLesseeId = rs.getLong("current_lessee_id");
//            this.nextLeasingHeightFrom = rs.getInt("next_leasing_height_from");
//            this.nextLeasingHeightTo = rs.getInt("next_leasing_height_to");
//            this.nextLesseeId = rs.getLong("next_lessee_id");
//        }
//
//        private void save(Connection con) throws SQLException {
//            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_lease "
//                    + "(lessor_id, current_leasing_height_from, current_leasing_height_to, current_lessee_id, "
//                    + "next_leasing_height_from, next_leasing_height_to, next_lessee_id, height, latest) "
//                    + "KEY (lessor_id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
//                int i = 0;
//                pstmt.setLong(++i, this.lessorId);
//                DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightFrom);
//                DbUtils.setIntZeroToNull(pstmt, ++i, this.currentLeasingHeightTo);
//                DbUtils.setLongZeroToNull(pstmt, ++i, this.currentLesseeId);
//                DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightFrom);
//                DbUtils.setIntZeroToNull(pstmt, ++i, this.nextLeasingHeightTo);
//                DbUtils.setLongZeroToNull(pstmt, ++i, this.nextLesseeId);
//                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
//                pstmt.executeUpdate();
//            }
//        }
//
//        public long getLessorId() {
//            return lessorId;
//        }
//
//        public long getCurrentLesseeId() {
//            return currentLesseeId;
//        }
//
//        public int getCurrentLeasingHeightFrom() {
//            return currentLeasingHeightFrom;
//        }
//
//        public int getCurrentLeasingHeightTo() {
//            return currentLeasingHeightTo;
//        }
//
//        public long getNextLesseeId() {
//            return nextLesseeId;
//        }
//
//        public int getNextLeasingHeightFrom() {
//            return nextLeasingHeightFrom;
//        }
//
//        public int getNextLeasingHeightTo() {
//            return nextLeasingHeightTo;
//        }
//
//    }
//
//    public static final class AccountInfo {
//
//        private final long accountId;
//        private final DbKey dbKey;
//        private String name;
//        private String description;
//
//        private AccountInfo(long accountId, String name, String description) {
//            this.accountId = accountId;
//            this.dbKey = accountInfoDbKeyFactory.newKey(this.accountId);
//            this.name = name;
//            this.description = description;
//        }
//
//        private AccountInfo(ResultSet rs) throws SQLException {
//            this.accountId = rs.getLong("account_id");
//            this.dbKey = accountInfoDbKeyFactory.newKey(this.accountId);
//            this.name = rs.getString("name");
//            this.description = rs.getString("description");
//        }
//
//        private void save(Connection con) throws SQLException {
//            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_info "
//                    + "(account_id, name, description, height, latest) "
//                    + "KEY (account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
//                int i = 0;
//                pstmt.setLong(++i, this.accountId);
//                DbUtils.setString(pstmt, ++i, this.name);
//                DbUtils.setString(pstmt, ++i, this.description);
//                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
//                pstmt.executeUpdate();
//            }
//        }
//
//        public long getAccountId() {
//            return accountId;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public String getDescription() {
//            return description;
//        }
//
//        private void save() {
//            if (this.name != null || this.description != null) {
//                accountInfoTable.insert(this);
//            } else {
//                accountInfoTable.delete(this);
//            }
//        }
//
//    }
//
//    static class DoubleSpendingException extends RuntimeException {
//
//        DoubleSpendingException(String message, long accountId, long confirmed, long unconfirmed) {
//            super(message + " account: " + Long.toUnsignedString(accountId) + " confirmed: " + confirmed + " unconfirmed: " + unconfirmed);
//        }
//
//    }
//
//    private static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {
//
//        @Override
//        public DbKey newKey(Account account) {
//            return account.dbKey;
//        }
//
//    };
//
//    private static final VersionedEntityDbTable<Account> accountTable = new VersionedEntityDbTable<Account>("account", accountDbKeyFactory) {
//
//        @Override
//        protected Account load(Connection con, ResultSet rs) throws SQLException {
//            return new Account(rs);
//        }
//
//        @Override
//        protected void save(Connection con, Account account) throws SQLException {
//            account.save(con);
//        }
//
//    };
//
//    private static final DbKey.LongKeyFactory<AccountInfo> accountInfoDbKeyFactory = new DbKey.LongKeyFactory<AccountInfo>("account_id") {
//
//        @Override
//        public DbKey newKey(AccountInfo accountInfo) {
//            return accountInfo.dbKey;
//        }
//
//    };
//
//    private static final DbKey.LongKeyFactory<AccountLease> accountLeaseDbKeyFactory = new DbKey.LongKeyFactory<AccountLease>("lessor_id") {
//
//        @Override
//        public DbKey newKey(AccountLease accountLease) {
//            return accountLease.dbKey;
//        }
//
//    };
//
//    private static final VersionedEntityDbTable<AccountLease> accountLeaseTable = new VersionedEntityDbTable<AccountLease>("account_lease",
//            accountLeaseDbKeyFactory) {
//
//        @Override
//        protected AccountLease load(Connection con, ResultSet rs) throws SQLException {
//            return new AccountLease(rs);
//        }
//
//        @Override
//        protected void save(Connection con, AccountLease accountLease) throws SQLException {
//            accountLease.save(con);
//        }
//
//    };
//
//    private static final VersionedEntityDbTable<AccountInfo> accountInfoTable = new VersionedEntityDbTable<AccountInfo>("account_info",
//            accountInfoDbKeyFactory, "name,description") {
//
//        @Override
//        protected AccountInfo load(Connection con, ResultSet rs) throws SQLException {
//            return new AccountInfo(rs);
//        }
//
//        @Override
//        protected void save(Connection con, AccountInfo accountInfo) throws SQLException {
//            accountInfo.save(con);
//        }
//
//    };
//
//    private static final DbKey.LongKeyFactory<byte[]> publicKeyDbKeyFactory = new DbKey.LongKeyFactory<byte[]>("account_id") {
//
//        @Override
//        public DbKey newKey(byte[] publicKey) {
//            return newKey(Account.getId(publicKey));
//        }
//
//    };
//
//    private static final PersistentDbTable<byte[]> publicKeyTable = new PersistentDbTable<byte[]>("public_key", publicKeyDbKeyFactory) {
//
//        @Override
//        protected byte[] load(Connection con, ResultSet rs) throws SQLException {
//            return rs.getBytes("public_key");
//        }
//
//        @Override
//        protected void save(Connection con, byte[] publicKey) throws SQLException {
//            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO public_key (account_id, public_key, height) "
//                    + "KEY (account_id) VALUES (?, ?, ?)")) {
//                int i = 0;
//                pstmt.setLong(++i, Account.getId(publicKey));
//                pstmt.setBytes(++i, publicKey);
//                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
//                pstmt.executeUpdate();
//            }
//        }
//
//    };
//
//    private static final DbKey.LinkKeyFactory<AccountAsset> accountAssetDbKeyFactory = new DbKey.LinkKeyFactory<AccountAsset>("account_id", "asset_id") {
//
//        @Override
//        public DbKey newKey(AccountAsset accountAsset) {
//            return accountAsset.dbKey;
//        }
//
//    };
//
//    private static final VersionedEntityDbTable<AccountAsset> accountAssetTable = new VersionedEntityDbTable<AccountAsset>("account_asset", accountAssetDbKeyFactory) {
//
//        @Override
//        protected AccountAsset load(Connection con, ResultSet rs) throws SQLException {
//            return new AccountAsset(rs);
//        }
//
//        @Override
//        protected void save(Connection con, AccountAsset accountAsset) throws SQLException {
//            accountAsset.save(con);
//        }
//
//        @Override
//        public void trim(int height) {
//            super.trim(Math.max(0, height - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK));
//        }
//
//        @Override
//        public void checkAvailable(int height) {
//            if (height + Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK < Nxt.getBlockchainProcessor().getMinRollbackHeight()) {
//                throw new IllegalArgumentException("Historical data as of height " + height +" not available.");
//            }
//            if (height > Nxt.getBlockchain().getHeight()) {
//                throw new IllegalArgumentException("Height " + height + " exceeds blockchain height " + Nxt.getBlockchain().getHeight());
//            }
//        }
//
//        @Override
//        protected String defaultSort() {
//            return " ORDER BY quantity DESC, account_id, asset_id ";
//        }
//
//    };
//
//    private static final DbKey.LinkKeyFactory<AccountCurrency> accountCurrencyDbKeyFactory = new DbKey.LinkKeyFactory<AccountCurrency>("account_id", "currency_id") {
//
//        @Override
//        public DbKey newKey(AccountCurrency accountCurrency) {
//            return accountCurrency.dbKey;
//        }
//
//    };
//
//    private static final VersionedEntityDbTable<AccountCurrency> accountCurrencyTable = new VersionedEntityDbTable<AccountCurrency>("account_currency", accountCurrencyDbKeyFactory) {
//
//        @Override
//        protected AccountCurrency load(Connection con, ResultSet rs) throws SQLException {
//            return new AccountCurrency(rs);
//        }
//
//        @Override
//        protected void save(Connection con, AccountCurrency accountCurrency) throws SQLException {
//            accountCurrency.save(con);
//        }
//
//        @Override
//        protected String defaultSort() {
//            return " ORDER BY units DESC, account_id, currency_id ";
//        }
//
//    };
//
//    private static final DerivedDbTable accountGuaranteedBalanceTable = new DerivedDbTable("account_guaranteed_balance") {
//
//        @Override
//        public void trim(int height) {
//            try (Connection con = Db.db.getConnection();
//                 PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM account_guaranteed_balance "
//                         + "WHERE height < ? AND height >= 0")) {
//                pstmtDelete.setInt(1, height - Constants.GUARANTEED_BALANCE_CONFIRMATIONS);
//                pstmtDelete.executeUpdate();
//            } catch (SQLException e) {
//                throw new RuntimeException(e.toString(), e);
//            }
//        }
//
//    };
//
//    private static final Listeners<Account,Event> listeners = new Listeners<>();
//
//    private static final Listeners<AccountAsset,Event> assetListeners = new Listeners<>();
//
//    private static final Listeners<AccountCurrency,Event> currencyListeners = new Listeners<>();
//
//    private static final Listeners<AccountLease,Event> leaseListeners = new Listeners<>();
//
//    public static boolean addListener(Listener<Account> listener, Event eventType) {
//        return listeners.addListener(listener, eventType);
//    }
//
//    public static boolean removeListener(Listener<Account> listener, Event eventType) {
//        return listeners.removeListener(listener, eventType);
//    }
//
//    public static boolean addAssetListener(Listener<AccountAsset> listener, Event eventType) {
//        return assetListeners.addListener(listener, eventType);
//    }
//
//    public static boolean removeAssetListener(Listener<AccountAsset> listener, Event eventType) {
//        return assetListeners.removeListener(listener, eventType);
//    }
//
//    public static boolean addCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
//        return currencyListeners.addListener(listener, eventType);
//    }
//
//    public static boolean removeCurrencyListener(Listener<AccountCurrency> listener, Event eventType) {
//        return currencyListeners.removeListener(listener, eventType);
//    }
//
//    public static boolean addLeaseListener(Listener<AccountLease> listener, Event eventType) {
//        return leaseListeners.addListener(listener, eventType);
//    }
//
//    public static boolean removeLeaseListener(Listener<AccountLease> listener, Event eventType) {
//        return leaseListeners.removeListener(listener, eventType);
//    }
//
//    public static DbIterator<Account> getAllAccounts(int from, int to) {
//        return accountTable.getAll(from, to);
//    }
//
//    public static int getCount() {
//        return accountTable.getCount();
//    }
//
//    public static int getAssetAccountCount(long assetId) {
//        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId));
//    }
//
//    public static int getAssetAccountCount(long assetId, int height) {
//        return accountAssetTable.getCount(new DbClause.LongClause("asset_id", assetId), height);
//    }
//
//    public static int getAccountAssetCount(long accountId) {
//        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId));
//    }
//
//    public static int getAccountAssetCount(long accountId, int height) {
//        return accountAssetTable.getCount(new DbClause.LongClause("account_id", accountId), height);
//    }
//
//    public static int getCurrencyAccountCount(long currencyId) {
//        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId));
//    }
//
//    public static int getCurrencyAccountCount(long currencyId, int height) {
//        return accountCurrencyTable.getCount(new DbClause.LongClause("currency_id", currencyId), height);
//    }
//
//    public static int getAccountCurrencyCount(long accountId) {
//        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId));
//    }
//
//    public static int getAccountCurrencyCount(long accountId, int height) {
//        return accountCurrencyTable.getCount(new DbClause.LongClause("account_id", accountId), height);
//    }
//
//    public static int getAccountLeaseCount() {
//        return accountLeaseTable.getCount();
//    }
//
//    public static int getActiveLeaseCount() {
//        return accountTable.getCount(new DbClause.FixedClause("active_lessee_id IS NOT NULL"));
//    }
//
//    public static Account getAccount(long id) {
//        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id));
//    }
//
//    public static Account getAccount(long id, int height) {
//        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id), height);
//    }
//
//    public static Account getAccount(byte[] publicKey) {
//        Account account = accountTable.get(accountDbKeyFactory.newKey(getId(publicKey)));
//        if (account == null) {
//            return null;
//        }
//        if (account.getPublicKey() == null || Arrays.equals(account.getPublicKey(), publicKey)) {
//            return account;
//        }
//        throw new RuntimeException("DUPLICATE KEY for account " + Long.toUnsignedString(account.getId())
//                + " existing key " + com.coinomi.core.coins.nxt.Convert.toHexString(account.getPublicKey()) + " new key " + com.coinomi.core.coins.nxt.Convert.toHexString(publicKey));
//    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

//    public static byte[] getPublicKey(long id) {
//        return publicKeyTable.get(publicKeyDbKeyFactory.newKey(id));
//    }

//    static Account addOrGetAccount(long id) {
//        if (id == 0) {
//            throw new IllegalArgumentException("Invalid accountId 0");
//        }
//        Account account = accountTable.get(accountDbKeyFactory.newKey(id));
//        if (account == null) {
//            account = new Account(id);
//            accountTable.insert(account);
//        }
//        return account;
//    }
//
//    private static DbIterator<AccountLease> getLeaseChangingAccounts(final int height) {
//        Connection con = null;
//        try {
//            con = Db.db.getConnection();
//            PreparedStatement pstmt = con.prepareStatement(
//                    "SELECT * FROM account_lease WHERE current_leasing_height_from = ? AND latest = TRUE "
//                            + "UNION ALL SELECT * FROM account_lease WHERE current_leasing_height_to = ? AND latest = TRUE "
//                            + "ORDER BY current_lessee_id, lessor_id");
//            int i = 0;
//            pstmt.setInt(++i, height);
//            pstmt.setInt(++i, height);
//            return accountLeaseTable.getManyBy(con, pstmt, true);
//        } catch (SQLException e) {
//            DbUtils.close(con);
//            throw new RuntimeException(e.toString(), e);
//        }
//    }
//
//    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int from, int to) {
//        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to, " ORDER BY quantity DESC, account_id ");
//    }
//
//    public static DbIterator<AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
//        return accountAssetTable.getManyBy(new DbClause.LongClause("asset_id", assetId), height, from, to, " ORDER BY quantity DESC, account_id ");
//    }
//
//    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int from, int to) {
//        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), from, to);
//    }
//
//    public static DbIterator<AccountCurrency> getCurrencyAccounts(long currencyId, int height, int from, int to) {
//        return accountCurrencyTable.getManyBy(new DbClause.LongClause("currency_id", currencyId), height, from, to);
//    }
//
//    public static long getAssetBalanceQNT(long accountId, long assetId, int height) {
//        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId), height);
//        return accountAsset == null ? 0 : accountAsset.quantityQNT;
//    }
//
//    public static long getAssetBalanceQNT(long accountId, long assetId) {
//        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
//        return accountAsset == null ? 0 : accountAsset.quantityQNT;
//    }
//
//    public static long getUnconfirmedAssetBalanceQNT(long accountId, long assetId) {
//        AccountAsset accountAsset = accountAssetTable.get(accountAssetDbKeyFactory.newKey(accountId, assetId));
//        return accountAsset == null ? 0 : accountAsset.unconfirmedQuantityQNT;
//    }
//
//    public static long getCurrencyUnits(long accountId, long currencyId, int height) {
//        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId), height);
//        return accountCurrency == null ? 0 : accountCurrency.units;
//    }
//
//    public static long getCurrencyUnits(long accountId, long currencyId) {
//        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
//        return accountCurrency == null ? 0 : accountCurrency.units;
//    }
//
//    public static long getUnconfirmedCurrencyUnits(long accountId, long currencyId) {
//        AccountCurrency accountCurrency = accountCurrencyTable.get(accountCurrencyDbKeyFactory.newKey(accountId, currencyId));
//        return accountCurrency == null ? 0 : accountCurrency.unconfirmedUnits;
//    }
//
//    public static DbIterator<AccountInfo> searchAccounts(String query, int from, int to) {
//        return accountInfoTable.search(query, DbClause.EMPTY_CLAUSE, from, to);
//    }
//
//    static {
//
//        Nxt.getBlockchainProcessor().addListener(block -> {
//            int height = block.getHeight();
//            if (height < Constants.TRANSPARENT_FORGING_BLOCK_6) {
//                return;
//            }
//            List<AccountLease> changingLeases = new ArrayList<>();
//            try (DbIterator<AccountLease> leases = getLeaseChangingAccounts(height)) {
//                while (leases.hasNext()) {
//                    changingLeases.add(leases.next());
//                }
//            }
//            for (AccountLease lease : changingLeases) {
//                Account lessor = accountTable.get(accountDbKeyFactory.newKey(lease.lessorId));
//                if (height == lease.currentLeasingHeightFrom) {
//                    lessor.activeLesseeId = lease.currentLesseeId;
//                    leaseListeners.notify(lease, Event.LEASE_STARTED);
//                } else if (height == lease.currentLeasingHeightTo) {
//                    leaseListeners.notify(lease, Event.LEASE_ENDED);
//                    lessor.activeLesseeId = 0;
//                    if (lease.nextLeasingHeightFrom == 0) {
//                        lease.currentLeasingHeightFrom = 0;
//                        lease.currentLeasingHeightTo = 0;
//                        lease.currentLesseeId = 0;
//                        accountLeaseTable.delete(lease);
//                    } else {
//                        lease.currentLeasingHeightFrom = lease.nextLeasingHeightFrom;
//                        lease.currentLeasingHeightTo = lease.nextLeasingHeightTo;
//                        lease.currentLesseeId = lease.nextLesseeId;
//                        lease.nextLeasingHeightFrom = 0;
//                        lease.nextLeasingHeightTo = 0;
//                        lease.nextLesseeId = 0;
//                        accountLeaseTable.insert(lease);
//                        if (height == lease.currentLeasingHeightFrom) {
//                            lessor.activeLesseeId = lease.currentLesseeId;
//                            leaseListeners.notify(lease, Event.LEASE_STARTED);
//                        }
//                    }
//                }
//                accountTable.insert(lessor);
//            }
//        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
//
//    }
//
//    static void init() {}
//
//
//    private final long id;
//    private final DbKey dbKey;
//    private final int creationHeight;
//    private volatile byte[] publicKey;
//    private int keyHeight;
//    private long balanceNQT;
//    private long unconfirmedBalanceNQT;
//    private long forgedBalanceNQT;
//    private long activeLesseeId;
//
//    private Account(long id) {
//        if (id != com.coinomi.core.coins.nxt.Crypto.rsDecode(com.coinomi.core.coins.nxt.Crypto.rsEncode(id))) {
//            Logger.logMessage("CRITICAL ERROR: Reed-Solomon encoding fails for " + id);
//        }
//        this.id = id;
//        this.dbKey = accountDbKeyFactory.newKey(this.id);
//        this.creationHeight = Nxt.getBlockchain().getHeight();
//    }
//
//    private Account(ResultSet rs) throws SQLException {
//        this.id = rs.getLong("id");
//        this.dbKey = accountDbKeyFactory.newKey(this.id);
//        this.creationHeight = rs.getInt("creation_height");
//        this.keyHeight = rs.getInt("key_height");
//        this.balanceNQT = rs.getLong("balance");
//        this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
//        this.forgedBalanceNQT = rs.getLong("forged_balance");
//        this.activeLesseeId = rs.getLong("active_lessee_id");
//    }
//
//    private void save(Connection con) throws SQLException {
//        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account (id, creation_height, "
//                + "key_height, balance, unconfirmed_balance, forged_balance, "
//                + "active_lessee_id, height, latest) "
//                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
//            int i = 0;
//            pstmt.setLong(++i, this.id);
//            pstmt.setInt(++i, this.creationHeight);
//            pstmt.setInt(++i, this.keyHeight);
//            pstmt.setLong(++i, this.balanceNQT);
//            pstmt.setLong(++i, this.unconfirmedBalanceNQT);
//            pstmt.setLong(++i, this.forgedBalanceNQT);
//            DbUtils.setLongZeroToNull(pstmt, ++i, this.activeLesseeId);
//            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
//            pstmt.executeUpdate();
//        }
//    }
//
//    public long getId() {
//        return id;
//    }
//
//    public AccountInfo getAccountInfo() {
//        return accountInfoTable.get(accountInfoDbKeyFactory.newKey(this.id));
//    }
//
//    void setAccountInfo(String name, String description) {
//        name = com.coinomi.core.coins.nxt.Convert.emptyToNull(name.trim());
//        description = com.coinomi.core.coins.nxt.Convert.emptyToNull(description.trim());
//        AccountInfo accountInfo = getAccountInfo();
//        if (accountInfo == null) {
//            accountInfo = new AccountInfo(id, name, description);
//        } else {
//            accountInfo.name = name;
//            accountInfo.description = description;
//        }
//        accountInfo.save();
//    }
//
//    public AccountLease getAccountLease() {
//        return accountLeaseTable.get(accountLeaseDbKeyFactory.newKey(this.id));
//    }
//
//    public byte[] getPublicKey() {
//        if (this.publicKey == null) {
//            this.publicKey = publicKeyTable.get(publicKeyDbKeyFactory.newKey(this.id));
//        }