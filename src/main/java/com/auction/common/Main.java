package com.auction.common;

import com.auction.common.auction.Auction;
import com.auction.common.item.Electronics;
import com.auction.common.item.Item;
import com.auction.common.item.Vehicle;
import com.auction.common.user.Bidder;
import com.auction.common.user.Seller;

import java.time.LocalTime;
import java.util.Scanner;

public class Main {
    public static void main(String[] args){
//        Test tính năng đấu giá
        Item car = new Vehicle("Ferrari", "C001", "Best car ever");
        LocalTime closedTime = LocalTime.parse("12:10");
        Seller seller = new Seller("New Seller", "S001", "10101010");
        Bidder bidder1 = new Bidder("Bidder1", "B001", "10000001");
        Bidder bidder2 = new Bidder("Bidder2", "B002", "10000002");
        Bidder bidder3 = new Bidder("Bidder3", "B003", "10000003");

        Auction auction = seller.createAuction(car, 10000, closedTime);
        bidder1.addAmount(100000);
        bidder2.addAmount(125000);
        bidder3.addAmount(75000);

        while(closedTime.compareTo(LocalTime.now()) > 0){
            bidder1.bid(auction);
            bidder2.bid(auction);
            bidder3.bid(auction);
        }
        auction.closed();
    }
}
