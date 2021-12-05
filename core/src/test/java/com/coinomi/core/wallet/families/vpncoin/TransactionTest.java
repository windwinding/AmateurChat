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
    static long ENCRYPTION_KEY = 123;
    static byte[] COMPRESSED = Base64.decode("AAAABXicy0jNyckHAAYsAhU=");

    static String FROM_USER = "user@domain.com";
    static String SUBJECT = "A 'lorem ipsum' message for you";
    static String MESSAGE = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.\n" +
            "Donec a diam lectus. Sed sit amet ipsum mauris.\n" +
            "Maecenas congue ligula ac quam viverra nec consectetur ante hendrerit.\n" +
            "Donec et mollis dolor. Praesent et diam eget libero egestas mattis sit amet vitae augue.";

    String TX_HASH_1 = "192c485deab867c994a2396c815a48e4c3808c166b4edd70f87085e22c1417e5";
    Transaction TX_1 = new Transaction(VpncoinMain.get(), Hex.decode("0100000089cec45502c8f83c02c59583876c6e1fa447e7ab6083e392753ddf8707269b36340afcf028000000006a47304402206552a703cc1865c72bc43899d4841502f52f5110d2cb637983965e153abe197802200b31b94c1d014fc0ea360f5d29a79ab98421f6104145477a7077540393aeb0bd0121028cbb0a3d32a4e076c5131c90b49ddcb940eaa67cf918a1484ca69d19d475a579ffffffff35908ed4a3aa92f2c0b19605a07fbb9a76146db542cc30b9d5e75d2259ac8735010000006a473044022053a2f59dc313e43c96b01b3787bfab948aa1b95a40a1b9691eff01545d924d9e02204845df419347bff68207febe7fe05bed41daa5164d7f26ec170cac7455b0e4d601210282a4a113c70c352ee86d2a60297a7eeb719664f8610514d442346d7999ffa3b6ffffffff0220b81800000000001976a914a620a48d22391100afceaff72c73a4032f3fb02b88ac00e40b54020000001976a914ce7f924d1886c3525e8454eebda251a0f0d1b10b88ac00000000384177494d6465373776652b6737566e5665466c354e5641312f47487735363251324c31592b6c464543566f64495031622f73577533664d3d"));
    String TX_HASH_2 = "9925c0724b5ce11e13c9cb1349cb084377ab6e3245112eec109d54d3bcf38b34";
    Transaction TX_2 = new Transaction(VpncoinMain.get(), Hex.decode("01000000347bc75501f4fddc83d4a27cb80e73547503e23f3f9336effa180aed06cf60c65f6625059c010000006b483045022100be8e81194d4e77ca53cf472973f065f04709a8fe7b429955b829b08c69ac6213022040db08bb6f8a9d8a78157752c1f534239d9d339546423d5cfd1462fda5408b9101210351548af711794d4e556d2c215bd25f9d2a4d7ec5c99d5ca42d4c33fb21307835ffffffff02f09eb31e030000001976a914a620a48d22391100afceaff72c73a4032f3fb02b88ac00e40b54020000001976a914ce7f924d1886c3525e8454eebda251a0f0d1b10b88ac000000006c4177493672364f32384b4c746f4b6b30584a313638307173426642336353526d4c424842425750515566362b3835536f55745a58687a6e705938692b4f724e6a316761504a46766561324776452b6b384e6430536c316e6c483955596c535438543563457075566f77513d3d"));
    String TX_HASH_3 = "51f0ce3a8c56078d2d05660a042b820dffd2c81f370ab5af0d1135477461348a";
    Transaction TX_3 = new Transaction(VpncoinMain.get(), Hex.decode("01000000f07ac75501b14681fad878ae6d2416816be38bc926a5d428c7140a43e8dc4a64b61f324306010000006b483045022100f7beded6ce2b642af5523e1141032c68bc76fed629080766891b7aa54a4831b802200ad9a30203b93a3661f8132e5f132d865328874e709fe14b953c2ab72e20c7d50121025e3c4bfdc18ea84ec3403360c4b5f01b71fc4cdca5e170e508b05a121ffd5230ffffffff02f00f1789000000001976a914a620a48d22391100afceaff72c73a4032f3fb02b88ac00e40b54020000001976a914ce7f924d1886c3525e8454eebda251a0f0d1b10b88ac00000000fd900241774d4b5161667938764532546d53443139395669636e467357514256