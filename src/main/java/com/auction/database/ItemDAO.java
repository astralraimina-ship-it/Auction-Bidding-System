package com.auction.database;

import com.auction.common.item.*;
import com.auction.server.AuctionServer;
import com.auction.server.AuctionState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static com.auction.server.AuctionServer.activeAuctions;
import static com.auction.server.AuctionServer.activeAutoBids;

public class ItemDAO {

    // --- Hàm bổ trợ để kiểm tra cột tồn tại ---
    private boolean columnExists(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (meta.getColumnName(i).equalsIgnoreCase(columnName)) return true;
        }
        return false;
    }

    // --- 1. Lấy tất cả sản phẩm đang OPEN (ĐÃ ĐỒNG BỘ GIỜ VỚI DB) ---
    public ObservableList<Item> getAllOpenItems() {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i JOIN users u ON i.seller_id = u.id WHERE i.status = 'OPEN' AND i.end_time > ? ORDER BY i.end_time ASC";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()) {
                    list.add(mapResultSetToItem(rs));
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- 2. Lấy danh sách sản phẩm thắng (ĐỒNG BỘ CHUẨN ĐIỀU KIỆN ĐỂ HIỂN THỊ) ---
    public ObservableList<Item> getWonItems(int userId) {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "WHERE i.status = 'CLOSED' AND i.winner_id = ? AND i.payment_status = 'PENDING' " +
                "ORDER BY i.end_time DESC";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // --- 3. Đóng phiên (SỬA LỖI CHÍ MẠNG: Đưa trực tiếp giá từ RAM xuống để tránh lỗi sai tên cột) ---
    public boolean closeAuction(int itemId, int winnerId) {
        double winPrice = 0.0;
        AuctionState state = activeAuctions.get(itemId);
        if (state != null) {
            winPrice = state.getCurrentPrice();
        }

        String sql = "UPDATE items SET status = 'CLOSED', winner_id = ?, end_time = NOW(), payment_status = 'PENDING', win_price = ? WHERE id = ? AND status = 'OPEN'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, winnerId);
            ps.setDouble(2, winPrice);
            ps.setInt(3, itemId);

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                // Tự động dọn dẹp các Map lưu trữ trên RAM sau khi lưu DB thành công
                activeAuctions.remove(itemId);
                activeAutoBids.remove(itemId);
                System.out.println(">>> [DB -> RAM] Đã lưu thông tin người thắng và dọn dẹp phòng đấu giá ID " + itemId + " trên RAM.");
            }
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- 4. Gia hạn (Hỗ trợ Anti-Snipe tăng hạn thời gian thực dưới DB) ---
    public boolean extendAuctionTime(int itemId, int minutesToAdd) {
        String sql = "UPDATE items SET end_time = DATE_ADD(end_time, INTERVAL ? MINUTE) WHERE id = ? AND status = 'OPEN'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minutesToAdd); ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // --- 5. Lấy tất cả cho Admin ---
    public ObservableList<Item> getAllItemsForAdmin() {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i JOIN users u ON i.seller_id = u.id ORDER BY i.id DESC";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSetToItem(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // --- 6. Lấy theo Seller ---
    public ObservableList<Item> getItemsBySeller(int sellerId) {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i JOIN users u ON i.seller_id = u.id WHERE i.seller_id = ? ORDER BY i.id DESC";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapResultSetToItem(rs)); }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // --- 7. Thêm sản phẩm ---
    public boolean addItem(Item item, int sellerId) {
        String sql = "INSERT INTO items (name, description, startPrice, binPrice, step, category, end_time, status, " +
                "brand, warranty, state, artist, medium, modelYear, engineType, age, mileage, seller_id, payment_status, image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)";

        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName()); ps.setString(2, item.getDescription()); ps.setDouble(3, item.getStartPrice());
            ps.setDouble(4, item.getBinPrice()); ps.setDouble(5, item.getStep()); ps.setString(6, item.getCategory());
            ps.setTimestamp(7, item.getEndTime()); ps.setString(8, item.getStatus());

            setSpecificData(ps, item);

            ps.setInt(18, sellerId);
            ps.setString(19, item.getImagePath() != null ? item.getImagePath() : "default.png");

            if (ps.executeUpdate() > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        item.setId(generatedId);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void setSpecificData(PreparedStatement ps, Item item) throws SQLException {
        for (int i = 9; i <= 17; i++) ps.setNull(i, Types.NULL);
        if (item instanceof Vehicle v) { ps.setString(9, v.getBrand()); ps.setString(11, v.getState()); ps.setInt(14, v.getModelYear()); ps.setString(15, v.getEngineType()); ps.setInt(16, v.getAge()); ps.setDouble(17, v.getMileage()); }
        else if (item instanceof Art a) { ps.setString(12, a.getArtist()); ps.setString(13, a.getMedium()); ps.setString(11, a.getState()); }
        else if (item instanceof Electronics e) { ps.setString(9, e.getBrand()); ps.setString(10, e.getWarranty()); ps.setString(11, e.getState()); }
    }

    private Item mapResultSetToItem(ResultSet rs) throws Exception {
        String category = rs.getString("category");
        Map<String, Object> common = new HashMap<>();
        common.put("id", rs.getInt("id"));
        common.put("name", rs.getString("name"));
        common.put("description", rs.getString("description"));
        common.put("startPrice", rs.getDouble("startPrice"));
        common.put("binPrice", rs.getDouble("binPrice"));

        if (columnExists(rs, "currentPrice")) {
            common.put("currentPrice", rs.getDouble("currentPrice"));
        } else if (columnExists(rs, "current_price")) {
            common.put("currentPrice", rs.getDouble("current_price"));
        } else {
            common.put("currentPrice", 0.0);
        }

        if (columnExists(rs, "win_price")) {
            common.put("winPrice", rs.getDouble("win_price"));
        } else if (columnExists(rs, "winPrice")) {
            common.put("winPrice", rs.getDouble("winPrice"));
        } else {
            common.put("winPrice", 0.0);
        }

        common.put("step", rs.getDouble("step"));
        common.put("endTime", rs.getTimestamp("end_time"));
        common.put("status", rs.getString("status"));
        common.put("sellerName", rs.getString("seller_name"));
        common.put("paymentStatus", columnExists(rs, "payment_status") ? rs.getString("payment_status") : "PENDING");
        common.put("imagePath", columnExists(rs, "image_path") ? rs.getString("image_path") : "default.png");

        Map<String, Object> specific = new HashMap<>();
        specific.put("brand", rs.getString("brand")); specific.put("warranty", rs.getString("warranty")); specific.put("state", rs.getString("state"));
        specific.put("artist", rs.getString("artist")); specific.put("medium", rs.getString("medium")); specific.put("modelYear", rs.getInt("modelYear"));
        specific.put("engineType", rs.getString("engineType")); specific.put("age", rs.getInt("age")); specific.put("mileage", rs.getDouble("mileage"));

        return ItemFactory.createItem(category, common, specific);
    }

    public boolean checkAndCloseExpiredItems() {
        String sqlGetExpired = "SELECT id FROM items WHERE end_time <= NOW() AND status = 'OPEN'";
        String sqlSetWinner = "UPDATE items i " +
                "JOIN bids b ON b.item_id = i.id " +
                "SET i.status = 'CLOSED', " +
                "    i.winner_id = b.user_id, " +
                "    i.win_price = b.bid_amount, " +
                "    i.payment_status = 'PENDING' " +
                "WHERE i.id = ? " +
                "  AND b.bid_amount = (SELECT MAX(bid_amount) FROM bids WHERE item_id = ?)";

        String sqlCancelItem = "UPDATE items SET status = 'CLOSED', payment_status = 'EXPIRED' WHERE id = ? " +
                "AND (SELECT COUNT(*) FROM bids WHERE item_id = ?) = 0";

        boolean hasChanges = false;

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psGet = conn.prepareStatement(sqlGetExpired)) {
                try(ResultSet rs = psGet.executeQuery()){
                    while (rs.next()) {
                        int itemId = rs.getInt("id");

                        try (PreparedStatement psWin = conn.prepareStatement(sqlSetWinner)) {
                            psWin.setInt(1, itemId);
                            psWin.setInt(2, itemId);
                            int rows = psWin.executeUpdate();
                            if (rows > 0) {
                                System.out.println(">>> [DB] Sản phẩm ID " + itemId + " đã kết thúc -> Xác định được người thắng.");
                                hasChanges = true;
                                activeAuctions.remove(itemId);
                                activeAutoBids.remove(itemId);
                                continue;
                            }
                        }

                        try (PreparedStatement psCancel = conn.prepareStatement(sqlCancelItem)) {
                            psCancel.setInt(1, itemId);
                            psCancel.setInt(2, itemId);
                            int rows = psCancel.executeUpdate();
                            if (rows > 0) {
                                System.out.println(">>> [DB] Sản phẩm ID " + itemId + " đã kết thúc -> Không ai đấu giá (HỦY PHIÊN).");
                                hasChanges = true;
                                activeAuctions.remove(itemId);
                                activeAutoBids.remove(itemId);
                            }
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasChanges;
    }

    public static void processExpiredPayments() {
        String getExpiredSQL = "SELECT id, winner_id FROM items WHERE end_time < NOW() - INTERVAL 1 DAY AND payment_status = 'PENDING' AND status = 'CLOSED'";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtGet = conn.prepareStatement(getExpiredSQL);
                 PreparedStatement stmtItem = conn.prepareStatement("UPDATE items SET payment_status = 'EXPIRED' WHERE id = ?");
                 PreparedStatement stmtUser = conn.prepareStatement("UPDATE users SET cancel_count = cancel_count + 1 WHERE id = ?");
                 PreparedStatement stmtBan = conn.prepareStatement("UPDATE users SET status = 'BLOCKED' WHERE id = ? AND cancel_count >= 3")) {
                try (ResultSet rs = stmtGet.executeQuery()) {
                    while (rs.next()) {
                        int itemId = rs.getInt("id"); int winnerId = rs.getInt("winner_id");
                        stmtItem.setInt(1, itemId); stmtItem.executeUpdate();
                        stmtUser.setInt(1, winnerId); stmtUser.executeUpdate();
                        stmtBan.setInt(1, winnerId); stmtBan.executeUpdate();
                    }
                }
                conn.commit();
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean payForItem(int itemId, int userId, double clientAmount) {
        String sqlCheck = "SELECT win_price, currentPrice, seller_id FROM items WHERE id = ? AND winner_id = ? AND payment_status = 'PENDING'";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            double priceToPay = 0; int sellerId = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, itemId); ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double win = rs.getDouble("win_price");
                        priceToPay = (win > 0) ? win : rs.getDouble("currentPrice");
                        sellerId = rs.getInt("seller_id");
                    } else { conn.rollback(); return false; }
                }
            }
            String sqlDeduct = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?";
            String sqlAdd = "UPDATE users SET balance = balance + ? WHERE id = ?";
            String sqlUpdateItem = "UPDATE items SET payment_status = 'PAID' WHERE id = ?";
            try (PreparedStatement psD = conn.prepareStatement(sqlDeduct); PreparedStatement psA = conn.prepareStatement(sqlAdd); PreparedStatement psU = conn.prepareStatement(sqlUpdateItem)) {
                psD.setDouble(1, priceToPay); psD.setInt(2, userId); psD.setDouble(3, priceToPay);
                psA.setDouble(1, priceToPay); psA.setInt(2, sellerId);
                psU.setInt(1, itemId);
                if (psD.executeUpdate() > 0 && psA.executeUpdate() > 0 && psU.executeUpdate() > 0) { conn.commit(); return true; }
                else { conn.rollback(); return false; }
            }
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public void createAuctionState(){
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, currentPrice, step, binPrice FROM items WHERE status = 'OPEN'");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                double startPrice = rs.getDouble("currentPrice");
                double step = rs.getDouble("step");
                double binPrice = rs.getDouble("binPrice");

                AuctionState state = new AuctionState(id, startPrice, step, binPrice);
                activeAuctions.put(id, state);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ObservableList<Item> getParticipatedAndLostItems(int userId) {
        ObservableList<Item> list = FXCollections.observableArrayList();
        String sql = "SELECT i.*, u.username AS seller_name FROM items i " +
                "JOIN users u ON i.seller_id = u.id " +
                "WHERE i.id IN (SELECT DISTINCT item_id FROM bids WHERE user_id = ?) " +
                "AND ( " +
                "    i.status = 'OPEN' " +
                "    OR (i.status = 'CLOSED' AND (i.winner_id IS NULL OR i.winner_id != ?) AND i.end_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)) " +
                ") " +
                "ORDER BY CASE WHEN i.status = 'OPEN' THEN 1 ELSE 2 END, i.end_time DESC";

        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
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

    public Item getItemById(int itemId) {
        String sql = "SELECT i.*, u.username AS seller_name FROM items i JOIN users u ON i.seller_id = u.id WHERE i.id = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}