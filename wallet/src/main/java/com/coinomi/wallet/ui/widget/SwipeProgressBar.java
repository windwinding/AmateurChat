package com.coinomi.wallet.ui.widget;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;


/**
 * Custom progress bar that shows a cycle of colors as widening circles that
 * overdraw each other. When finished, the bar is cleared from the inside out as
 * the main cycle continues. Before running, this can also indicate how close
 * the user is to triggering something (e.g. how far they need to pull down to
 * trigger a refresh).
 */
final class SwipeProgressBar {

    // Default progress animation colors are grays.
    private final static int COLOR1 = 0xB3000000;
    private final static int COLOR2 = 0x80000000;
    private final static int COLOR3 = 0x4d000000;
    private final static int COLOR4 = 0x1a000000;

    // The duration of the animation cycle.
    private static final int ANIMATION_DURATION_MS = 2000;

    // The duration of the animation to clear the bar.
    private static final int FINISH_ANIMATION_DURATION_MS = 1000;

    // Interpolator for varying the speed of the animation.
    private static final Interpolator INTERPOLATOR = BakedBezierInterpolator.getInstance();

    private final Paint mPaint = new Paint();
    private final RectF mClipRect = new RectF();
    private float mTriggerPercentage;
    private long mStartTime;
    private long mFinishTime;
    private boolean mRunning;

    // Colors used when rendering the animation,
    private int mColor1;
    private int mColor2;
    private int mColor3;
    private int mColor4;
    private View mParent;

    private Rect mBounds = new Rect();

    public SwipeProgressBar(View parent) {
        mParent = parent;
        mColor1 = COLOR1;
        mColor2 = COLOR2;
        mColor3 = COLOR3;
        mColor4 = COLOR4;
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     *
     * @param color1 Integer representation of a color.
     * @param color2 Integer representation of a color.
     * @param color3 Integer representation of a color.
     * @param color4 Integer representation of a color.
     */
    void setColorScheme(int color1, int color2, int color3, int color4) {
        mColor1 = color1;
        mColor2 = color2;
        mColor3 = color3;
        mColor4 = color4;
    }

    /**
     * Update the progress the user has made toward triggering the swipe
     * gesture. and use this value to update the percentage of the trigger that
     * is shown.
     */
    void setTriggerPercentage(float triggerPercentage) {
        mTriggerPercentage = triggerPercentage;
        mStartTime = 0;
        ViewCompat.postInvalidateOnAnimation(mParent);
    }

    /**
     * Start showing the progress animation.
     */
    void start() {
        if (!mRunning) {
            mTriggerPercentage = 0;
            mStartTime = AnimationUtils.currentAnimationTimeMillis();
            mRunning = true;
            mParent.postInvalidate();
        }
    }

    /**
     * Stop showing the progress animation.
     */
    void stop() {
        if (mRunning) {
            mTriggerPercentage = 0;
            mFinishTime = AnimationUtils.currentAnimationTimeMillis();
            mRunning = false;
            mParent.postInvalidate();
        }
    }

    /**
     * @return Return whether the progress animation is currently running.
     */
    boolean isRunning() {
        return mRunning || mFinishTime > 0;
    }

    void draw(Canvas canvas) {
        final int width = mBounds.width();
        final int height = mBounds.height();
        final int cx = width / 2;
        final int cy = height / 2;
        boolean drawTriggerWhileFinishing = false;
        int restoreCount = canvas.save();
        canvas.clipRect(mBounds);

        if (mRunning || (mFinishTime > 0)) {
            long now = AnimationUtils.currentAnimationTimeMillis();
            long elapsed = (now - mStartTime) % ANIMATION_DURATION_MS;
            long iterations = (now - mStartTime) / ANIMATION_DURATION_MS;
            float rawProgress = (elapsed / (ANIMATION_DURATION_MS / 100f));

            // If we're not running anymore, that means we're running through
            // the finish animation.
            if (!mRunning) {
                // If the finish animation is done, don't draw anything, and
                // don't repost.
                if ((now - mFinishTime) >= FINISH_ANIMATION_DURATION_MS) {
                    mFinishTime = 0;
                    return;
                }

                // Otherwise, use a 0 opacity alpha layer to clear the animation
                // from the inside out. This layer will prevent the circles from
                // drawing within its bounds.
                long finishElapsed = (now - mFinishTime) % FINISH_ANIMATION_DURATION_MS;
                float finishProgress = (finishElapsed / (FINISH_ANIMATION_DURATION_MS / 100f));
                float pct = (finishProgress / 100f);
                // Radius of the circle is half of the screen.
                float clearRadius = width / 2 * INTERPOLATOR.getInterpolation(pct);
                mClipRect.set(cx - clearRadius, 0, cx + clearRadius, height);
                canvas