package com.algotrader.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class PanicStrategyService {

    private static final int EXIT_CODE = 1;

    private int sellCount = 0;

    @Value("${panic.sell.param}")
    private int panicParam;

    @Autowired
    private ApplicationContext context;

    public void discard() {
        sellCount = 0;
        System.out.println("*** PanicStrategy sellCount discarded : " + sellCount);
    }

    public void incrementSellCount() {
        sellCount++;
        System.out.println("*** PanicStrategy current sellCount : " + sellCount);
    }

    public boolean isThreshold() {
        return sellCount >= panicParam;
    }

    public void initiateAppShutDown() {
        SpringApplication.exit(context, () -> EXIT_CODE);
    }
}
