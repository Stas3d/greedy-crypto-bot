package com.algotrader.mapper;

import com.algotrader.dto.ClientOrder;
import com.binance.api.client.domain.account.Order;

import org.springframework.stereotype.Component;

@Component
public class OrdersMapper {

    public ClientOrder toRecord(Order order) {
        return new ClientOrder(order.getClientOrderId(),
                order.getSide().toString(),
                order.getPrice(),
                order.getSymbol(),
                order.isWorking());
    }
}
