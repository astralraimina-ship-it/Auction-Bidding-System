package com.auction.common.auction;

public class AuctionManager {
    //    Áp dụng singleton để tạo duy nhất một đối tượng
    private AuctionManager manager = null;

    private AuctionManager(){}

    public AuctionManager getManager() {
        if (manager == null){
            manager = new AuctionManager();
        }
        return manager;
    }
}
