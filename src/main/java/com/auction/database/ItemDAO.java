package com.auction.database;

import com.auction.common.item.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ItemDAO {

    /**
     * 1. Lấy tất cả sản phẩm đang OPEN và chưa hết hạn (Cho màn hình chính của Bidder)
     */
    public ObservableList<Item> getAllOpenItems() {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "WHERE i.status = 'OPEN' AND i.end_time > NOW() " +
                "ORDER BY i.end_time ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToItem(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 2. Lấy danh sách sản phẩm mà người dùng đã thắng (Để thanh toán)
     */
    public ObservableList<Item> getWonItems(int userId) {
        ObservableList<Item> list = FXCollections.observableArrayList();
        // Lấy những item có status CLOSED và winner_id là chính mình
        String sql = "SELECT i.*, u.username AS seller_name FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "WHERE i.status = 'CLOSED' AND i.winner_id = ? " +
                "ORDER BY i.end_time DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 3. Đóng phiên đấu giá và cập nhật người thắng (Dùng khi mua đứt hoặc hết giờ)
     */
    public boolean closeAuction(int itemId, int winnerId) {
        // Cập nhật trạng thái thành CLOSED, set winner_id và thời gian kết thúc
        String sql = "UPDATE items SET status = 'CLOSED', winner_id = ?, end_time = NOW() WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, winnerId);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 4. Lấy tất cả sản phẩm cho Admin
     */
    public ObservableList<Item> getAllItemsForAdmin() {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i " +
                "JOIN users u ON i.seller_id = u.id ORDER BY i.id DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToItem(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 5. Lấy sản phẩm theo Seller ID
     */
    public ObservableList<Item> getItemsBySeller(int sellerId) {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "WHERE i.seller_id = ? ORDER BY i.id DESC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 6. Thêm sản phẩm mới
     */
    public boolean addItem(Item item, int sellerId) {
        String sql = "INSERT INTO items (name, description, startPrice, binPrice, step, category, " +
                "end_time, status, brand, warranty, state, artist, medium, modelYear, engineType, age, mileage, seller_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartPrice());
            ps.setDouble(4, item.getBinPrice());
            ps.setDouble(5, item.getStep());
            ps.setString(6, item.getCategory());
            ps.setTimestamp(7, item.getEndTime());
            ps.setString(8, item.getStatus());
            setSpecificData(ps, item);
            ps.setInt(18, sellerId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- CÁC HÀM TRỢ GIÚP (HELPER) ---

    private void setSpecificData(PreparedStatement ps, Item item) throws SQLException {
        for (int i = 9; i <= 17; i++) ps.setNull(i, Types.NULL);
        if (item instanceof Vehicle v) {
            ps.setString(9, v.getBrand());
            ps.setString(11, v.getState());
            ps.setInt(14, v.getModelYear());
            ps.setString(15, v.getEngineType());
            ps.setInt(16, v.getAge());
            ps.setDouble(17, v.getMileage());
        } else if (item instanceof Art a) {
            ps.setString(12, a.getArtist());
            ps.setString(13, a.getMedium());
            ps.setString(11, a.getState());
        } else if (item instanceof Electronics e) {
            ps.setString(9, e.getBrand());
            ps.setString(10, e.getWarranty());
            ps.setString(11, e.getState());
        }
    }

    private Item mapResultSetToItem(ResultSet rs) throws Exception {
        String category = rs.getString("category");
        Map<String, Object> common = new HashMap<>();
        common.put("id", rs.getInt("id"));
        common.put("name", rs.getString("name"));
        common.put("description", rs.getString("description"));
        common.put("startPrice", rs.getDouble("startPrice"));
        common.put("binPrice", rs.getDouble("binPrice"));
        common.put("step", rs.getDouble("step"));
        common.put("endTime", rs.getTimestamp("end_time"));
        common.put("status", rs.getString("status"));
        common.put("sellerName", rs.getString("seller_name"));

        Map<String, Object> specific = new HashMap<>();
        specific.put("brand", rs.getString("brand"));
        specific.put("warranty", rs.getString("warranty"));
        specific.put("state", rs.getString("state"));
        specific.put("artist", rs.getString("artist"));
        specific.put("medium", rs.getString("medium"));
        specific.put("modelYear", rs.getInt("modelYear"));
        specific.put("engineType", rs.getString("engineType"));
        specific.put("age", rs.getInt("age"));
        specific.put("mileage", rs.getDouble("mileage"));

        return ItemFactory.createItem(category, common, specific);
    }
}