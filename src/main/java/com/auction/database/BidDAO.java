package com.auction.database;
import java.sql.*;

public class BidDAO {

    /**
     * SỬA: Thực hiện Transaction để đảm bảo tính nhất quán (Insert lịch sử và Update giá cùng lúc)
     */
    public boolean placeBid(int itemId, int userId, double amount) {
        String insertBidSQL = "INSERT INTO bids (item_id, user_id, bid_amount, bid_time) VALUES (?, ?, ?, NOW())";
        String updateItemSQL = "UPDATE items SET current_price = ? WHERE id = ? AND status = 'OPEN' AND ? > current_price";

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Thêm vào bảng lịch sử
            try (PreparedStatement psBid = conn.prepareStatement(insertBidSQL)) {
                psBid.setInt(1, itemId);
                psBid.setInt(2, userId);
                psBid.setDouble(3, amount);
                psBid.executeUpdate();
            }

            // 2. Cập nhật giá vào bảng items
            try (PreparedStatement psItem = conn.prepareStatement(updateItemSQL)) {
                psItem.setDouble(1, amount);
                psItem.setInt(2, itemId);
                psItem.setDouble(3, amount);

                int affectedRows = psItem.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit(); // Thành công thì lưu
                    return true;
                } else {
                    conn.rollback(); // Nếu không update được (ví dụ giá thấp hơn hiện tại) thì hủy
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Các hàm khác giữ nguyên
    public double getCurrentMaxBid(int itemId, double startPrice) {
        String sql = "SELECT MAX(bid_amount) as max_bid FROM bids WHERE item_id = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double maxBid = rs.getDouble("max_bid");
                    return maxBid > 0 ? maxBid : startPrice;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return startPrice;
    }

    public int getBidCount(int itemId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE item_id = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }
}