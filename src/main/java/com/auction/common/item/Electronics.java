package com.auction.common.item;

import java.sql.Timestamp;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String warranty;
    private String state;

    // ĐÃ SỬA: Thêm double currentPrice vào constructor và super()
    public Electronics(int id, String name, String description, double startPrice, double winPrice, double binPrice, double currentPrice, double step,
                       String sellerName, Timestamp endTime, String status, String paymentStatus,
                       String brand, String warranty, String state) {
        super(id, name, description, startPrice, binPrice, currentPrice, winPrice, step, sellerName,  "ELECTRONICS", endTime, status, paymentStatus);
        this.brand = brand;
        this.warranty = warranty;
        this.state = state;
    }

    @Override
    public String getItemDetails() {
        return String.format("Hãng: %s | Bảo hành: %s | Tình trạng: %s", brand, warranty, state);
    }

    public String getBrand() { return brand; }
    public String getWarranty() { return warranty; }
    public String getState() { return state; }
}