package com.auction.common.item;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private String warranty;
    private String state;

    public Electronics(int id, String name, String description, double startPrice, double binPrice, double step, String brand, String warranty, String state) {
        super(id, name, description, startPrice, binPrice, step, "ELECTRONICS");
        this.brand = brand;
        this.warranty = warranty;
        this.state = state;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getWarranty() { return warranty; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    @Override
    public String getItemDetails() { return brand + " - " + getName() + " (" + state + ")"; }
}