package com.auction.transaction; // Hoặc package bạn chứa các Model

public class Transaction {
    private int id;
    private String username;
    private String type;
    private double amount;
    private double fee;
    private double netAmount;
    private String status;

    // Constructor đầy đủ
    public Transaction(int id, String username, String type, double amount, double fee, double netAmount, String status) {
        this.id = id;
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.fee = fee;
        this.netAmount = netAmount;
        this.status = status;
    }

    // Getter và Setter (Quan trọng để TableView hiển thị được dữ liệu)
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public double getFee() { return fee; }
    public double getNetAmount() { return netAmount; }
    public String getStatus() { return status; }
}