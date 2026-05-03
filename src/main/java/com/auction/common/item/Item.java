package com.auction.common.item;

import java.io.Serializable;
import java.sql.Timestamp;

public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L; // Đảm bảo đồng bộ khi truyền qua Socket
    private int id;
    private String name;
    private String description;
    private double startPrice;
    private double binPrice;
    private double step;
    private String sellerName;
    private String category;
    private Timestamp endTime;
    private String status;

    public Item(int id, String name, String description, double startPrice, double binPrice,
                double step, String sellerName, String category, Timestamp endTime, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.binPrice = binPrice;
        this.step = step;
        this.sellerName = sellerName;
        this.category = category;
        this.endTime = endTime;
        this.status = status;
    }

    public abstract String getItemDetails();

    // Getters và Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartPrice() { return startPrice; }
    public double getBinPrice() { return binPrice; }
    public double getStep() { return step; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getCategory() { return category; }
    public Timestamp getEndTime() { return endTime; }
    public String getStatus() { return status; }
}