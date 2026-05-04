package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    // 1. Đặt giá mới
    public boolean placeBid(int itemId, int userId, double amount) {
        String sql = "INSERT INTO bids (item_id, user_id, bid_amount) VALUES (?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Lấy giá cao nhất hiện tại của sản phẩm
    // Nếu chưa có ai đặt, hàm sẽ trả về giá khởi điểm (startPrice)
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

    // 3. Đếm số lượt đấu giá (Để hiển thị kiểu: "15 lượt bid")
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