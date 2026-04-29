package com.auction.common.item;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    private String brand;
    private int modelYear;
    private String engineType;
    private String state;
    private int age;
    private double mileage;

    public Vehicle(int id, String name, String description, double startPrice, double binPrice, double step,
                   String brand, int modelYear, String engineType, String state, int age, double mileage) {
        super(id, name, description, startPrice, binPrice, step, "VEHICLE");
        this.brand = brand;
        this.modelYear = modelYear;
        this.engineType = engineType;
        this.state = state;
        this.age = age;
        this.mileage = mileage;
    }

    // Getters & Setters
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getModelYear() { return modelYear; }
    public void setModelYear(int modelYear) { this.modelYear = modelYear; }

    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getMileage() { return mileage; }
    public void setMileage(double mileage) { this.mileage = mileage; }

    @Override
    public String getItemDetails() { return brand + " " + modelYear + " (" + state + ")"; }
}