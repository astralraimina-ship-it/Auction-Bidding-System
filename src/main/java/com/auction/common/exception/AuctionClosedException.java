package com.auction.common.exception;

public class AuctionClosedException extends Exception{
    @Override
    public String getMessage() {
        return "This auction is closed";
    }
}
