package com.algotrader.controller;

import com.algotrader.dto.Balance;
import com.algotrader.service.TradingStrategy;
import com.algotrader.util.Converter;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ManualController {

    private final TradingStrategy strategy;

    @RequestMapping(method = RequestMethod.GET, value = "/balance/{currency}")
    public Balance getCurrentBalance(@PathVariable String currency) {
        return new Balance(currency, Converter.convertToStringDecimal(strategy.getBalanceFor(currency)));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/manual/cancel-current-order")
    public void cancelCurrent() {
        strategy.cancelCurrentOrder();
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
