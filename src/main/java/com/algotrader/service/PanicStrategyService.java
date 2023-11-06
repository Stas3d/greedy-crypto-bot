package com.algotrader.service;

import lombok.extern.java.Log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Log
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
        logger.info(">>> PanicStrategy sellCount discarded : " + sellCount);
    }

    public void incrementSellCount() {

        sellCount++;
        logger.info(">>> PanicStrategy current sellCount : " + sellCount);
    }

    public boolean isThreshold() {

        return sellCount >= panicParam;
    }

    public void initiateAppShutDown() {

        logger.warning(">>> Application shutDown executing ... ");
        SpringApplication.exit(context, () -> EXIT_CODE);
    }
}
