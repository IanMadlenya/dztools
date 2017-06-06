package com.jforex.dzjforex.brokerhistory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.IBar;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.jforex.dzjforex.brokertime.TimeConvert;
import com.jforex.programming.math.MathUtil;
import com.jforex.programming.misc.DateTimeUtil;
import com.jforex.programming.quote.BarQuote;
import com.jforex.programming.quote.TickQuote;

public class HistoryTickFiller {

    private final double tickParams[];

    private final static Logger logger = LogManager.getLogger(HistoryTickFiller.class);

    public HistoryTickFiller(final double tickParams[]) {
        this.tickParams = tickParams;
    }

    public void fillBarQuote(final BarQuote barQuote,
                             final int startIndex) {
        final IBar bar = barQuote.bar();
        final double noSpreadAvailable = 0.0;

        tickParams[startIndex] = bar.getOpen();
        tickParams[startIndex + 1] = bar.getClose();
        tickParams[startIndex + 2] = bar.getHigh();
        tickParams[startIndex + 3] = bar.getLow();
        tickParams[startIndex + 4] = TimeConvert.getUTCTimeFromBar(bar);
        tickParams[startIndex + 5] = noSpreadAvailable;
        tickParams[startIndex + 6] = bar.getVolume();
        logger.trace("Stored bar for " + barQuote.instrument()
                + " open " + bar.getOpen()
                + " close " + bar.getClose()
                + " high " + bar.getHigh()
                + " low " + bar.getLow()
                + " time " + DateTimeUtil.formatMillis(bar.getTime())
                + " spread " + noSpreadAvailable
                + " volume " + bar.getVolume());
    }

    public void fillTickQuote(final TickQuote tickQuote,
                              final int startIndex) {
        final ITick tick = tickQuote.tick();
        final double ask = tick.getAsk();
        final double bid = tick.getBid();
        final Instrument instrument = tickQuote.instrument();

        tickParams[startIndex] = ask;
        tickParams[startIndex + 1] = ask;
        tickParams[startIndex + 2] = ask;
        tickParams[startIndex + 3] = ask;
        tickParams[startIndex + 4] = TimeConvert.getUTCTimeFromTick(tick);
        tickParams[startIndex + 5] = MathUtil.roundPrice(ask - bid, instrument);
        tickParams[startIndex + 6] = tick.getAskVolume();
        logger.trace("Stored tick for " + instrument
                + " ask " + ask
                + " time " + DateTimeUtil.formatMillis(tick.getTime())
                + " spread " + MathUtil.roundPrice(ask - bid, instrument)
                + " volume " + tick.getAskVolume());
    }
}
