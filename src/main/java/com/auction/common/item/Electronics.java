package com.auction.common.item;

import java.sql.Timestamp;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String warranty;
    private String state;

    public Electronics(int id, String name, String description, double startPrice, double binPrice, double step,
                       String sellerName, Timestamp endTime, String status,
                       String brand, String warranty, String state) {
        super(id, name, description, startPrice, binPrice, step, sellerName, "ELECTRONICS", endTime, status);
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