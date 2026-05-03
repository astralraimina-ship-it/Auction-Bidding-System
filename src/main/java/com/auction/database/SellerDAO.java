package com.auction.database;

import com.auction.common.item.Item;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class SellerDAO extends UserDAO {

    // 1. Lấy danh sách sản phẩm RIÊNG của Seller này để quản lý
    public ObservableList<Item> getMyItems(String sellerUsername) {
        ObservableList<Item> list = FXCollections.observableArrayList();
        // Giả sử bảng items có cột seller_name để biết hàng của ai
        String sql = "SELECT * FROM items WHERE seller_name = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Chỗ này Long dùng ItemFactory đã sửa hôm trước để tạo object nhé
                    // list.add(ItemFactory.createItem(...));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. Hàm để Seller đăng sản phẩm mới lên sàn
    public boolean postNewItem(Item item, String sellerUsername) {
        String sql = "INSERT INTO items (name, description, start_price, bin_price, step, category, end_time, status, seller_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN', ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartPrice());
            ps.setDouble(4, item.getBinPrice());
            ps.setDouble(5, item.getStep());
            ps.setString(6, item.getCategory());
            ps.setTimestamp(7, item.getEndTime());
            ps.setString(8, sellerUsername);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. Hàm nạp tiền (Seller cũng cần nạp tiền để đóng phí sàn chẳng hạn)
    public boolean requestDeposit(String username, double amount) {
        String sql = "INSERT INTO transactions (username, type, amount, fee, net_amount, status) " +
                "VALUES (?, 'DEPOSIT', ?, 0, ?, 'PENDING')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDouble(2, amount);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. Hàm rút tiền (Sau khi bán được hàng, Seller rút tiền về túi)
    public boolean requestWithdraw(String username, double amount) {
        double currentBalance = getUserBalance(username); // Gọi hàm của class cha UserDAO
        if (currentBalance < amount) return false;

        double fee = amount * 0.1;
        double netAmount = amount - fee;

        String sql = "INSERT INTO transactions (username, type, amount, fee, net_amount, status) " +
                "VALUES (?, 'WITHDRAW', ?, ?, ?, 'PENDING')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDouble(2, amount);
            ps.setDouble(3, fee);
            ps.setDouble(4, netAmount);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}