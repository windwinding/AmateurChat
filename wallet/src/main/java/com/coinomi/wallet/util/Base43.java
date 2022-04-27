package com.coinomi.wallet.util;

/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.charset.Charset;

import javax.annotation.Nonnull;

/**
 * Base43, derived from bitcoinj Base58
 *
 * @author Andreas Schildbach
 */
public class Base43
{
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$*+-./:".toCharArray();

    private static final int[] INDEXES = new int[128];
    static
    {
        for (int i = 0; i < INDEXES.length; i++)
            INDEXES[i] = -1;

        for (int i = 0; i < ALPHABET.length; i++)
            INDEXES[ALPHABET[i]] = i;
    }

    public static String encode(@Nonnull byte[] input)
    {
        if (input.length == 0)
            return "";

        input = copyOfRange(input, 0, input.length);

        // Count leading zeroes.
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0)
            ++zeroCount;

        // The actual encoding.
        final byte[] temp = new byte[input.length * 2];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input.length)
        {
            byte mod = divmod43(input, startAt);
            if (input[startAt] == 0)
                ++startAt;
            temp[--j] = (byte) ALPHABET[mod];
        }

        // Strip e