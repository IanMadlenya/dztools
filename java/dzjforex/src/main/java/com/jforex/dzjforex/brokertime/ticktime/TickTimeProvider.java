package com.jforex.dzjforex.brokertime.ticktime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jforex.dzjforex.brokertime.TimeWatch;
import com.jforex.programming.misc.DateTimeUtil;

import io.reactivex.Single;

public class TickTimeProvider {

    private final TickTimeFetch tickTimeFetch;
    private final TimeWatch timeWatch;

    private final static Logger logger = LogManager.getLogger(TickTimeProvider.class);

    public TickTimeProvider(final TickTimeFetch tickTimeFetch,
                            final TimeWatch timeWatch) {
        this.tickTimeFetch = tickTimeFetch;
        this.timeWatch = timeWatch;
    }

    public Single<Long> get() {
        return Single
            .defer(tickTimeFetch::get)
            .doOnSuccess(latestTickTime -> logger.debug("Fetched latest tick time "
                    + DateTimeUtil.formatMillis(latestTickTime)))
            .map(timeWatch::getForNewTime);
    }
}
