package com.algotrader.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledService {

    private final TradingStrategy strategy;

    @Autowired
    public ScheduledService(TradingStrategy strategy) {
        this.strategy = strategy;
    }

    @Scheduled(cron = "${cron.expression}")
    public void scheduled() {

        strategy.saveCurrentPrice();
        strategy.executeLogic();
    }
}
