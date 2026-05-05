package com.auction.database;

import java.sql.*;

public class BidDAO {

    /**
     * 1. Đặt giá mới (Chốt chặn cuối cùng tại Database)
     * Chỉ INSERT thành công nếu end_time > NOW() và status = 'OPEN'
     */
    public boolean placeBid(int itemId, int userId, double amount) {
        String sql = "INSERT INTO bids (item_id, user_id, bid_amount, bid_time) " +
                "SELECT ?, ?, ?, NOW() FROM items " +
                "WHERE id = ? AND end_time > NOW() AND status = 'OPEN'";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            ps.setDouble(3, amount);
            ps.setInt(4, itemId);

            // Nếu executeUpdate trả về 0, nghĩa là điều kiện WHERE không thỏa mãn (hết giờ)
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 2. Lấy giá cao nhất hiện tại của sản phẩm
     */
    public double getCurrentMaxBid(int itemId, double startPrice) {
        String sql = "SELECT MAX(bid_amount) as max_bid FROM bids WHERE item_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double maxBid = rs.getDouble("max_bid");
                    return maxBid > 0 ? maxBid : startPrice;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return startPrice;
    }

    /**
     * 3. Đếm số lượt đấu giá
     */
    public int getBidCount(int itemId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE item_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}