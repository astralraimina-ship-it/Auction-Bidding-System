package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    // 🔥 THÊM MỚI: Class chứa dữ liệu điểm tọa độ để truyền cho Biểu đồ (LineChart)
    public static class BidHistoryPoint {
        public double price;
        public String timeLabel; // Định dạng "HH:mm:ss" làm trục X

        public BidHistoryPoint(double price, String timeLabel) {
            this.price = price;
            this.timeLabel = timeLabel;
        }
    }

    // 🔥 THÊM MỚI: Hàm lấy lịch sử vẽ biểu đồ không làm reset đồ thị khi out phòng
    public List<BidHistoryPoint> getBidHistoryOfItem(int itemId) {
        List<BidHistoryPoint> history = new ArrayList<>();
        String sql = "SELECT bid_amount, bid_time FROM bids WHERE item_id = ? ORDER BY bid_time ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
                while (rs.next()) {
                    double price = rs.getDouble("bid_amount");
                    Timestamp time = rs.getTimestamp("bid_time");
                    String timeLabel = (time != null) ? sdf.format(time) : "00:00:00";

                    history.add(new BidHistoryPoint(price, timeLabel));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }

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

    /**
     * 🔥 THÊM MỚI: Lưu hoặc cập nhật cấu hình Auto-Bid của người dùng xuống Database
     */
    public void saveOrUpdateAutoBid(int itemId, int userId, double maxBudget) {
        String sql = "INSERT INTO autobids (item_id, user_id, max_budget, is_active) " +
                "VALUES (?, ?, ?, TRUE) " +
                "ON DUPLICATE KEY UPDATE max_budget = ?, is_active = TRUE";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            ps.setDouble(3, maxBudget);
            ps.setDouble(4, maxBudget);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔥 THÊM MỚI: Tắt trạng thái hoạt động Auto-Bid của người dùng cho sản phẩm cụ thể
     */
    public void deactivateAutoBid(int itemId, int userId) {
        String sql = "UPDATE autobids SET is_active = FALSE WHERE item_id = ? AND user_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    public List<String> getBidHistoryText(int itemId) {
        List<String> historyList = new ArrayList<>();
        // Sắp xếp bid_time ASC để lượt cũ ở trên, lượt mới nhất xuất hiện ở dưới cùng
        String sql = "SELECT user_id, bid_amount FROM bids WHERE item_id = ? ORDER BY bid_time ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int bidderId = rs.getInt("user_id");
                    double bidAmount = rs.getDouble("bid_amount");

                    // Định dạng chuỗi hiển thị theo ý bạn
                    String logLine = "User ID " + bidderId + " đã đặt giá " + String.format("%,.0f", bidAmount) + " VNĐ";
                    historyList.add(logLine);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return historyList;
    }
}