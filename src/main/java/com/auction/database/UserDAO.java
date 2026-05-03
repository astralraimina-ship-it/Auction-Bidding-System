package com.auction.database;

import com.auction.common.user.*;
import java.sql.*;

public class UserDAO {
    // Đăng nhập dùng chung
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND status = 'APPROVED'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String userVal = rs.getString("username");
                    String passVal = rs.getString("password");
                    String role = rs.getString("role");
                    User user = new User(id, userVal, passVal, role) {
                        @Override public String getUserDetails() { return "User: " + getUsername(); }
                    };
                    user.setStatus(rs.getString("status"));
                    user.setBalance(rs.getDouble("balance"));
                    return user;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // Đăng ký dùng chung
    public boolean register(String username, String password, String role) {
        String defaultStatus = "ADMIN".equalsIgnoreCase(role) ? "PENDING" : "APPROVED";
        String sql = "INSERT INTO users (username, password, role, status, balance) VALUES (?, ?, ?, ?, 0)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role.toUpperCase());
            ps.setString(4, defaultStatus);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // Lấy số dư dùng chung
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
}