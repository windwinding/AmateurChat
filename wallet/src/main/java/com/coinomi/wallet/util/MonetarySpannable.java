
package com.coinomi.wallet.util;

/*
 * Copyright 2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.coinomi.core.util.MonetaryFormat;
import com.coinomi.wallet.Constants;

import org.bitcoinj.core.Monetary;

import java.util.regex.Matcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Andreas Schildbach
 */
public final class MonetarySpannable extends SpannableString {
    public MonetarySpannable(final MonetaryFormat format, final boolean signed, @Nullable final Monetary value) {
        super(format(format, signed, value));
    }

    public MonetarySpannable(final MonetaryFormat format, @Nullable final Monetary value) {
        super(format(format, false, value));
    }

    private static CharSequence format(final MonetaryFormat format, final boolean signed,
                                       final Monetary value) {
        if (value == null)
            return "";

        checkArgument(value.signum() >= 0 || signed);

        int smallestUnitExponent = value.smallestUnitExponent();

        if (signed)
            return format.negativeSign(Constants.CURRENCY_MINUS_SIGN).positiveSign(Constants.CURRENCY_PLUS_SIGN).format(value, smallestUnitExponent);
        else
            return format.format(value, smallestUnitExponent);
    }

    public MonetarySpannable applyMarkup(@Nullable final Object prefixSpan1, @Nullable final Object prefixSpan2,
                                         @Nullable final Object insignificantSpan) {
        applyMarkup(this, prefixSpan1, prefixSpan2, BOLD_SPAN, insignificantSpan);
        return this;
    }
