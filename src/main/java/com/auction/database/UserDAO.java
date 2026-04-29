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
        // Khi đăng ký mới, status mặc định là 'PENDING'
        String sql = "INSERT INTO users (username, password, role, status, balance) VALUES (?, ?, ?, 'PENDING', 0)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role.toUpperCase());
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
}