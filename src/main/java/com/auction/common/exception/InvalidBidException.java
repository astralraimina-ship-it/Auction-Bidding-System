package com.auction.common.exception;

public class InvalidBidException extends Exception{
    @Override
    public String getMessage() {
        return "Can't bid lower than current price";
    }
}
