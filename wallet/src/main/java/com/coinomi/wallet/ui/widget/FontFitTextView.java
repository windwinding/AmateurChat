package com.coinomi.wallet.ui.widget;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on
 * http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview?answertab=votes#tab-top
 */
public class FontFitTextView extends TextView {
    private static final Logger log = LoggerFactory.getLogger(FontFitTextView.class);

    //Attributes
    private float maxTextSize;
    private Paint mTestPaint;

    public FontFitTextView(Context context) {
        super(context);
        initialise();
    }

    public FontFitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    private void initialise() {
        mTestPaint = new Paint();
        mTestPaint.set(this.getPaint());
        maxTextSize = getTextSize();
        //max size defaults to the initially specified text size 