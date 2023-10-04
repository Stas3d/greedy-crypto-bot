package com.algotrader.exception;

public class ApiException extends RuntimeException {

    public ApiException(String msg) {
        super(msg);
    }

    public ApiException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
