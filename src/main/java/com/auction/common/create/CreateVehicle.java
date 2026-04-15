package com.auction.common.create;

import com.auction.common.item.Item;
import com.auction.common.item.Vehicle;

public class CreateVehicle extends CreateItem {
    @Override
    public Item create(String _name, String _id, String _description) {
        return new Vehicle(_name, _id, _description);
    }
}
