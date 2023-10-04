package com.algotrader.controller;

import com.algotrader.service.PanicStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommonController {

    private final PanicStrategyService service;

    @RequestMapping(method = RequestMethod.POST, value = "/exit")
    public ResponseEntity<String> exit() {
        service.initiateAppShutDown();
        return ResponseEntity.ok("EXIT");
    }
}
