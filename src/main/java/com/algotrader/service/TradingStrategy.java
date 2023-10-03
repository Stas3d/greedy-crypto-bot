package com.algotrader.service;

import com.algotrader.cache.LastTrendCache;
import com.algotrader.util.Constants;
import com.algotrader.util.Converter;

import com.binance.api.client.domain.account.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static com.algotrader.util.Constants.*;
import static java.lang.Double.parseDouble;

@Log
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

    private final ClientOrderService orderService;

    public String getBalanceFor(String currency) {

        return orderService.getFreeBalanceForCurrency(currency);
    }

    public void saveCurrentPrice() {

        String currentPrice = orderService.getCurrentPrice(currentTradePair);
        trendCache.save(parseDouble(currentPrice));
    }

    public void executeLogic() {

        if (!canEnter()) {
            logger.info(WAITING_MSG);
            return;
        }
        List<Order> openOrders = getOpenOrders();

        Order currentOrder = null;
        if (!openOrders.isEmpty()) {
            currentOrder = openOrders.stream()
                    .filter(Objects::nonNull)
                    .filter(Order::isWorking)
                    .filter(order -> Objects.equals(currentTradePair, order.getSymbol()))
                    .findAny().orElseThrow(
                            () -> new RuntimeException(UNEXPECTED_MSG));
        }

        if (isFirstRun) {
            showTime();
            var current = (currentOrder != null) ? currentOrder : ORDER_UNAVAILABLE_MSG;
            logger.info(current.toString());
            isFirstRun = Boolean.FALSE;
        }

        if (panicStrategyService.isThreshold()) {
            logger.info("*** Provide emergency panic sell");
            String orderId = panicSell();
            logger.info("*** EMERGENCY orderId is " + orderId);
            panicStrategyService.initiateAppShutDown();
        }

        if (openOrders.isEmpty() && isEnoughAssetBalance(USDT, enoughAssetBalance)) {
            showTime();
            logger.info(BUY_MSG);
            double enterPrice = findEnterPrice();
            String orderId = createBuyOrder(enterPrice);
            logger.info(BUY_ORDER_MSG + orderId);
            panicStrategyService.discard();

        } else if (openOrders.isEmpty() && isEnoughAssetBalance(currentCoin, Double.parseDouble(buyQuantity))) {
            showTime();
            logger.info(SELL_MSG);
            double enterPrice = findExitPrice();
            String orderId = createSellOrder(enterPrice);
            logger.info(SELL_ORDER_MSG + orderId);

        } else if (currentOrder != null && BUY_ORDER_SIDE.equals(currentOrder.getSide())) {
            if (checkIfNeedExit(currentOrder.getPrice())) {
                cancelOrder(currentOrder.getClientOrderId());
            }

        } else if (currentOrder != null && SELL_ORDER_SIDE.equals(currentOrder.getSide())) {
            if (checkIfNeedExit(currentOrder.getPrice())) {
                panicStrategyService.incrementSellCount();
                cancelOrder(currentOrder.getClientOrderId());
            }

        } else {
            logger.info(UNEXPECTED_MSG + currentOrder);
        }
    }

    public void forceExit() {

        logger.info("*** EMERGENCY EXIT");
        panicStrategyService.initiateAppShutDown();
    }

    public String panicSell() {

        logger.info("*** EMERGENCY SELL " + getAssetBalanceForCurrency(USDT));
        return orderService.createMarketSell(buyQuantity);
    }

    public void cancelCurrentOrder() {

        logger.info("*** Cancel Current Order ***");
        List<Order> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) {
            Order currentOrder = openOrders.get(0);
            cancelOrder(currentOrder.getClientOrderId());
        } else {
            logger.info("*** No Order Found ***");
        }
    }

    public void cancelAllOrders() {

        logger.info("*** Cancel All Orders for trade pair " + currentTradePair + "***");
        List<Order> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) {
            openOrders.forEach(el -> cancelOrder(el.getClientOrderId()));
        } else {
            logger.info("*** No Orders Found ***");
        }
    }

    public String cancelCurrentOrderAndBuy() {

        logger.info("*** Cancel Current Order And Buy");
        List<Order> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) {
            Order currentOrder = openOrders.stream()
                    .findAny()
                    .get();
            cancelOrder(currentOrder.getClientOrderId());
        }
        logger.info("*** Buy for current price order ");
        return orderService.createMarketBuy(buyQuantity);
    }

    private boolean checkIfNeedExit(final String currentPrice) {

        double averagePrice = trendCache.getAverage().getAsDouble();
        double orderPrice = parseDouble(currentPrice);
        return Math.abs((averagePrice - orderPrice) * 100D / averagePrice) > exitLevel;
    }

    private String createBuyOrder(double enterPrice) {

        logger.info(CURRENT_BALANCE_MSG + getAssetBalanceForCurrency(USDT));
        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        logger.info("*** Creating BUY order ");
        return orderService.createLimitBuy(buyQuantity, formatPrice);
    }

    private String createSellOrder(double enterPrice) {

        logger.info(CURRENT_BALANCE_MSG + getAssetBalanceForCurrency(USDT));
        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        logger.info("*** Creating SELL order ");
        return orderService.createLimitSell(buyQuantity, formatPrice);
    }

    private List<Order> getOpenOrders() {

        return orderService.getOpenOrders(currentTradePair);
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

    private boolean isEnoughAssetBalance(final String currency, final Double limit) {

        String value = orderService.getFreeBalanceForCurrency(currency);
        return Double.parseDouble(value) > limit;
    }

    private double getAssetBalanceForCurrency(final String currency) {

        String value = orderService.getFreeBalanceForCurrency(currency);
        return Double.parseDouble(value);
    }

    private void cancelOrder(String clientOrderId) {

        orderService.cancelOrder(currentTradePair, clientOrderId);
    }

    private boolean canEnter() { // TODO REM0VE REDUNDANT

        return trendCache.isComfy();
    }

    private void showTime() {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.PATTERN);
        LocalDateTime now = LocalDateTime.now();
        logger.info("*** " + dtf.format(now));
    }
}
