package com.auction.common.exception;

public class InvalidBINPriceException extends RuntimeException {
    public InvalidBINPriceException(String message) {
        super(message);
    }
}
