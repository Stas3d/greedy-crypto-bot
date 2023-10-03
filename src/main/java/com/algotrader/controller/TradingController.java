package com.algotrader.controller;

import com.algotrader.dto.ClientBalance;
import com.algotrader.service.TradingStrategy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TradingController {

    private final TradingStrategy strategy;

    @RequestMapping(method = RequestMethod.GET, value = "/balance/asset/{currency}")
    public ResponseEntity<ClientBalance> getCurrentAssetBalance(@PathVariable String currency) {
        return ResponseEntity.ok(
                new ClientBalance(currency, strategy.getAssetBalanceFor(currency)));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/balance/free/{currency}")
    public ResponseEntity<ClientBalance> getCurrentFreeBalance(@PathVariable String currency) {
        return ResponseEntity.ok(
                new ClientBalance(currency, strategy.getFreeBalanceFor(currency)));
    }

    @RequestMapping(method = RequestMethod.POST, value = "orders/cancel-all")
    public void cancelCurrent() {
        strategy.cancelCurrentOrders();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/manual/cancel-current-order-and-buy")
    public void cancelCurrentAndBuy() {
        strategy.cancelCurrentOrderAndBuy();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/manual/panic-sell")
    public void panicSell() {
        strategy.panicSell();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/manual/panic-sell-and-exit")
    public void panicSellAndExit() {
        strategy.panicSell();
        strategy.forceExit();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/manual/just-exit")
    public void justExit() {
        strategy.forceExit();
    }
}
