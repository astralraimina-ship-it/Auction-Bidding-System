package com.auction.common.item;

import java.sql.Timestamp;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private int modelYear;
    private String engineType;
    private String state;
    private int age;
    private double mileage;

    // ĐÃ SỬA: Thêm double currentPrice vào constructor và super()
    public Vehicle(int id, String name, String description, double startPrice, double binPrice, double currentPrice, double step,
                   String sellerName, Timestamp endTime, String status, String paymentStatus,
                   String brand, int modelYear, String engineType, String state, int age, double mileage, double winPrice) {
        super(id, name, description, startPrice, binPrice, currentPrice, winPrice, step, sellerName, "VEHICLE", endTime, status, paymentStatus);
        this.brand = brand;
        this.modelYear = modelYear;
        this.engineType = engineType;
        this.state = state;
        this.age = age;
        this.mileage = mileage;
    }

    @Override
    public String getItemDetails() {
        return String.format("Hãng: %s | Đời: %d | Tình trạng: %s | ODO: %,.1f km", brand, modelYear, state, mileage);
    }

    public String getEngineType() { return engineType; }
    public int getAge() { return age; }
    public String getBrand() { return brand; }
    public int getModelYear() { return modelYear; }
    public String getState() { return state; }
    public double getMileage() { return mileage; }
}