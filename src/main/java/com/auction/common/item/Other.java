package com.auction.common.item;

import java.sql.Timestamp;

public class Other extends Item {
    private static final long serialVersionUID = 1L;

    public Other(int id, String name, String description, double startPrice, double binPrice, double step, Timestamp endTime, String status) {
        super(id, name, description, startPrice, binPrice, step, "OTHER", endTime, status);
    }

    @Override
    public String getItemDetails() {
        // Chỉ hiện giá và mô tả, không hiện lại Tên để tránh trùng lặp
        return String.format("Giá khởi điểm: %,.0f VNĐ | Bước giá: %,.0f VNĐ", getStartPrice(), getStep());
    }
}