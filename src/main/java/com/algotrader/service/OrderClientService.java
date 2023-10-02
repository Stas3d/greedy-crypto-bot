package com.algotrader.service;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.account.request.OrderRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.algotrader.util.Constants.USDT;

@Slf4j
@Component
public class OrderClientService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.secret}")
    private String apiSecret;

    @Value("${current.trade.pair}")
    private String currentTradePair;

    @Value("${enter.price}")
    private double enterPriceParam;

    @Value("${exit.price}")
    private double exitPriceParam;

    @Value("${exit.strategy.percentage}")
    private double exitLevel;

    private BinanceApiRestClient restClient;

    @PostConstruct
    public void postConstruct() {
        logger.warn("*** *** *** GRID BOT *** *** ***");
        logger.info("*** *** *** GRID BOT *** *** ***");
        logger.debug("*** Current settings : currentTradePair = " + currentTradePair +
                "; enterPriceParam + " + enterPriceParam
                + "; exitPriceParam = " + exitPriceParam
                + "; exit level percentage = " + exitLevel + "%;");

        restClient = BinanceApiClientFactory
                .newInstance(apiKey, apiSecret)
                .newRestClient();

        restClient.ping();

        //  System.out.println("*** Asset Balance for BTC = " + restClient.getAccount().getAssetBalance(currentCoin));
        System.out.println("*** Asset Balance for USDT = " + restClient.getAccount().getAssetBalance(USDT));
        System.out.println("*** *** *** *** *** *** *** *** *** *** *** ");
        // System.out.println("*** Balance for USDT = " + getBalanceForCurrency(restClient.getAccount().getAssetBalance(USDT)));
    }

    public Account getAcc() {

        return restClient.getAccount();
    }

    public NewOrderResponse createOrder(NewOrder order) {

        return restClient.newOrder(order);
    }

    public String getCurrentPrice(String currentTradePair) {

        return restClient.getPrice(currentTradePair).getPrice();
    }

    public List<Order> getOpenOrders(String currentTradePair) {

        OrderRequest orderRequest = new OrderRequest(currentTradePair);
        return restClient.getOpenOrders(orderRequest);
    }

    public CancelOrderResponse cancelOrder(String currentTradePair, String clientOrderId) {

        CancelOrderRequest orderRequest = new CancelOrderRequest(currentTradePair, clientOrderId);
        return restClient.cancelOrder(orderRequest);
    }
}
