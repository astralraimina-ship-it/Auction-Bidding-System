package com.auction.common;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.item.Electronics;
import com.auction.common.item.Item;
import com.auction.common.user.Bidder;
import com.auction.common.user.Seller;

public class Main {
    public static void main(String[] args){
        try{
            Seller seller = new Seller("Tom", "2","farmer");
            Bidder bidder1 = new Bidder("Jerry","3","worker");
            Item Iphone = new Electronics("Iphone 18","3618","Apple");
        }catch (InvalidBidException e){
            System.out.println("Can't bid lower than current price");
        }catch (AuctionClosedException e){
            System.out.println("This auction is closed");
        }
    }
}
