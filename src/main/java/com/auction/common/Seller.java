package com.auction.common;

import java.util.ArrayList;

public class Seller extends User {
    private double money = 0.0;
    private ArrayList<Item> items = new ArrayList<>();

    Seller(String _name, String _id, String _password){
        super(_name, _id, _password);
    }
}
