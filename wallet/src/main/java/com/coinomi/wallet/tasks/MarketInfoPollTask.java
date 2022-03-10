package com.coinomi.wallet.tasks;

import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

import javax.annotation.Nullable;

/**
 * @author John L. Jegutanis
 */
public abstract class MarketInfoPollTask extends TimerTask {
    private static final Logger log = LoggerFactory.getLogger(MarketInfoPollTask.class);

    private final ShapeShift shapeShift;
    private String pair;

    public MarketInfoPollTask(ShapeShift shapeShift, String pair) {
        this.shapeShift = shapeShift;
        this.pair = pair;
    }

    abstract public void onHandleMarketInfo(ShapeShiftMarketInfo marketInfo);

    public void updatePair(String newPair) {
        this.pair = newPair;
    }

    @Override
    public void run() {
        ShapeShiftMarketInfo marketInfo = getMarketInfoSync(shapeShift, pair);
        if (mar