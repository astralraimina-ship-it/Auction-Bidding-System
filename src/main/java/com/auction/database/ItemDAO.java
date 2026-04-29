package com.auction.database;

import com.auction.common.item.Item;
import com.auction.common.item.ItemFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ItemDAO {
    public ObservableList<Item> getAllItems() {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM items";

        try (Connection conn = DBContext.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String category = rs.getString("category");

                // 1. Đóng gói dữ liệu chung
                Map<String, Object> commonData = new HashMap<>();
                commonData.put("id", rs.getInt("id"));
                commonData.put("name", rs.getString("name"));
                commonData.put("description", rs.getString("description"));
                commonData.put("startPrice", rs.getDouble("startPrice"));
                commonData.put("binPrice", rs.getDouble("binPrice"));
                commonData.put("step", rs.getDouble("step"));

                // 2. Đóng gói dữ liệu riêng (Lấy hết các cột, class nào cần gì thì Factory tự lấy nấy)
                Map<String, Object> specificData = new HashMap<>();
                specificData.put("brand", rs.getString("brand"));
                specificData.put("warranty", rs.getString("warranty"));
                specificData.put("state", rs.getString("state"));
                specificData.put("artist", rs.getString("artist"));
                specificData.put("medium", rs.getString("medium"));
                specificData.put("modelYear", rs.getInt("modelYear"));
                specificData.put("engineType", rs.getString("engineType"));
                specificData.put("age", rs.getInt("age"));
                specificData.put("mileage", rs.getDouble("mileage"));

                // 3. Nhờ Factory tạo Object
                Item item = ItemFactory.createItem(category, commonData, specificData);
                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}