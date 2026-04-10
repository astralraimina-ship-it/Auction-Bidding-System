package com.auction.common;

import java.util.ArrayList;

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
