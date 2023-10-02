package com.algotrader.service;

import com.algotrader.cache.LastTrendCache;
import com.algotrader.util.Constants;
import com.algotrader.util.Converter;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.algotrader.util.Constants.*;
import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.limitSell;
import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;
import static java.lang.Double.parseDouble;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingStrategy {

    private boolean isFirstRun = Boolean.TRUE;
    @Value("${current.trade.pair}")
    private String currentTradePair;
    @Value("${current.coin}")
    private String currentCoin;
    @Value("${exit.strategy.percentage}")
    private double exitLevel;
    @Value("${enter.price}")
    private double enterPriceParam;
    @Value("${exit.price}")
    private double exitPriceParam;
    @Value("${buy.quantity}")
    private String buyQuantity;

    @Value("${enough.asset.balance}")
    private double enoughAssetBalance;

    private final LastTrendCache trendCache;

    private final PanicStrategyService panicStrategyService;

    private final OrderClientService restClient;

    @PostConstruct
    public void postConstruct() {

        logger.info("*** Asset Balance for BTC = "
                + restClient.getAcc().getAssetBalance(currentCoin));
        System.out.println("*** Balance for USDT = "
                + getBalanceForCurrency(restClient.getAcc().getAssetBalance(USDT)));
    }


    public double getBalance(String currency) {

        return getBalanceForCurrency(restClient.getAcc().getAssetBalance(currency));
    }

    public void saveCurrentPrice() {

        String currentPrice = restClient.getCurrentPrice(currentTradePair);
        trendCache.save(parseDouble(currentPrice));
    }

    public void executeLogic() {
        if (!canEnter()) {
            System.out.println(WAITING_MSG);
            return;
        }
        List<Order> openOrders = getOpenOrders();
        Order currentOrder = null;
        if (!openOrders.isEmpty()) {
            currentOrder = openOrders.get(0);
        }

        if (isFirstRun) {
            showTime();
            System.out.println((currentOrder != null) ? currentOrder : ORDER_UNAVAILABLE_MSG);
            isFirstRun = false;
        }

        if (panicStrategyService.isThreshold()) {
            System.out.println("*** EMERGENCY PANIC SELL");
            String orderId = panicSell();
            System.out.println("*** EMERGENCY orderId is " + orderId);
            panicStrategyService.initiateAppShutDown();
        }

        if (openOrders.isEmpty() && isEnoughAssetBalance(USDT, enoughAssetBalance)) {
            showTime();
            System.out.println(BUY_MSG);
            double enterPrice = findEnterPrice();
            String orderId = createBuyOrder(enterPrice);
            System.out.println(BUY_ORDER_MSG + orderId);
            panicStrategyService.discard();
        } else if (openOrders.isEmpty() && isEnoughAssetBalance(currentCoin, Double.parseDouble(buyQuantity))) {
            showTime();
            System.out.println(SELL_MSG);
            double enterPrice = findExitPrice();
            String orderId = createSellOrder(enterPrice);
            //System.out.println(SELL_ORDER_MSG + orderId);
        } else if (currentOrder != null && OrderSide.BUY.equals(currentOrder.getSide())) {
            if (checkIfNeedExit(currentOrder.getPrice())) {
                cancelOrder(currentOrder.getClientOrderId());
            }
        } else if (currentOrder != null && OrderSide.SELL.equals(currentOrder.getSide())) {
            if (checkIfNeedExit(currentOrder.getPrice())) {
                panicStrategyService.incrementSellCount();
                cancelOrder(currentOrder.getClientOrderId());
            }
        } else {
            System.out.println(UNEXPECTED_MSG + currentOrder);
        }
    }

    public void forceExit() {

        panicStrategyService.initiateAppShutDown();
    }

    public String panicSell() {
        System.out.println("*** EMERGENCY SELL " + getAssetBalanceForCurrency(USDT));
        NewOrderResponse newOrderResponse = restClient.createOrder(marketSell(currentTradePair, buyQuantity));
        System.out.println("*** SELL EMERGENCY SELL order created with status : " + newOrderResponse.getStatus() +
                " with price : " + newOrderResponse.getPrice());
        return newOrderResponse.getClientOrderId();
    }

    public void cancelCurrentOrder() {
        System.out.println("*** Cancel Current Order ***");
        List<Order> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) {
            Order currentOrder = openOrders.get(0);
            cancelOrder(currentOrder.getClientOrderId());
        } else {
            System.out.println("*** No Order Found ***");
        }
    }

    public void cancelAllOrders() {
        System.out.println("*** Cancel All Orders for trade pair " + currentTradePair + "***");
        List<Order> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) {
            openOrders.forEach(el -> cancelOrder(el.getClientOrderId()));
        } else {
            System.out.println("*** No Orders Found ***");
        }
    }

    public String cancelCurrentOrderAndBuy() {

        System.out.println("*** Cancel Current Order And Buy");
        List<Order> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) {
            Order currentOrder = openOrders.get(0);
            cancelOrder(currentOrder.getClientOrderId());
        }
        NewOrderResponse newOrderResponse = restClient.createOrder(marketBuy(currentTradePair, buyQuantity));
        System.out.println("*** Buy for current price order created with status : " + newOrderResponse.getStatus() +
                " with price : " + newOrderResponse.getPrice());
        return newOrderResponse.getClientOrderId();
    }

    private boolean checkIfNeedExit(final String currentPrice) {

        double averagePrice = trendCache.getAverage().getAsDouble();
        double orderPrice = parseDouble(currentPrice);
        return Math.abs((averagePrice - orderPrice) * 100D / averagePrice) > exitLevel;
    }

    private String createBuyOrder(double enterPrice) {

        System.out.println(CURRENT_BALANCE_MSG + getAssetBalanceForCurrency(USDT));
        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        NewOrderResponse newOrderResponse = restClient.createOrder(
                limitBuy(currentTradePair, TimeInForce.GTC, buyQuantity, formatPrice));
        System.out.println("*** Order status : " + newOrderResponse.getStatus());
        if (newOrderResponse.getStatus() == OrderStatus.REJECTED) {
            System.out.println("*** newOrderResponse REJECTED ");
        }
        System.out.println("*** BUY order created with status : " + newOrderResponse.getStatus() +
                " with price : " + newOrderResponse.getPrice());
        return newOrderResponse.getClientOrderId();
    }

    private String createSellOrder(double enterPrice) {

        System.out.println(CURRENT_BALANCE_MSG + getAssetBalanceForCurrency(USDT));
        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        NewOrderResponse newOrderResponse = restClient.createOrder(
                limitSell(currentTradePair, TimeInForce.GTC, buyQuantity, formatPrice));
        System.out.println("*** SELL order created with status : " + newOrderResponse.getStatus() +
                "with price : " + newOrderResponse.getPrice());
        return newOrderResponse.getClientOrderId();
    }

    private List<Order> getOpenOrders() {

        return restClient.getOpenOrders(currentTradePair);
    }

    private double findEnterPrice() {

        double enterPrice = trendCache.getAverage().getAsDouble();
        enterPrice *= enterPriceParam;
        return enterPrice;
    }

    private double findExitPrice() {

        double exitPrice = trendCache.getAverage().getAsDouble();
        exitPrice *= exitPriceParam;
        return exitPrice;
    }

    private boolean isEnoughAssetBalance(String currency, Double limit) {

        String value = restClient.getAcc().getAssetBalance(currency).getFree();
        return Double.parseDouble(value) > limit;
    }

    private double getAssetBalanceForCurrency(String currency) {

        String value = restClient.getAcc().getAssetBalance(currency).getFree();
        return Double.parseDouble(value);
    }

    private void cancelOrder(String clientOrderId) {

        CancelOrderResponse orderResponse = restClient.cancelOrder(currentTradePair, clientOrderId);
        System.out.println(ORDER_CANCELLED_MSG + orderResponse.getStatus());
    }

    private boolean canEnter() { // TODO REM0VE REDUNDANT

        return trendCache.isComfy();
    }

    private double getBalanceForCurrency(AssetBalance balance) {

        return parseDouble(balance.getFree()) + parseDouble(balance.getLocked());
    }

    private void showTime() {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.PATTERN);
        LocalDateTime now = LocalDateTime.now();
        System.out.println("*** " + dtf.format(now));
    }
}
