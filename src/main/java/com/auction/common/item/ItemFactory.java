package com.auction.common.item;

import java.util.Map;
import java.sql.Timestamp;

public class ItemFactory {

    public static Item createItem(String category, Map<String, Object> commonData, Map<String, Object> specificData) {
        int id = (int) commonData.getOrDefault("id", 0);
        String name = (String) commonData.getOrDefault("name", "");
        String desc = (String) commonData.getOrDefault("description", "");
        double start = (double) commonData.getOrDefault("startPrice", 0.0);
        double bin = (double) commonData.getOrDefault("binPrice", 0.0);

        // Bóc tách cả currentPrice và winPrice
        double currentPrice = (double) commonData.getOrDefault("currentPrice", 0.0);
        double winPrice = (double) commonData.getOrDefault("winPrice", 0.0);

        double step = (double) commonData.getOrDefault("step", 0.0);
        String sellerName = (String) commonData.getOrDefault("sellerName", "Unknown");
        Timestamp endTime = (Timestamp) commonData.get("endTime");
        String status = (String) commonData.getOrDefault("status", "OPEN");
        String paymentStatus = (String) commonData.getOrDefault("paymentStatus", "PENDING");

        if (category == null) category = "OTHER";

        switch (category.toUpperCase()) {
            case "ART":
                return new Art(id, name, desc, start, bin, currentPrice, step, sellerName, endTime, status, paymentStatus,
                        (String) specificData.getOrDefault("artist", ""),
                        (String) specificData.getOrDefault("medium", ""),
                        (String) specificData.getOrDefault("state", ""),
                        winPrice); // winPrice ở cuối

            case "ELECTRONICS":
                return new Electronics(id, name, desc, start, winPrice, bin, currentPrice, step, sellerName, endTime, status, paymentStatus,
                        (String) specificData.getOrDefault("brand", ""),
                        (String) specificData.getOrDefault("warranty", ""),
                        (String) specificData.getOrDefault("state", ""));

            case "VEHICLE":
                return new Vehicle(id, name, desc, start, bin, currentPrice, step, sellerName, endTime, status, paymentStatus,
                        (String) specificData.getOrDefault("brand", ""),
                        (int) specificData.getOrDefault("modelYear", 0),
                        (String) specificData.getOrDefault("engineType", ""),
                        (String) specificData.getOrDefault("state", ""),
                        (int) specificData.getOrDefault("age", 0),
                        (double) specificData.getOrDefault("mileage", 0.0),
                        winPrice); // winPrice ở cuối

            case "OTHER":
            default:
                return new Other(id, name, desc, start, bin, currentPrice, step, sellerName, endTime, status, paymentStatus, winPrice);
        }
    }
}