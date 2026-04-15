package com.auction.common.user;

import com.auction.common.auction.Auction;
import com.auction.common.interfaces.Transaction;
import com.auction.common.item.Item;

import java.time.LocalTime;
import java.util.ArrayList;

public class Seller extends User implements Transaction {
    private double money = 0.0;
    private ArrayList<Item> items = new ArrayList<>();
    private ArrayList<Auction> auctions = new ArrayList<>();

    Seller(String _name, String _id, String _password){
        super(_name, _id, _password);
    }

//    tạo phiên đấu giá mới
    public void sell(Item item, double startPrice, LocalTime closedTime){
        Auction auction = new Auction(this, item, startPrice, closedTime);
    }

    @Override
    public void addAmount(double _money) {
        money += _money;
    }

    @Override
    public void subtract(double _money) {
        money -= _money;
    }
}
