package com.jforex.dzjforex.history;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.jforex.dzjforex.config.PluginConfig;
import com.jforex.dzjforex.misc.RxUtility;

import io.reactivex.Single;

public class TickHistoryByShift {

    private final HistoryWrapper historyWrapper;
    private final HistoryFetchDate historyFetchDate;
    private final PluginConfig pluginConfig;
    private final long tickFetchMillis;

    private final static Logger logger = LogManager.getLogger(TickHistoryByShift.class);

    public TickHistoryByShift(final HistoryWrapper historyWrapper,
                              final HistoryFetchDate historyFetchDate,
                              final PluginConfig pluginConfig) {
        this.historyWrapper = historyWrapper;
        this.historyFetchDate = historyFetchDate;
        this.pluginConfig = pluginConfig;

        tickFetchMillis = pluginConfig.tickFetchMillis();
    }

    public Single<List<ITick>> get(final Instrument instrument,
                                   final long endDate,
                                   final int shift) {
        return historyFetchDate
            .startDatesForTick(instrument, endDate)
            .flatMapSingle(startDate -> getTicksReversed(instrument, startDate))
            .flatMapIterable(ticks -> ticks)
            .take(shift + 1)
            .toList()
            .retryWhen(RxUtility.retryForHistory(pluginConfig))
            .doOnSuccess(ticks -> logger.debug("Fetched " + ticks.size() + " ticks for " + instrument));
    }

    private Single<List<ITick>> getTicksReversed(final Instrument instrument,
                                                 final long startDate) {
        return historyWrapper.getTicksReversed(instrument,
                                               startDate,
                                               startDate + tickFetchMillis - 1);
    }
}
