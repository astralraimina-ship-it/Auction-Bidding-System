package com.auction.common.item;

import java.io.Serializable;

public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private double startPrice;
    private double binPrice;
    private double step;
    private String category;

    public Item(int id, String name, String description, double startPrice, double binPrice, double step, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.binPrice = binPrice;
        this.step = step;
        this.category = category;
    }

    // Getters & Setters đầy đủ
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    public double getBinPrice() { return binPrice; }
    public void setBinPrice(double binPrice) { this.binPrice = binPrice; }

    public double getStep() { return step; }
    public void setStep(double step) { this.step = step; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public abstract String getItemDetails();
}