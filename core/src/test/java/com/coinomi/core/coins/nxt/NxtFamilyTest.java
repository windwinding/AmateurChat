
package com.coinomi.core.coins.nxt;

/**
 * @author John L. Jegutanis
 */

import com.coinomi.core.coins.NxtMain;
import com.coinomi.core.coins.nxt.Appendix.EncryptedMessage;
import com.coinomi.core.wallet.families.nxt.NxtAddress;
import com.coinomi.core.wallet.families.nxt.NxtFamilyKey;

import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NxtFamilyTest {
    String recoveryPhrase = "heavy virus hollow shrug shadow double dwarf affair novel weird image prize frame anxiety wait";
    byte[] nxtPrivateKey = Hex.decode("200a8ead018adb6c78f2c821500ad13f5f24d101ed8431adcfb315ca58468553");
    byte[] nxtPublicKey = Hex.decode("163c6583ed489414f27e73a74f72080b478a55dfce4a086ded2990976e8bb81e");
    String nxtRsAddress = "NXT-CGNQ-8WBM-3P2F-AVH9J";