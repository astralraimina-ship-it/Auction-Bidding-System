package com.auction.common.auction;

import java.util.ArrayList;

public class AuctionManager {
    //    Áp dụng singleton để tạo duy nhất một đối tượng
    private ArrayList<Auction> runningAuctions = new ArrayList<>();
    private ArrayList<Auction> finishedAuctions = new ArrayList<>();

    public static AuctionManager manager = null;

    private AuctionManager(){}

    public AuctionManager getManager() {
        if (manager == null){
            manager = new AuctionManager();
        }
        return manager;
    }
    public synchronized void addAuction(Auction auction){
        runningAuctions.add(auction);
    }
}
