package com.jforex.dzjforex.brokerlogin;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.system.IClient;
import com.jforex.dzjforex.config.PluginConfig;
import com.jforex.dzjforex.config.ZorroReturnValues;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class BrokerLogin {

    private final IClient client;
    private final LoginExecutor loginExecutor;
    private Observable<Long> retryDelayTimer;
    private final BehaviorSubject<Boolean> isLoginAvailable = BehaviorSubject.createDefault(true);

    private final static Logger logger = LogManager.getLogger(BrokerLogin.class);

    public BrokerLogin(final IClient client,
                        final LoginExecutor loginExecutor,
                        final PluginConfig pluginConfig) {
        this.client = client;
        this.loginExecutor = loginExecutor;

        initRetryDelayTimer(pluginConfig.loginRetryDelay());
    }

    private void initRetryDelayTimer(final long retryDelay) {
        retryDelayTimer = Observable
            .timer(retryDelay, TimeUnit.MILLISECONDS)
            .doOnSubscribe(d -> {
                isLoginAvailable.onNext(false);
                logger.debug("Starting login retry delay timer. Login is not available until timer elapsed.");
            })
            .doOnComplete(() -> {
                isLoginAvailable.onNext(true);
                logger.debug("Login retry delay timer completed. Login is available again.");
            });
    }

    public int login(final BrokerLoginData brokerLoginData) {
        if (client.isConnected())
            return ZorroReturnValues.LOGIN_OK.getValue();
        if (!isLoginAvailable.getValue())
            return ZorroReturnValues.LOGIN_FAIL.getValue();

        return handleLoginResult(loginExecutor.login(brokerLoginData));
    }

    private int handleLoginResult(final int loginResult) {
        if (loginResult == ZorroReturnValues.LOGIN_FAIL.getValue())
            startRetryDelayTimer();
        
        return loginResult;
    }

    private void startRetryDelayTimer() {
        retryDelayTimer.subscribe();
    }

    public int logout() {
        return loginExecutor.logout();
    }
}
