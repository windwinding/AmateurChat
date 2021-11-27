package com.coinomi.core.wallet.families.bitcoin;

import com.coinomi.core.coins.BitcoinMain;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.LitecoinMain;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.wallet.AbstractAddress;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author John L. Jegutanis
 */
public class BitAddressTest {
    CoinType BTC = BitcoinMain.get();
    CoinType LTC = LitecoinMain.get();

    ECKey pubKey = ECKey.fromPublicOnly(Hex.decode("037a305e15ddef015bb64d147eb479d64c9f3c85d9bf6f3f8c39252e29e11c0db8"));
    final static byte[] HASH160 = Hex.decode("9d00cbda32e56ef87058e8dacfd20f3e297fc84c");
    final static byte[] P2PKH_SCRIPT = Hex.decode("76a9149d00cbda32e56ef87058e8dacfd20f3e297fc84c88ac");
    final static byte[] P2SH_SCRIPT = Hex.decode("a9149d00cbda32e56ef87058e8dacfd20f3e297fc84c87");
    final static String BTC_P2PKH_ADDR = "1FKA3SpU5rkCJhza3d4mGjs7unVy3jgH9T";
    final static String BTC_P2SH_ADDR = "3G1AxzJudm4aPsh1AijMhNE44Jngf2oCSe";
    final static String LTC_P2PKH_ADDR = "LZY7Jf8JAWzFZWgjDm44Ykvt7zsF9YHFyW";
    final static String LTC_P2SH_ADDR = "3G1AxzJudm4aPsh1AijMhNE44Jngf2oCSe";

    @Test
    public void testConstructors() throws AddressFormatException {
    