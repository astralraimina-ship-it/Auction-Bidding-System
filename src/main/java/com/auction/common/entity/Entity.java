package com.auction.common.entity;

public abstract class Entity {
    protected String id;
    protected String name;

    public Entity(String _name, String _id){
        name = _name;
        id = _id;
    }

    public String getName(){
        return name;
    }

    public String getId() {
        return id;
    }
}
