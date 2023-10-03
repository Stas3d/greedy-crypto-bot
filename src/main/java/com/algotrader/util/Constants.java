package com.algotrader.util;

import com.binance.api.client.domain.OrderSide;

public class Constants {

    public static final String SELL_ORDER_SIDE = OrderSide.SELL.name();
    public static final String BUY_ORDER_SIDE = OrderSide.BUY.name();

    public static final String PATTERN = "yyyy/MM/dd HH:mm:ss";
    public static final String BTC = "BTC";
    public static final String USDT = "USDT";
    public static final String ORDER_UNAVAILABLE_MSG = "No current order available";
    public static final String UNEXPECTED_MSG = "*** unexpected case for order ...";
    public static final String BUY_MSG = "*** BUY ";
    public static final String SELL_MSG = "*** SELL ";
    public static final String WAITING_MSG = "*** Waiting till enough data points ... ";
    public static final String BUY_ORDER_MSG = "*** BUY order created with orderId : ";
    public static final String SELL_ORDER_MSG = "*** SELL order created with orderId : ";
    public static final String CURRENT_BALANCE_MSG = "### Current USDT balance : ";
    public static final String ORDER_CANCELLED_MSG = "*** Order Canceled with status : ";
}
