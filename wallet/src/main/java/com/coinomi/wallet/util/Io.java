package com.coinomi.wallet.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;

import javax.annotation.Nonnull;

/**
 * @author Andreas Schildbach
 */
public class Io
{
    private static final Logger log = LoggerFactory.getLogger(Io.class);

    public static final long copy(@Nonnull final Reader reader, @Nonnull final StringBuilder builder) throws IOException
    {
        return copy(reader, builder, 0);
    }

    public static final long copy(@Nonnull final Reader reader, @Nonnull final StringBuilder builder, final long maxChars) throws IOException
    {
        final char[] buffer = new char[256];
        long count = 0;
        int n = 0;
        while (-1 != (n = reader.read(buffer)))
       