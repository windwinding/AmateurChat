package com.coinomi.core.exchange.shapeshift.data;

import com.coinomi.core.exchange.shapeshift.ShapeShift;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Stack;

/**
 * @author John L. Jegutanis
 */
public class ShapeShiftTime extends ShapeShiftBase {
    public final Status status;
    public final int secondsRemaining;

    public static enum Status {
        PENDING, EXPIRED, UN