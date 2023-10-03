package com.algotrader.service;

import com.algotrader.exception.InvalidCredException;
import com.algotrader.exception.NoCredsException;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderRequest;

import lombok.NonNull;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.algotrader.util.Constants.*;
import static com.binance.api.client.domain.account.NewOrder.*;

@Log
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

        logger.info("*** GRID BOT 2023 *** \n Current settings : currentTradePair = " + currentTradePair +
                "; \nenterPriceParam + " + enterPriceParam
                + "; \nexitPriceParam = " + exitPriceParam
                + "; \nexit level percentage = " + exitLevel + "%;");

        initCheckPing();
    }

    private void initCheckPing() {

        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            throw new NoCredsException();
        }

        restClient = BinanceApiClientFactory
                .newInstance(apiKey, apiSecret)
                .newRestClient();

        restClient.ping();

        Account account;
        try {
            account = getAcc();
        } catch (com.binance.api.client.exception.BinanceApiException binanceApiException) {
            logger.warning("Signature is not valid, check credentials");
            throw new InvalidCredException();
        }
        logger.info("*** Asset Balance for BTC = " + account.getAssetBalance(BTC));
        logger.info("*** Asset Balance for USDT = " + account.getAssetBalance(USDT));
    }

    public String createMarketBuy(String quantity) {

        return createOrder(marketBuy(currentTradePair, quantity));
    }

    public String createMarketSell(String quantity) {

        return createOrder(marketSell(currentTradePair, quantity));
    }

    public String createLimitBuy(String quantity,
                                 String price) {

        return createOrder(limitBuy(currentTradePair, TimeInForce.GTC, quantity, price));
    }

    public String createLimitSell(String quantity,
                                  String price) {

        return createOrder(limitSell(currentTradePair, TimeInForce.GTC, quantity, price));
    }

    public AssetBalance getBalanceForCurrency(@NonNull String currency) {

        return restClient.getAccount().getAssetBalance(currency);
    }

    public String getCurrentPrice(String currentTradePair) {

        return restClient.getPrice(currentTradePair).getPrice();
    }

    public List<Order> getOpenOrders(String currentTradePair) {

        OrderRequest orderRequest = new OrderRequest(currentTradePair);
        return restClient.getOpenOrders(orderRequest);
    }

    public String cancelOrder(String currentTradePair, String clientOrderId) {

        CancelOrderRequest orderRequest = new CancelOrderRequest(currentTradePair, clientOrderId);
        var orderResponse = restClient.cancelOrder(orderRequest);
        logger.info(ORDER_CANCELLED_MSG + orderResponse.getStatus());
        return orderResponse.getStatus();
    }

    private String createOrder(NewOrder order) {

        NewOrderResponse newOrderResponse = restClient.newOrder(order);

        logger.info("*** New Order status : " + newOrderResponse.getStatus());
        if (newOrderResponse.getStatus() == OrderStatus.REJECTED) {
            logger.info("*** newOrderResponse REJECTED ");
        }

        logger.info("Order created with status : " + newOrderResponse.getStatus() +
                " with price : " + newOrderResponse.getPrice());

        return newOrderResponse.getClientOrderId();
    }

    private Account getAcc() {

        return restClient.getAccount();
    }
}
