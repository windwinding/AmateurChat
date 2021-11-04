package com.coinomi.core.wallet;

import com.coinomi.core.coins.BitcoinMain;
import com.coinomi.core.coins.BitcoinTest;
import com.coinomi.core.protos.Protos;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.AbstractKeyChainEventListener;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author John L. Jegutanis
 */
public class SimpleHDKeyChainTest {
    SimpleHDKeyChain chain;
    DeterministicKey masterKey;
    byte[] ENTROPY = Sha256Hash.create("don't use a string seed like this in real life".getBytes()).getBytes();

    @Before
    public void setup() {
        BriefLogFormatter.init();

        DeterministicSeed seed = new DeterministicSeed(ENTROPY, "", 0);
        masterKey = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);
        DeterministicKey rootKey = hierarchy.get(ImmutableList.of(ChildNumber.ZERO_HARDENED), false, true);
        chain = new SimpleHDKeyChain(rootKey);
        chain.setLookaheadSize(10);
    }

    @Test
    public void derive() throws Exception {
        ECKey key1 = chain.getKey(SimpleHDKeyChain.KeyPurpose.RECEIVE_FUNDS);
        ECKey key2 = chain.getKey(SimpleHDKeyChain.KeyPurpose.RECEIVE_FUNDS);

        final Address address = new Address(UnitTestParams.get(), "n1bQNoEx8uhmCzzA5JPG6sFdtsUQhwiQJV");
        assertEquals(address, key1.toAddress(UnitTestParams.get()));
        assertEquals("mnHUcqUVvrfi5kAaXJDQzBb9HsWs78b42R", key2.toAddress(UnitTestParams.get()).toString());
        assertEquals(key1, chain.findKeyFromPubHash(address.getHash160()));
        assertEquals(key2, chain.findKeyFromPubKey(key2.getPubKey()));

        key1.sign(Sha256Hash.ZERO_HASH);

        ECKey key3 = chain.getKey(SimpleHDKeyChain.KeyPurpose.CHANGE);
        assertEquals("mqumHgVDqNzuXNrszBmi7A2UpmwaPMx4HQ", key3.toAddress(UnitTestParams.get()).toString());
        key3.sign(Sha256Hash.ZERO_HASH);
    }

    @Test
    public void deriveCoin() throws Exception {
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);
        DeterministicKey rootKey = hierarchy.get(BitcoinMain.get().getBip44Path(0), false, true);
        chain = new SimpleHDKeyChain(rootKey);

        ECKey key1 = chain.getKey(SimpleHDKeyChain.KeyPurpose.RECEIVE_FUNDS);
        ECKey key2 = chain.getKey(SimpleHDKeyChain.KeyPurpose.RECEIVE_FUNDS);

        final Address address = new Address(BitcoinMain.get(), "1Fp7CA7ZVqZNFVNQ9TpeqWUas7K28K9zig");
        assertEquals(address, key1.toAddress(BitcoinMain.get()));
        assertEquals("1AKqkQM4VqyVis6hscj8695WHPCCzgHNY3", key2.toAddress(BitcoinMain.get()).toString());
        assertEquals(key1, chain.findKeyFromPubHash(address.getHash160()));
        assertEquals(key2, chain.findKeyFromPubKey(key2.getPubKey()));

        key1.sign(Sha256Hash.ZERO_HASH);

        ECKey key3 = chain.getKey(SimpleHDKeyChain.KeyPurpose.CHANGE);
        assertEquals("18YvGiRqXKxrzB72ckfrRSizWeHgwRP94V", key3.toAddress(BitcoinMain.get()).toString());
        key3.sign(Sha256Hash.ZERO_HASH);

        ECKey key4 = chain.getKey(SimpleHDKeyChain.KeyPurpose.CHANGE);
        assertEquals("1861TX2MbyPEUrxDQVWgV4Tp9991bK1zpy", key4.toAddress(BitcoinMain.get()).toString());
        key4.sign(Sha256Hash.ZERO_HASH);
    }

    @Test
    public void getLastIssuedKey() {
        assertNull(chain.getLastIssuedKey(KeyChain.KeyPurpose.RECEIVE_FUNDS));
        assertNull(chain.getLastIssuedKey(KeyChain.KeyPurpose.CHANGE));
        DeterministicKey extKey = chain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        DeterministicKey intKey = chain.getKey(KeyChain.KeyPurpose.CHANGE);
        assertEquals(extKey, chain.getLastIssuedKey(KeyChain.KeyPurpose.RECEIVE_FUNDS));
        assertEquals(intKey, chain.getLastIssuedKey(KeyChain.KeyPurpose.CHANGE));
    }

    @Test
    public void externalKeyCheck() {
        assertFalse(chain.isExternal(chain.getKey(KeyChain.KeyPurpose.CHANGE)));
        assertTrue(chain.isExternal(chain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS)));
    }

    @Test
    public void events() throws Exception {
        // Check that we get the right events at the right time.
        final List<List<ECKey>> listenerKeys = Lists.newArrayList();
        long secs = 1389353062L;
        chain.addEventListener(new AbstractKeyChainEventListener() {
            @Override
            public void onKeysAdded(List<ECKey> keys) {
                listenerKeys.add(keys);
            }
        }, Threading.SAME_THREAD);
        assertEquals(0, listenerKeys.size());
        chain.setLookaheadSize(5);
        assertEquals(0, listenerKeys.size());
        ECKey key = chain.getKey(SimpleHDKeyChain.KeyPurpose.CHANGE);
        assertEquals(1, listenerKeys.size());  // 1 event
        final List<ECKey> firstEvent = listenerKeys.get(0);
        assertEquals(1, firstEvent.size());
        assertTrue(firstEvent.contains(key));   // order is not specified.
        listenerKeys.clear();

        chain.maybeLookAhead();
        final List<ECKey> secondEvent = listenerKeys.get(0);
        assertEquals(12, secondEvent.size());  // (5 lookahead keys, +1 lookahead threshold) * 2 chains
        listenerKeys.clear();

        chain.getKey(SimpleHDKeyChain.KeyPurpose.CHANGE);
        // At this point we've entered the threshold zone so more keys won't immediately trigger more generations.
        assertEquals(0, listenerKeys.size());  // 1 event
        final int lookaheadThreshold = chain.getLookaheadThreshold() + chain.getLookaheadSize();
        for (int i = 0; i < lookaheadThreshold; i++)
            chain.getKey(SimpleHDKeyChain.KeyPurpose.CHANGE);
        assertEquals(1, listenerKeys.size());  // 1 event
        assertEquals(1, listenerKeys.get(0).size());  // 1 key.
    }

    @Test
    public void serializeUnencryptedNormal() throws UnreadableWalletException {
        serializeUnencrypted(chain, DETERMINISTIC_WALLET_SERIALIZATION_TXT_MASTER_KEY);
    }

    @Test
    public void serializeUnencryptedChildRoot() throws UnreadableWalletException {
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);
        DeterministicKey rootKey = hierarchy.get(BitcoinTest.get().getBip44Path(0), false, true);
        SimpleHDKeyChain newChain = new SimpleHDKeyChain(rootKey);
        serializeUnencrypted(newChain, DETERMINISTIC_WALLET_SERIALIZATION_TXT_CHILD_ROOT_KEY);
    }


    public void serializeUnencrypted(SimpleHDKeyChain keyChain, String expectedSerialization) throws UnreadableWalletException {
        keyChain.setLookaheadSize(10);

        keyChain.maybeLookAhead();
        DeterministicKey key1 = keyChain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        DeterministicKey key2 = keyChain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        DeterministicKey key3 = keyChain.getKey(KeyChain.KeyPurpose.CHANGE);
        List<Protos.Key> keys = keyChain.toProtobuf();
        // 1 master key, 1 account key, 2 internal keys, 3 derived, 20 lookahead and 5 lookahead threshold.
        int numItems =
                          1  // master key/account key
                        + 2  // ext/int parent keys
                        + (keyChain.getLookaheadSize() + keyChain.getLookaheadThreshold()) * 2   // lookahead zone on each chain
                ;
        assertEquals(numItems, keys.size());

        // Get another key that will be lost during round-tripping, to ensure we can derive it again.
        DeterministicKey key4 = keyChain.getKey(KeyChain.KeyPurpose.CHANGE);

        String sb = protoToString(keys);
        assertEquals(expectedSerialization, sb);

        // Round trip the data back and forth to check it is preserved.
        int oldLookaheadSize = keyChain.getLookaheadSize();
        keyChain = SimpleHDKeyChain.fromProtobuf(keys, null);
        assertEquals(expectedSerialization, protoToString(keyChain.toProtobuf()));
        assertEquals(key1, keyChain.findKeyFromPubHash(key1.getPubKeyHash()));
        assertEquals(key2, keyChain.findKeyFromPubHash(key2.getPubKeyHash()));
        assertEquals(key3, keyChain.findKeyFromPubHash(key3.getPubKeyHash()));
        assertEquals(key4, keyChain.getKey(KeyChain.KeyPurpose.CHANGE));
        key1.sign(Sha256Hash.ZERO_HASH);
        key2.sign(Sha256Hash.ZERO_HASH);
        key3.sign(Sha256Hash.ZERO_HASH);
        key4.sign(Sha256Hash.ZERO_HASH);
        assertEquals(oldLookaheadSize, keyChain.getLookaheadSize());
    }

    private String protoToString(List<Protos.Key> keys) {
        StringBuilder sb = new StringBuilder();
        for (Protos.Key key : keys) {
            sb.append(key.toString());
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String checkSerialization(List<Protos.Key> keys, String filename) {
        try {
            String sb = protoToString(keys);
            String expected = Resources.toString(getClass().getResource(filename), Charsets.UTF_8);
            assertEquals(expected, sb);
            return expected;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void notEncrypted() {
        chain.toDecrypted("fail");
    }

    @Test(expected = IllegalStateException.class)
    public void encryptTwice() {
        chain = chain.toEncrypted("once");
        chain = chain.toEncrypted("twice");
    }

    private void checkEncryptedKeyChain(SimpleHDKeyChain encChain, DeterministicKey key1) {
        // Check we can look keys up and extend the chain without the AES key being provided.
        DeterministicKey encKey1 = encChain.findKeyFromPubKey(key1.getPubKey());
        DeterministicKey encKey2 = encChain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        assertFalse(key1.isEncrypted());
        assertTrue(encKey1.isEncrypted());
        assertEquals(encKey1.getPubKeyPoint(), key1.getPubKeyPoint());
        final KeyParameter aesKey = checkNotNull(encChain.getKeyCrypter()).deriveKey("open secret");
        encKey1.sign(Sha256Hash.ZERO_HASH, aesKey);
        encKey2.sign(Sha256Hash.ZERO_HASH, aesKey);
        assertTrue(encChain.checkAESKey(aesKey));
        assertFalse(encChain.checkPassword("access denied"));
        assertTrue(encChain.checkPassword("open secret"));
    }

    @Test
    public void encryptionNormal() throws UnreadableWalletException {
        encryption(chain);
    }

    @Test
    public void encryptionChildRoot() throws UnreadableWalletException {
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(masterKey);
        DeterministicKey rootKey = hierarchy.get(BitcoinTest.get().getBip44Path(0), false, true);
        SimpleHDKeyChain newChain = new SimpleHDKeyChain(rootKey);

        encryption(newChain);
    }

    public void encryption(SimpleHDKeyChain unencChain) throws UnreadableWalletException {
        DeterministicKey key1 = unencChain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        SimpleHDKeyChain encChain = unencChain.toEncrypted("open secret");
        DeterministicKey encKey1 = encChain.findKeyFromPubKey(key1.getPubKey());
        checkEncryptedKeyChain(encChain, key1);

        // Round-trip to ensure de/serialization works and that we can store two chains and they both deserialize.
        List<Protos.Key> serialized = encChain.toProtobuf();
        System.out.println(protoToString(serialized));
        encChain = SimpleHDKeyChain.fromProtobuf(serialized, encChain.getKeyCrypter());
        checkEncryptedKeyChain(encChain, unencChain.findKeyFromPubKey(key1.getPubKey()));

        DeterministicKey encKey2 = encChain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        // Decrypt and check the keys match.
        SimpleHDKeyChain decChain = encChain.toDecrypted("open secret");
        DeterministicKey decKey1 = decChain.findKeyFromPubHash(encKey1.getPubKeyHash());
        DeterministicKey decKey2 = decChain.findKeyFromPubHash(encKey2.getPubKeyHash());
        assertEquals(decKey1.getPubKeyPoint(), encKey1.getPubKeyPoint());
        assertEquals(decKey2.getPubKeyPoint(), encKey2.getPubKeyPoint());
        assertFalse(decKey1.isEncrypted());
        assertFalse(decKey2.isEncrypted());
        assertNotEquals(encKey1.getParent(), decKey1.getParent());   // parts of a different hierarchy
        // Check we can once again derive keys from the decrypted chain.
        decChain.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).sign(Sha256Hash.ZERO_HASH);
        decChain.getKey(KeyChain.KeyPurpose.CHANGE).sign(Sha256Hash.ZERO_HASH);
    }

    // FIXME For some reason it doesn't read the resource file
    private final static String DETERMINISTIC_WALLET_SERIALIZATION_TXT_CHILD_ROOT_KEY =
            "type: DETERMINISTIC_KEY\n" +
                    "secret_bytes: \"\\023\\354\\032\\244\\374<y\\254aq\\a\\362=\\345\\204\\n^;5\\213M\\267\\311\\234\\037RkX\\235\\363ae\"\n" +
                    "public_key: \"\\002T5VO\\274\\362m\\300\\320\\352\\005\\r!\\307\\024\\250\\307C\\324\\323\\215[\\232@\\254S\\270\\217\\362\\370\\214\\362\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"F\\336\\2067\\377M\\026)%\\357ZL#\\203\\320\\324\\217-\\3305\\310\\244\\n\\205\\277E\\323L\\250ww\\314\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "secret_bytes: \"\\362\\305\\242\\3637\\2748Z?]\\035s\\272\\253J\\300\\033\\250\\022r\\350\\020\\277U\\036K<\\335\\237\\333/\\303\"\n" +
                    "public_key: \"\\003\\2471\\326i\\331A\\337|\\373\\276\\3214\\257\\363\\266Q\\315x\\341\\317\\200\\243\\234\\336<s}\\261\\240,\\233\\371\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"Pd\\032#\\325\\213\\332\\307\\3755\\020\\316\\276\\002\\037\\262\\241\\213\\211k\\376\\254\\220R\\351\\270\\\"\\260V\\260\\362\\257\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  issued_subkeys: 2\n" +
                    "  lookahead_size: 10\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "secret_bytes: \"\\311\\327\\205Q\\005\\346\\030\\365\\026\\0331\\356\\346\\036\\234\\024\\b\\322\\202\\3726I\\351\\001 \\373\\200\\003\\260\\276\\216\\000\"\n" +
                    "public_key: \"\\003\\334\\214L\\003Zq\\365\\212P\\203b~l\\367C@T\\341\\300\\216\\037\\375\\002\\224\\t=\\301:\\266l\u007F\\364\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\036\\277^\\322!\\227i^Z\\212\\347\\272\\365C\\016\\342\\371\\236a\\022\\\\\\n\\037\\304\\264\\021\\335\\344\\340=\\234T\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  issued_subkeys: 1\n" +
                    "  lookahead_size: 10\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\232\u007F\\236W\\2235py\\021\\331i\\350\\026H\\235\\nO\\217\\231\\361M/}5\\211v\\023Kc\\253\\2307\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\032\\366\\331\\360\\276Yo\\243;!\\023\\005\\305\\246\\354\\337N\\203\\302\\264\\250\\355\\275\\346\\271!L\\\\\\252\\270\\364t\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 0\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003\\231\\254]q\\030g3\\316u\\322h\\304Ao4\\246f\\026\\266\\374\\nY\\233\\022\\034\\243\\'\\000\\030\\256\\342\\006\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\364\\201U66\\201$7l%Sy\\365]A\\265\\277&\\370\\256\\364\\347\\356\\334T\\323\\375\\347mB0\\374\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 1\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002/ \\336M\\347\\357pl@z\\204\\3240\\027\\366\\0170\\307\\337\\327\\312_$\\n\\'\\216\\237W\\017\\263bg\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"i\\206:\\003\\205BLMI\\376\\347S~\\315\\0306\\255\\274>\\220\\017\\302\\241u\\017DvD\\2662\\342\\316\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 2\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003 1\\316\\277\\355_\\343\\214}\\a\\205\\233\\325\\232\\241n\\256\\325\\300\\2369\\020\\nh\\335\\0243\\355\\362$?7\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\334(\\\\u\\022\\245\\370\\t*\\372\\315\\330\\365\\256Ms\\254J_{B\\035[f\\333\\351\\272\\261\\363\\373_\\023\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 3\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003\\354\\fhI\\2731\\026\\222\\v\\274\\027\\357\\327\\033X\\324\\270\\323\\252}\\314}\\221\\213\\272\\\\\\362k\\352\\334\u007F#\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"#\\351\\\"(\\t\\245\\006\\351\\354f\\334\\216(\\272\\252\\200\\226\\337\\370\\260XO\\375\\016/\\377\\306\\263yE\\222\\311\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 4\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002-7\\rx2\u007FzP\\r(B\\247\\350\\026\\205\\210w\\251G\\b\\254\\213\\000\\227\\271Q\\272\\342\\357\\304>G\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"US\\242J\\307\\2672<\\373l\\217\\200[\\316\\352\\361*~\\324\\f\\304\\267oD\\273\\300_\\340K\\247V\\370\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 5\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002Y\\260\\332\\377;T\\263\\335\\331\\004\\020kv\\207E=\\311|\\270*hP>)\\340\\272\\203LD\\036\\313\\271\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\r34\\'I\\027\\266\\272\\300\\003\\366f\\274\\333\\260\\006\\311\\3556!\\227\\216\\301\\361\\247\\354\\025\\305\\321\\376\\274\\214\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 6\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002I\\367x\\235p9\\334\\234\\034\\366\\247&\\321\\237\\217\\241V\\252\\017`w\\212\\301\\000\\305-\\312\\003\\352`\\302V\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\257\\001\\375\\203E\\315\\221W\\316&\\035\\244\\306\\037\\351\\361\\027\\020\\346\\305^Z\\274O\\212\\363P\\036\\273n\\367\\326\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 7\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\0029\\373\\030u\\305\\214S\\345/\\373y%\\t\\252~\\267\\f\\016t|\\354\\020\\356\\306\\313\\317\\027\\325\\376\\232kh\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\260\\203\\277\\231\\352\\265y\\020\\356r_bO\\374l\\347\\002\\032i\\216Ct\\260\\221-\\207\\200\\243\\364;\\247I\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 8\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002[\\304\\301.\\342\\253\\256\\364\\025\\'\\017\\356-t\\340R\\250Z\\327\\374\\250\\r\\331\\221`\\334\\362a[q\\260\\271\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\376\\277#\\275\\035S\\362`\\323\\246<b~D\\253\\250p\\263\\256\\364\\257\\256\\201\\016\\223[\\305\\261\\2758\\353\\203\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 9\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\253\\213j!\\316\\\\\\372-tA\\331\\362T\\363\\252)\\302\\2620\\246=X#\\201\\274\\212\\331Gi9\\362\\366\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"S`\\237\\003Q\\021\\201\\301\\341Ij\\\"\\270:\\335\\374;\\320\\037\\350m\\037\\331\\343\\3208\\211E\\276y\\215.\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 10\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\231\\323\\307\\3348\\267\\357\\310\\323\\nWf$\\326\\334\\v\\360\\273\\357\\207\\0207\\036)\\270Api\\346\\321\\270\\351\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\232Y6\\020\\317@\\r\\251]\\204m\\37796\\v5\\006\\300\\t\\342\\246.\\231r\\336Vt\\370\\373\\023_M\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 11\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003\\3647\\fs\\313\\367\\022\\317\\207\\276\\305\\370W\\b \\276\\274\\343\\344r\\351\\303\\035\\235\\227X\\a\\347Q2\\325\\351\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\030F|\\234$0\\315\\215\\203\\234\\037\\0054\\344M\\253\\343\\214S\\360\\274_\\330\\2663\\364L\\025\\250\\v\\236\\247\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 0\n" +
                    "  path: 12\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003\\273\\t\\2539\\246\\023^H\\232\\202}Hi+\\226Q\\337\\371\\002\\021\\201\\276\\261*\\236\\353\\'\\247\\325\\367t^\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\233\\314\\253j\\213|Y\\344\\200k\\217\\357*\\\\\\030\\347`\\016\\266\\033g|\\273\\330\\261\\226cj\\204\\351\\234Z\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 0\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\266}M\\017f0\\350\\357\\232\\037\\2713\\203N\\nei\\202_\\t\\034\\0343X,\\006Y\\260\\356\\340\\031W\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\2569jU\\231\\242\\230rH\\202\\025\\356C\\373\\327\\374-\\305\\313\\277Jy5mx\\034\\350\\2264\\321\\327J\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 1\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\221o\\254\\027\\3547s\\260&\\217b\\252r\\fo)~\\2107\\016\\255\\273\\223\\245A\\234\\247ay\\204\\021H\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\265\\233\\233\\354DG\\226\\326\\215\\024\\230\\334\\262J\\257LL\\363R\\316$\\357\\347\\017v\\371rpA\\244PK\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 2\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003\\301\\036A\\004O\\323\\350\\240\\227>C73n\\326P]{\\260@\\327\\242\\'\\263$H\\271\\371\\371YIJ\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"9\\214\\315\\275\\300\\206\\253U;\\235\\002\u007FfA\\016\\215\\222\\235K\\253\\311\\3648w20\\2005\\343\\310\\\\:\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 3\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002!\\373+L\\341\\025\\265\\232\\a\\247J?v\\274\\273|v\\035g\\033\\211\\026\\332\\233\\37378c\\226\\020\\304\\360\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"i\\003w\\037b!Y]\\214l\\373]`x\\355Je\\\\\\v\\205\\n\\310\\254s\\301\\272\\246\\315\\024}\\366\\037\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 4\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002p\\261|\\230%\\350\\a\\347?-}\\317\\274W\\210\\032\\331\\006\\350\\320\\016\\331\\300\\024\\302\\321[O\\210E\\231\\342\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\367\\263\\236B#W\\223\\3206\\3644!?Im\\250\\277\\vY\\322\\302\\254\\212A\\227\\352\\244\\003\\031)\\374b\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 5\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003\\355\\213b\\333\\3157\\262iu\\361\\274\\252\\271\\223\\346\\276^\\350\\260q\\272m\\025.\\256\\353\\006\\005\\020\\255&\\017\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\003\\265\\376U\\001\\240P\\'X\\364\\326\\326\\275\\375s\\306\\225\\373\\264\\306H5[\\356\\b\\301>\\227\\325\\323\\315\\344\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 6\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\003\\317\\215\\311\\246\\322\\335\\226,\\355\\243\\274E\\270\\027\\307I\\264\\344\\260\\350_\\230\\372\\034\\340\\363\\3113T\\222\\274c\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"r\\203\\252\\217hI\\312S\\323\\377](\\331C\\2711\\214T.\\031\\277\\333\\267,U|\\323x\\006\\003\\263\\233\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 7\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\277\\306\\215\\221N\\224\\b=\\v\\001\\216Ui\\033v\\250\\326\\361\\221\\332\\215\\343$\\344A\\306\\357f\\236\\330\\241\\337\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"1n@\\331(\\246#4\\262\\017\\006\\360a\\206i\\270\\211n\\344\\363\\343Y\\0000\\372,\\231\\352\\252f\\272!\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 8\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002!\\032\\210\\034\\267\\234\\t\u007F\\311T\\'(~c-dKt;\\366\\030fI5[\\026\\242\\372\\310\\342\\'\\205\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\004AX{\\301\\030\\035K\\353\\353S\\223m\\271\\352\\323\\272\\'\\202=_5\\322\\240J.\\227\\370[gZ?\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 9\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002KZ\\215?\\f\\365\\\"o\\364\\035\\n\\240\\276_\\335\\\\\\256\\277\\212J\\247\\201A\\325\\220\\361\\356\\213/!\\301\\224\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"\\247\\233H)F\\252\\276\\242\\370\\350\\263\\270\\b:^\\247d]\\232\\316QUA2\\n\\262\\321\u007FU\\003\\r]\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 10\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\204U\\310%\\024\\\"\\363\\267\\340\\220\\031\\341koQ\\210\\037\\022\\224Y\\354\\016\\370\\360\\374\\346\\216\\354@B\\247\\233\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"w\\265\\277h\\352\\025\\351\\274\\233\\310;rk\\264`*-H-r\\026\\326\\237%\\230\\034\\005\\236_6#a\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 11\n" +
                    "}\n" +
                    "\n" +
                    "type: DETERMINISTIC_KEY\n" +
                    "public_key: \"\\002\\213\\241A\\022c\\322\\031\\367O\\273\\375\\3354\\'Vh\\371\\362\\202\\220\\253\\366\\206:\\033\\347\\300\\227<6\\252\\034\"\n" +
                    "deterministic_key {\n" +
                    "  chain_code: \"R\\222\\341\\341.\\352\\306O\\340+\\276\\266#K\\211\\022\\264\\203\\225\\240\\246\\263\\023l\\327\\356 \\350\\342\\242]F\"\n" +
                    "  path: 2147483692\n" +
                    "  path: 2147483649\n" +
                    "  path: 2147483648\n" +
                    "  path: 1\n" +
                    "  path: 12\n" +
                    "}";

    private final static String DETERMINISTIC_WALLET_SERIALIZATION_TXT_MASTER_KEY =
            "type: DETERMINISTIC_KEY\n" +
            "secret_bytes: \"\\354B\\331\\275;\\000\\254?\\3428\\006\\220G\\365\\243\\333s\\260s\\213R\\313\\307\\377f\\331B\\351\\327=\\001\\333\"\n" +
            "public_key: \"\\002\\357\\\\\\252\\376]\\023\\315\\'\\316`\\317\\362\\032@\\232\\\"\\360\\331\\335\\221] `\\016,\\351<\\b\\300\\225\\032m\"\n" +
            "deterministic_key {\n" +
            "  chain_code: \"\\370\\017\\223\\021O?.@gZ|\\233j\\3437\\317q-\\241!\u007FJ \\323\\'\\264s\\203\\314\\321\\v\\346\"\n" +
            "  path: 2147483648\n" +
            "}\n" +
            "\n" +
            "type: DETERMINISTIC_KEY\n" +
            "secret_bytes: \"<M\\020I\\364\\276\\336Z\\255\\341\\330\\257\\337 \\366E_\\027\\2433w\\325\\263\\\"$\\350\\f\\244\\006\\251u\\021\"\n" +
            "public_key: \"\\002\\361V\\216\\001\\371p\\270\\212\\272\\236%\\216\\356o\\025g\\r\\035>a\\305j\\001P\\217Q\\242\\261.\\353\\367\\315\"\n" +
            "deterministic_key {\n" +
            "  chain_code: \"\\231B\\211S[\\216\\237\\277q{a