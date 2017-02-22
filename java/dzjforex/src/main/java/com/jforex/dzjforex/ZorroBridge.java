package com.jforex.dzjforex;

import java.time.Clock;

import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.IContext;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.jforex.dzjforex.brokerapi.BrokerAccount;
import com.jforex.dzjforex.brokerapi.BrokerAsset;
import com.jforex.dzjforex.brokerapi.BrokerBuy;
import com.jforex.dzjforex.brokerapi.BrokerHistory2;
import com.jforex.dzjforex.brokerapi.BrokerLogin;
import com.jforex.dzjforex.brokerapi.BrokerSell;
import com.jforex.dzjforex.brokerapi.BrokerStop;
import com.jforex.dzjforex.brokerapi.BrokerSubscribe;
import com.jforex.dzjforex.brokerapi.BrokerTime;
import com.jforex.dzjforex.brokerapi.BrokerTrade;
import com.jforex.dzjforex.config.PluginConfig;
import com.jforex.dzjforex.config.ZorroReturnValues;
import com.jforex.dzjforex.handler.LoginHandler;
import com.jforex.dzjforex.history.BarFetchTimeCalculator;
import com.jforex.dzjforex.history.BarFetcher;
import com.jforex.dzjforex.history.HistoryProvider;
import com.jforex.dzjforex.history.TickFetcher;
import com.jforex.dzjforex.misc.AccountInfo;
import com.jforex.dzjforex.misc.CredentialsFactory;
import com.jforex.dzjforex.misc.InfoStrategy;
import com.jforex.dzjforex.misc.MarketData;
import com.jforex.dzjforex.misc.PinProvider;
import com.jforex.dzjforex.order.HistoryOrders;
import com.jforex.dzjforex.order.OrderClose;
import com.jforex.dzjforex.order.OrderLabelUtil;
import com.jforex.dzjforex.order.OrderRepository;
import com.jforex.dzjforex.order.OrderSetLabel;
import com.jforex.dzjforex.order.OrderSetSL;
import com.jforex.dzjforex.order.OrderSubmit;
import com.jforex.dzjforex.order.RunningOrders;
import com.jforex.dzjforex.order.TradeUtil;
import com.jforex.dzjforex.time.NTPFetch;
import com.jforex.dzjforex.time.NTPProvider;
import com.jforex.dzjforex.time.ServerTimeProvider;
import com.jforex.dzjforex.time.TickTimeProvider;
import com.jforex.dzjforex.time.TimeConvert;
import com.jforex.programming.client.ClientUtil;
import com.jforex.programming.strategy.StrategyUtil;

public class ZorroBridge {

    private IClient client;
    private final ClientUtil clientUtil;
    private IContext context;
    private final PinProvider pinProvider;
    private final InfoStrategy infoStrategy;
    private long strategyID;
    private final CredentialsFactory credentialsFactory;
    private AccountInfo accountInfo;
    private final LoginHandler loginHandler;
    private final BrokerLogin brokerLogin;
    private HistoryProvider historyProvider;
    private BrokerAsset brokerAsset;
    private BrokerAccount brokerAccount;
    private MarketData marketData;
    private BrokerTime brokerTime;
    private BrokerTrade brokerTrade;
    private TradeUtil tradeUtil;
    private OrderLabelUtil labelUtil;
    private BrokerStop brokerStop;
    private BrokerBuy brokerBuy;
    private OrderSubmit submitHandler;
    private OrderSetLabel setLabel;
    private OrderClose orderClose;
    private OrderSetSL setSLHandler;
    private BrokerSell brokerSell;
    private TimeConvert dateTimeUtils;
    private BrokerSubscribe brokerSubscribe;
    private RunningOrders runningOrders;
    private HistoryOrders historyOrders;
    private OrderRepository orderRepository;
    private BrokerHistory2 brokerHistory2;
    private BarFetcher barFetcher;
    private BarFetchTimeCalculator barFetchTimeCalculator;
    private TickFetcher tickFetcher;
    private NTPFetch ntpFetch;
    private NTPProvider ntpProvider;
    private ServerTimeProvider serverTimeProvider;
    private TickTimeProvider tickTimeProvider;
    private final PluginConfig pluginConfig = ConfigFactory.create(PluginConfig.class);

    private final static Logger logger = LogManager.getLogger(ZorroBridge.class);

    public ZorroBridge() {
        initClientInstance();

        clientUtil = new ClientUtil(client, pluginConfig.cacheDirectory());
        pinProvider = new PinProvider(client, pluginConfig.realConnectURL());
        credentialsFactory = new CredentialsFactory(pinProvider, pluginConfig);
        loginHandler = new LoginHandler(clientUtil.authentification(), credentialsFactory);
        brokerLogin = new BrokerLogin(loginHandler,
                                      client,
                                      pluginConfig);
        infoStrategy = new InfoStrategy();
    }

    private void initClientInstance() {
        try {
            client = ClientFactory.getDefaultInstance();
            logger.debug("IClient successfully initialized.");
            return;
        } catch (final ClassNotFoundException e) {
            logger.error("IClient ClassNotFoundException occured! " + e.getMessage());
        } catch (final IllegalAccessException e) {
            logger.error("IClient IllegalAccessException occured!" + e.getMessage());
        } catch (final InstantiationException e) {
            logger.error("IClient InstantiationException occured!" + e.getMessage());
        }
        ZorroLogger.indicateError();
    }

