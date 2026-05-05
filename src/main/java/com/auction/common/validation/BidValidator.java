package com.auction.common.validation;

public final class BidValidator {
    private BidValidator() {
    }

    public static void validateBid(double bidAmount, double currentPrice, boolean auctionOpen) {
        if (!auctionOpen) {
            throw new IllegalStateException("Auction is closed");
        }

        if (bidAmount <= currentPrice) {
            throw new IllegalArgumentException("Bid amount must be greater than current price");
        }
    }
}