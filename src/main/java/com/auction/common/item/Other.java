package com.auction.common.item;

import java.sql.Timestamp;

public class Other extends Item {
    private static final long serialVersionUID = 1L;

    // ĐÃ SỬA: Thêm double currentPrice vào constructor và super()
    public Other(int id, String name, String description, double startPrice, double binPrice, double currentPrice, double step,
                 String sellerName, Timestamp endTime, String status, String paymentStatus, double winPrice) {
        super(id, name, description, startPrice, binPrice, currentPrice, winPrice, step, sellerName, "OTHER", endTime, status, paymentStatus);
    }

    @Override
    public String getItemDetails() {
        return String.format("Giá khởi điểm: %,.0f VNĐ | Bước giá: %,.0f VNĐ", getStartPrice(), getStep());
    }
}