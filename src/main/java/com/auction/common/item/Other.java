package com.auction.common.item;

import java.sql.Timestamp;

public class Other extends Item {
    private static final long serialVersionUID = 1L;

    public Other(int id, String name, String description, double startPrice, double binPrice, double step,
                 String sellerName, Timestamp endTime, String status) {
        super(id, name, description, startPrice, binPrice, step, sellerName, "OTHER", endTime, status);
    }

    @Override
    public String getItemDetails() {
        return String.format("Giá khởi điểm: %,.0f VNĐ | Bước giá: %,.0f VNĐ", getStartPrice(), getStep());
    }
}