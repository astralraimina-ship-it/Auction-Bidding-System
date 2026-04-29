package com.auction.common.item;

import java.util.Map;

public class ItemFactory {

    /**
     * Phương thức tạo Item linh hoạt.
     * @param category Loại hàng (ART, ELECTRONICS, VEHICLE, OTHER)
     * @param commonData Chứa id, name, description, startPrice, binPrice, step
     * @param specificData Chứa các thuộc tính riêng của từng loại
     */
    public static Item createItem(String category, Map<String, Object> commonData, Map<String, Object> specificData) {

        // Trích xuất dữ liệu chung
        int id = (int) commonData.get("id");
        String name = (String) commonData.get("name");
        String desc = (String) commonData.get("description");
        double start = (double) commonData.get("startPrice");
        double bin = (double) commonData.get("binPrice");
        double step = (double) commonData.get("step");

        switch (category.toUpperCase()) {
            case "ART":
                return new Art(id, name, desc, start, bin, step,
                        (String) specificData.get("artist"),
                        (String) specificData.get("medium"));

            case "ELECTRONICS":
                return new Electronics(id, name, desc, start, bin, step,
                        (String) specificData.get("brand"),
                        (String) specificData.get("warranty"),
                        (String) specificData.get("state"));

            case "VEHICLE":
                return new Vehicle(id, name, desc, start, bin, step,
                        (String) specificData.get("brand"),
                        (int) specificData.get("modelYear"),
                        (String) specificData.get("engineType"),
                        (String) specificData.get("state"),
                        (int) specificData.get("age"),
                        (double) specificData.get("mileage"));

            case "OTHER":
            default:
                return new Other(id, name, desc, start, bin, step);
        }
    }
}