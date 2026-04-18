package com.auction.common.user;

import com.auction.common.auction.Auction;
import com.auction.common.entity.Entity;

public abstract class User extends Entity {
    protected String password;

    public User(String _name, String _id, String _password){
        super(_name, _id);
        password = _password;
    }

}
