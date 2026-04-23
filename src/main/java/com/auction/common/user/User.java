package com.auction.common.user;

import com.auction.common.auction.Auction;
import com.auction.common.entity.Entity;

public abstract class User extends Entity {
    public User(String _name, String _id){
        super(_name, _id);
    }
}