    private void initComponents() {
        context = infoStrategy.getContext();
        final StrategyUtil strategyUtil = infoStrategy.strategyUtil();

        accountInfo = new AccountInfo(context.getAccount(),
                                      strategyUtil.calculationUtil(),
                                      pluginConfig);
        brokerSubscribe = new BrokerSubscribe(client, accountInfo);
        historyProvider = new HistoryProvider(context.getHistory(), pluginConfig);
        labelUtil = new OrderLabelUtil(pluginConfig, Clock.systemDefaultZone());

        barFetchTimeCalculator = new BarFetchTimeCalculator(historyProvider);
        barFetcher = new BarFetcher(historyProvider, barFetchTimeCalculator);
        tickFetcher = new TickFetcher(historyProvider);
        brokerHistory2 = new BrokerHistory2(barFetcher, tickFetcher);
        brokerAsset = new BrokerAsset(accountInfo, strategyUtil);
        brokerAccount = new BrokerAccount(accountInfo);

        ntpFetch = new NTPFetch(pluginConfig);
        ntpProvider = new NTPProvider(ntpFetch, pluginConfig);
        tickTimeProvider = new TickTimeProvider(strategyUtil.tickQuoteProvider().repository(),
                                                Clock.systemDefaultZone());
        serverTimeProvider = new ServerTimeProvider(ntpProvider,
                                                    tickTimeProvider,
                                                    Clock.systemDefaultZone());

        runningOrders = new RunningOrders(context.getEngine());
        historyOrders = new HistoryOrders(historyProvider,
                                          brokerSubscribe,
                                          pluginConfig,
                                          serverTimeProvider);
        orderRepository = new OrderRepository(runningOrders,
                                              historyOrders,
                                              labelUtil);
        marketData = new MarketData(context.getDataService());
        brokerTime = new BrokerTime(client,
                                    serverTimeProvider,
                                    marketData);

        tradeUtil = new TradeUtil(orderRepository,
                                  strategyUtil,
                                  accountInfo,
                                  labelUtil,
                                  pluginConfig);
        brokerTrade = new BrokerTrade(tradeUtil);
        setSLHandler = new OrderSetSL(tradeUtil);
        brokerStop = new BrokerStop(setSLHandler, tradeUtil);
        submitHandler = new OrderSubmit(tradeUtil);
        brokerBuy = new BrokerBuy(submitHandler, tradeUtil);
        setLabel = new OrderSetLabel(tradeUtil);
        orderClose = new OrderClose(tradeUtil);
        brokerSell = new BrokerSell(tradeUtil,
                                    orderClose,
                                    setLabel);
    }

    public int doLogin(final String userName,
                       final String password,
                       final String type,
                       final String accountInfos[]) {
        final int loginResult = brokerLogin.login(userName,
                                                  password,
                                                  type);
        if (loginResult == ZorroReturnValues.LOGIN_OK.getValue()) {
            strategyID = client.startStrategy(infoStrategy);
            initComponents();
            accountInfos[0] = accountInfo.id();
        }

        return loginResult;
    }

    public int doLogout() {
        logger.info("Logout called");
//        client.stopStrategy(strategyID);
//        return brokerLogin.logout();
        return ZorroReturnValues.LOGOUT_OK.getValue();
    }

    public int doBrokerTime(final double serverTimeData[]) {
        return brokerTime.doBrokerTime(serverTimeData);
    }

    public int doSubscribeAsset(final String instrumentName) {
        return brokerSubscribe.subscribe(instrumentName);
    }

    public int doBrokerAsset(final String instrumentName,
                             final double assetParams[]) {
        return brokerAsset.fillAssetParams(instrumentName, assetParams);
    }

    public int doBrokerAccount(final double accountInfoParams[]) {
        return brokerAccount.handle(accountInfoParams);
    }

    public int doBrokerBuy(final String instrumentName,
                           final double tradeParams[]) {
        return brokerBuy.openTrade(instrumentName, tradeParams);
    }

    public int doBrokerTrade(final int orderID,
                             final double orderParams[]) {
        return brokerTrade.fillTradeParams(orderID, orderParams);
    }

    public int doBrokerStop(final int orderID,
                            final double newSLPrice) {
        return brokerStop.setSL(orderID, newSLPrice);
    }

    public int doBrokerSell(final int nTradeID,
                            final int nAmount) {
        return brokerSell.closeTrade(nTradeID, nAmount);
    }

    public int doBrokerHistory2(final String instrumentName,
                                final double startDate,
                                final double endDate,
                                final int tickMinutes,
                                final int nTicks,
                                final double tickParams[]) {
        return brokerHistory2.get(instrumentName,
                                  startDate,
                                  endDate,
                                  tickMinutes,
                                  nTicks,
                                  tickParams);
    }

    public int doSetOrderText(final String orderText) {
        ZorroLogger.logError("doSetOrderText for " + orderText + " called but not yet supported!");
        return ZorroReturnValues.BROKER_COMMAND_OK.getValue();
    }
}
