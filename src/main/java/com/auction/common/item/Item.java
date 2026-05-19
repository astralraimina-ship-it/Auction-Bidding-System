package com.auction.common.item;

import java.io.Serializable;
import java.sql.Timestamp;

public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private double startPrice;
    private double binPrice;

    // TÁCH BIỆT RÕ RÀNG:
    private double currentPrice; // Giá đấu hiện tại (đang diễn ra)
    private double winPrice;     // Giá chốt thắng cuộc (sau khi kết thúc)

    private double step;
    private String sellerName;
    private String category;
    private Timestamp endTime;
    private String status;
    private String paymentStatus;

    public Item(int id, String name, String description, double startPrice, double binPrice,
                double currentPrice, double winPrice, double step, String sellerName,
                String category, Timestamp endTime, String status, String paymentStatus) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.binPrice = binPrice;
        this.currentPrice = currentPrice;
        this.winPrice = winPrice; // Khởi tạo giá thắng
        this.step = step;
        this.sellerName = sellerName;
        this.category = category;
        this.endTime = endTime;
        this.status = status;
        this.paymentStatus = paymentStatus;
    }

    public abstract String getItemDetails();

    // Getters & Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartPrice() { return startPrice; }
    public double getBinPrice() { return binPrice; }

    // Giá hiện hành (đang đấu)
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    // Giá chốt thắng cuộc
    public double getWinPrice() { return winPrice; }
    public void setWinPrice(double winPrice) { this.winPrice = winPrice; }

    public double getStep() { return step; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getCategory() { return category; }
    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}