package com.coinomi.wallet.ui;

/*
 * Copyright 2012-2014 the original author or authors.
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


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.coinomi.wallet.R;
import com.google.zxing.ResultPoint;

/**
 * @author Andreas Schildbach
 */
public class ScannerView extends View
{
    private static final long LASER_ANIMATION_DELAY_MS = 100l;
    private static final int DOT_OPACITY = 0xa0;
    private static final int DOT_SIZE = 8;
    private static final int DOT_TTL_MS = 500;

    private final Paint maskPaint;
    private final Paint laserPaint;
    private final Paint dotPaint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final Map<ResultPoint, Long> dots = new HashMap<ResultPoint, Long>(16);
    private Rect frame, framePreview;

    public ScannerView(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);

        final Resources res = getResources();
        maskColor = res.getColor(R.color.scan_mask);
        resultColor = res.getColor(R.color.scan_result_view);
        final int laserColor = res.getColor(R.color.scan_laser);
        final int dotColor = res.g