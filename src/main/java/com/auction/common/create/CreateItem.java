package com.auction.common.create;

import com.auction.common.item.Item;

public abstract class CreateItem {
//    Áp dụng factory method để tạo Item
    public abstract Item create(String _name, String _id, String _description);
}
