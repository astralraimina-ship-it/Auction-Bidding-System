package com.auction.database;

import com.auction.common.user.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class UserDAO {

    // 1. Lấy toàn bộ danh sách User cho Admin Dashboard
    public ObservableList<User> getAllUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = "SELECT id, username, password, role, status, balance FROM users";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String role = rs.getString("role").toUpperCase();
                double balance = rs.getDouble("balance");
                User u;

                // Khởi tạo đúng số lượng tham số cho từng Class con
                switch (role) {
                    case "ADMIN":
                        u = new Admin(id, username, password); break;
                    case "SELLER":
                        u = new Seller(id, username, password, balance); break;
                    default:
                        u = new Bidder(id, username, password, balance); break;
                }
                u.setStatus(rs.getString("status"));
                list.add(u);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // 2. Hàm đăng ký User mới - GIẢI QUYẾT LỖI ĐỎ Ở RegisterController
    public boolean register(String username, String password, String role) {
        // Logic: Nếu là BIDDER hoặc SELLER thì cho APPROVED luôn
        // Chỉ có ADMIN (hoặc role lạ) mới phải PENDING
        String defaultStatus = "APPROVED";
        if ("ADMIN".equalsIgnoreCase(role)) {
            defaultStatus = "PENDING";
        }

        // Chỗ VALUES mình đổi 'PENDING' thành dấu ? để truyền biến vào
        String sql = "INSERT INTO users (username, password, role, status, balance) VALUES (?, ?, ?, ?, 0)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role.toUpperCase());
            ps.setString(4, defaultStatus); // Truyền status đã được xử lý logic

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. Hàm đăng nhập - CHỈ CHO PHÉP TÀI KHOẢN ĐÃ 'APPROVED'
    public String authenticate(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ? AND status = 'APPROVED'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("role");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // 4. Lấy số dư cho DashboardController
    public double getUserBalance(String username) {
        String sql = "SELECT balance FROM users WHERE username = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }
    // Trong UserDAO.java
    public boolean approveUser(String username) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE username = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            return ps.executeUpdate() > 0; // Trả về true nếu cập nhật thành công
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. Hàm cập nhật trạng thái (Dùng khi Admin duyệt User)
    public boolean updateUserStatus(int userId, String newStatus) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean requestDeposit(String username, double amount) {
        // Nạp tiền thì phí = 0, net_amount = amount luôn
        String sql = "INSERT INTO transactions (username, type, amount, fee, net_amount, status) " +
                "VALUES (?, 'DEPOSIT', ?, 0, ?, 'PENDING')";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setDouble(2, amount);
            ps.setDouble(3, amount); // net_amount bằng amount vì nạp không mất phí

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean requestWithdraw(String username, double amount) {
        double fee = amount * 0.1; // Phí 10%
        double netAmount = amount - fee; // Số tiền thực nhận

        // Kiểm tra số dư trước khi cho rút
        double currentBalance = getUserBalance(username);
        if (currentBalance < amount) {
            return false; // Không đủ tiền
        }

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