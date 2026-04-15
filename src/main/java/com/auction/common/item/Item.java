package com.auction.common.item;

import com.auction.common.entity.Entity;

public abstract class Item extends Entity {
    protected String description;

    Item(String _name, String _id, String _description){
        super(_name, _id);
        description = _description;
    }
}
