package com.algotrader.exception;

public class NoCredentialsException extends RuntimeException{

    public NoCredentialsException(){
        super("No credentials provided");
    }
}
