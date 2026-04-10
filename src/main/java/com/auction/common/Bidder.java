package com.auction.common;

public class Bidder extends User {
    private double money = 0.0;
    Bidder(String _name, String _id, String _password){
        super(_name, _id, _password);
    }
}
