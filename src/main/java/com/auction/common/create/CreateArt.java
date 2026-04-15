package com.auction.common.create;

import com.auction.common.item.Art;
import com.auction.common.item.Item;

public class CreateArt extends CreateItem {
    @Override
    public Item create(String _name, String _id, String _description) {
        return new Art(_name, _id, _description);
    }
}
