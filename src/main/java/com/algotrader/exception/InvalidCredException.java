package com.algotrader.exception;

public class InvalidCredException extends RuntimeException {

    public InvalidCredException() {
        super("Invalid creds");
    }

    public InvalidCredException(String msg) {
        super(msg);
    }
}
