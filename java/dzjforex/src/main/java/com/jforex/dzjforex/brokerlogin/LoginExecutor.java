package com.jforex.dzjforex.brokerlogin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jforex.dzjforex.Zorro;
import com.jforex.dzjforex.config.ZorroReturnValues;
import com.jforex.programming.connection.Authentification;
import com.jforex.programming.connection.LoginCredentials;

import io.reactivex.Single;

public class LoginExecutor {

    private final Authentification authentification;
    private final CredentialsFactory credentialsFactory;
    private final Zorro zorro;

    private final static Logger logger = LogManager.getLogger(LoginExecutor.class);

    public LoginExecutor(final Authentification authentification,
                         final CredentialsFactory credentialsFactory,
                         final Zorro zorro) {
        this.authentification = authentification;
        this.credentialsFactory = credentialsFactory;
        this.zorro = zorro;
    }

    public int login(final BrokerLoginData brokerLoginData) {
        final LoginCredentials credentials = credentialsFactory.create(brokerLoginData.username(),
                                                                       brokerLoginData.password(),
                                                                       brokerLoginData.loginType());
        return loginWithCredentials(credentials);
    }

    private int loginWithCredentials(final LoginCredentials credentials) {
        final Single<Integer> login = authentification
            .login(credentials)
            .andThen(Single.just(ZorroReturnValues.LOGIN_OK.getValue()))
            .doOnError(e -> logger.error("Failed to login! " + e.getMessage()))
            .onErrorReturnItem(ZorroReturnValues.LOGIN_FAIL.getValue());

        return zorro.progressWait(login);
    }

    public int logout() {
        authentification
            .logout()
            .blockingAwait();
        return ZorroReturnValues.LOGOUT_OK.getValue();
    }
}
