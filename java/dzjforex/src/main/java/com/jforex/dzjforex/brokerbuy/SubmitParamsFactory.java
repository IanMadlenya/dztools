package com.jforex.dzjforex.brokerbuy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.Instrument;
import com.jforex.dzjforex.order.StopLoss;
import com.jforex.programming.order.OrderParams;
import com.jforex.programming.order.task.params.RetryParams;
import com.jforex.programming.order.task.params.basic.SubmitParams;

import io.reactivex.Single;

public class SubmitParamsFactory {

    private final RetryParams retryParams;
    private final StopLoss stopLoss;

    private final static Logger logger = LogManager.getLogger(SubmitParamsFactory.class);

    public SubmitParamsFactory(final RetryParams retryParams,
                               final StopLoss stopLoss) {
        this.retryParams = retryParams;
        this.stopLoss = stopLoss;
    }

    public Single<SubmitParams> get(final Instrument instrument,
                                    final BrokerBuyData brokerBuyData) {
        return Single
            .defer(() -> stopLoss.forSubmit(instrument, brokerBuyData))
            .map(slPrice -> create(instrument,
                                   brokerBuyData,
                                   slPrice));

    }

    private SubmitParams create(final Instrument instrument,
                                final BrokerBuyData brokerBuyData,
                                final double slPrice) {
        final String orderLabel = brokerBuyData.orderLabel();
        final int orderID = brokerBuyData.orderID();
        final OrderCommand orderCommand = brokerBuyData.orderCommand();
        final double amount = brokerBuyData.amount();

        final OrderParams orderParams = OrderParams
            .forInstrument(instrument)
            .withOrderCommand(orderCommand)
            .withAmount(amount)
            .withLabel(orderLabel)
            .stopLossPrice(slPrice)
            .build();

        return SubmitParams
            .withOrderParams(orderParams)
            .doOnStart(() -> logger.info("Trying to open order for " + instrument + ":\n"
                    + "command: " + orderCommand + "\n"
                    + "amount: " + amount + "\n"
                    + "label: " + orderLabel + "\n"
                    + "ID: " + orderID + "\n"
                    + "slPrice: " + slPrice))
            .doOnError(e -> logger.error("Opening order for " + instrument
                    + " with ID " + orderID
                    + " failed!" + e.getMessage()))
            .doOnComplete(() -> logger.info("Opening order for " + instrument
                    + " with ID " + orderID
                    + " done."))
            .retryOnReject(retryParams)
            .build();
    }
}
