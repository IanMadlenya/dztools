package com.jforex.dzjforex.brokertime.dummy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.IMessage;
import com.jforex.programming.order.event.OrderEvent;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;

public class DummyMessageHandler {

    private final BehaviorSubject<Boolean> wasOffline = BehaviorSubject.createDefault(false);

    private final static String systemUnavailablePrefix = "SYSTEM_UNAVAILABLE";
    private final static Logger logger = LogManager.getLogger(DummyMessageHandler.class);

    public boolean wasOffline() {
        return wasOffline.getValue();
    }

    public void handleRejectEvent(final OrderEvent orderEvent) {
        Single
            .just(orderEvent.message())
            .map(IMessage::getContent)
            .filter(content -> content.startsWith(systemUnavailablePrefix))
            .doOnSuccess(content -> logger.debug("System unavailable message received -> market is closed."))
            .isEmpty()
            .subscribe(isSystemAvailable -> wasOffline.onNext(!isSystemAvailable));
    }

    public void handleOKEvent(final OrderEvent orderEvent) {
        Single
            .just(orderEvent.order())
            .doOnSubscribe(d -> {
                logger.debug("Dummy order was opened -> market is open.");
                wasOffline.onNext(false);
            })
            .flatMapCompletable(order -> Completable.fromAction(order::close))
            .subscribe();
    }
}
