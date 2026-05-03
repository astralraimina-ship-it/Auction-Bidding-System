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
    private double step;
    private String category;
    private Timestamp endTime;
    private String status;

    public Item(int id, String name, String description, double startPrice, double binPrice, double step, String category, Timestamp endTime, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.binPrice = binPrice;
        this.step = step;
        this.category = category;
        this.endTime = endTime;
        this.status = status;
    }

    // --- GETTERS & SETTERS (Giữ nguyên để JavaFX PropertyValueFactory gọi được) ---
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartPrice() { return startPrice; }
    public double getBinPrice() { return binPrice; }
    public double getStep() { return step; }
    public String getCategory() { return category; }
    public Timestamp getEndTime() { return endTime; }
    public String getStatus() { return status; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    // ... các setter khác giữ nguyên ...

    /**
     * Hàm này để JavaFX TableView gọi qua PropertyValueFactory("itemDetails")
     * Long cần sang các class con (Other, Vehicle, Electronics)
     * để xóa phần "getName()" trong chuỗi return đi là sẽ hết bị trùng.
     */
    public abstract String getItemDetails();
}