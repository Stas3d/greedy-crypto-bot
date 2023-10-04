package com.algotrader.service;

import com.algotrader.cache.LastTrendCache;
import com.algotrader.dto.ClientBalance;
import com.algotrader.dto.ClientOrder;
import com.algotrader.mapper.OrdersMapper;
import com.algotrader.util.Constants;
import com.algotrader.util.Converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private final OrdersMapper ordersMapper;


    public ClientBalance getAssetBalanceFor(String currency) {

        return orderService.getBalanceForCurrency(currency);
    }

    public void saveCurrentPrice() {

        String currentPrice = orderService.getCurrentPrice(currentTradePair);
        trendCache.save(parseDouble(currentPrice));
    }

    public void execute() {

        if (!canEnter()) {
            logger.info(WAITING_MSG);
            return;
        }
        List<ClientOrder> openOrders = getOpenOrders();

        ClientOrder currentOrder = null;
        if (!openOrders.isEmpty()) {

            currentOrder = openOrders.stream()
                    .filter(Objects::nonNull)
                    .filter(ClientOrder::isWorking)
                    .filter(order -> Objects.equals(currentTradePair, order.symbol()))
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

        } else if (currentOrder != null && BUY_ORDER_SIDE.equals(currentOrder.side())) {

            if (checkIfNeedExit(currentOrder.price())) {
                cancelOrder(currentOrder.clientOrderId());
            }

        } else if (currentOrder != null && SELL_ORDER_SIDE.equals(currentOrder.side())) {

            if (checkIfNeedExit(currentOrder.price())) {
                panicStrategyService.incrementSellCount();
                cancelOrder(currentOrder.clientOrderId());
            }

        } else {
            logger.info(UNEXPECTED_MSG + currentOrder);
        }
    }

    public String panicSell() {

        logger.info("*** EMERGENCY SELL " + getAssetBalanceForCurrency(USDT));
        return orderService.createMarketSell(buyQuantity);
    }

    public void cancelCurrentOrders() {

        logger.info("*** Cancel All Current Orders ***");
        List<ClientOrder> openOrders = getOpenOrders();
        if (!openOrders.isEmpty()) {
            openOrders.forEach(o ->
                    cancelOrder(o.clientOrderId()));
        } else {
            logger.info("*** No Orders Found ***");
        }
    }

    private void cancelOrder(String clientOrderId) {

        orderService.cancelOrder(currentTradePair, clientOrderId);
    }

    private boolean checkIfNeedExit(final String currentPrice) {

        double averagePrice = trendCache.getAverage().getAsDouble();
        double orderPrice = parseDouble(currentPrice);
        return Math.abs((averagePrice - orderPrice) * 100D / averagePrice) > exitLevel;
    }

    private String createBuyOrder(double enterPrice) {

        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        logger.info("*** Creating BUY order ");
        return orderService.createLimitBuy(buyQuantity, formatPrice);
    }

    private String createSellOrder(double enterPrice) {

        String formatPrice = Converter.convertToStringDecimal(enterPrice);
        logger.info("*** Creating SELL order ");
        return orderService.createLimitSell(buyQuantity, formatPrice);
    }

    private List<ClientOrder> getOpenOrders() {

        return orderService.getOpenOrders(currentTradePair)
                .stream()
                .map(ordersMapper::toRecord)
                .collect(Collectors.toList());
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

        String value = getAssetBalanceFor(currency).free();
        return Double.parseDouble(value) > limit;
    }

    private double getAssetBalanceForCurrency(final String currency) {

        String value = getAssetBalanceFor(currency).free();
        return Double.parseDouble(value);
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
