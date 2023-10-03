package com.algotrader.exception;

public class NoCredsException extends RuntimeException{

    public NoCredsException(){
        super("no creds provided");
    }
}
