package com.jforex.dzjforex.test.util;

import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Clock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.BDDMockito;
import org.mockito.Mock;

import com.dukascopy.api.ICurrency;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.JFException;
import com.jforex.dzjforex.config.PluginConfig;
import com.jforex.dzjforex.config.ZorroReturnValues;
import com.jforex.dzjforex.order.OrderCloseResult;
import com.jforex.dzjforex.order.OrderLabelUtil;
import com.jforex.dzjforex.order.OrderSetLabelResult;
import com.jforex.dzjforex.order.OrderSetSLResult;
import com.jforex.dzjforex.order.OrderSubmitResult;
import com.jforex.programming.connection.LoginCredentials;
import com.jforex.programming.currency.CurrencyFactory;

public class CommonUtilForTest extends BDDMockito {

    @Mock
    protected IEngine engineMock;
    @Mock
    protected OrderLabelUtil labelUtilMock;
    @Mock
    protected PluginConfig pluginConfigMock;
    @Mock
    protected Clock clockMock;

    protected static final RxTestUtil rxTestUtil = RxTestUtil.get();
    protected static final String jnlpDEMO = "jnlpDEMO";
    protected static final String jnlpReal = "jnlpReal";
    protected static final String username = "John";
    protected static final String password = "Doe123";
    protected static final String pin = "1234";
    protected static final String loginTypeDemo = "Demo";
    protected static final String loginTypeReal = "Real";
    protected static final ICurrency accountCurrency = CurrencyFactory.EUR;
    protected static final String orderLabelPrefix = "Zorro";

    protected static final LoginCredentials loginCredentials =
            new LoginCredentials(jnlpDEMO,
                                 username,
                                 password);
    protected static final LoginCredentials loginCredentialsWithPin =
            new LoginCredentials(jnlpReal,
                                 username,
                                 password,
                                 pin);

    protected static final JFException jfException = new JFException("");
    protected static final Logger logger = LogManager.getLogger(CommonUtilForTest.class);

    public CommonUtilForTest() {
        initMocks(this);

        when(pluginConfigMock.orderLabelPrefix()).thenReturn(orderLabelPrefix);

        coverageOnEnumsCorrection();
    }

    private final void coverageOnEnumsCorrection() {
        OrderSubmitResult
            .valueOf(OrderSubmitResult.OK.toString());
        OrderSetSLResult
            .valueOf(OrderSetSLResult.OK.toString());
        OrderCloseResult
            .valueOf(OrderCloseResult.OK.toString());
        OrderSetLabelResult
            .valueOf(OrderSetLabelResult.OK.toString());
        ZorroReturnValues
            .valueOf(ZorroReturnValues.ACCOUNT_AVAILABLE.toString());
    }
}
