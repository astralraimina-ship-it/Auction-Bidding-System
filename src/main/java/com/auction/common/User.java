package com.auction.common;

public abstract class User extends Entity {
    protected String password;

    User(String _name, String _id, String _password){
        super(_name, _id);
        password = _password;
    }
}
