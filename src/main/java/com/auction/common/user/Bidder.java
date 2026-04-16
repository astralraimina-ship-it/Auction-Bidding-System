package com.auction.common.user;

import com.auction.common.auction.*;
import com.auction.common.interfaces.Transaction;

import java.time.LocalTime;

public class Bidder extends User implements Transaction {
    private double money = 0.0;
    Bidder(String _name, String _id, String _password){
        super(_name, _id, _password);
    }

    public void bid(Auction auction, double price){
        if (price < money){
            auction.update(this, price);
        }
    }

    @Override
    public void addAmount(double _money) {
        money += _money;
    }

    @Override
    public void subtract(double _money) {
        money -= _money;
    }
    public Bidder getBidder() {
        return Bidder;
    }
}
