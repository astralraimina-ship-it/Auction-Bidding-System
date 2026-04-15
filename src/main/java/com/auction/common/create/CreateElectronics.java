package com.auction.common.create;

import com.auction.common.item.Electronics;
import com.auction.common.item.Item;

public class CreateElectronics extends CreateItem {
    @Override
    public Item create(String _name, String _id, String _description) {
        return new Electronics(_name, _id, _description);
    }
}
