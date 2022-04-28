
/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coinomi.wallet.util;


import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.annotation.Nonnull;

import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.ParametersWithIV;

import com.google.common.io.BaseEncoding;

/**
 * This class encrypts and decrypts a string in a manner that is compatible with OpenSSL.
 *
 * If you encrypt a string with this class you can decrypt it with the OpenSSL command: openssl enc -d -aes-256-cbc -a
 * -in cipher.txt -out plain.txt -pass pass:aTestPassword
 *
 * where: cipher.txt = file containing the cipher text plain.txt - where you want the plaintext to be saved
 *
 * substitute your password for "aTestPassword" or remove the "-pass" parameter to be prompted.
 *
 * @author jim
 * @author Andreas Schildbach
 */
public class Crypto
{
    private static final BaseEncoding BASE64 = BaseEncoding.base64().withSeparator("\n", 76);
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * number of times the password & salt are hashed during key creation.
     */
    private static final int NUMBER_OF_ITERATIONS = 1024;

    /**
     * Key length.
     */
    private static final int KEY_LENGTH = 256;

    /**
     * Initialization vector length.
     */
    private static final int IV_LENGTH = 128;