package com.auction.database;

import com.auction.common.user.*;
import com.auction.transaction.Transaction;
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
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND status = 'APPROVED'";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 1. Lấy các thông tin cần thiết cho constructor
                    int id = rs.getInt("id");
                    String userVal = rs.getString("username");
                    String passVal = rs.getString("password");
                    String role = rs.getString("role");

                    // 2. Khởi tạo Anonymous Inner Class
                    // Truyền đủ 4 tham số vào constructor cha
                    User user = new User(id, userVal, passVal, role) {
                        @Override
                        public String getUserDetails() {
                            // Triển khai hàm abstract bắt buộc của class User
                            return "User: " + getUsername() + " | Role: " + getRole();
                        }
                    };

                    // 3. Đổ nốt các field không có trong constructor (Setter)
                    user.setStatus(rs.getString("status"));
                    user.setBalance(rs.getDouble("balance"));

                    return user; // Trả về object User hợp lệ cho LoginController
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public ObservableList<Transaction> getPendingTransactions() {
        ObservableList<Transaction> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM transactions WHERE status = 'PENDING'";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getDouble("fee"),
                        rs.getDouble("net_amount"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
    public boolean approveTransaction(Transaction t) {
        String updateTrans = "UPDATE transactions SET status = 'APPROVED' WHERE id = ?";
        String updateUser = "";

        if (t.getType().equals("DEPOSIT")) {
            updateUser = "UPDATE users SET balance = balance + ? WHERE username = ?";
        } else {
            updateUser = "UPDATE users SET balance = balance - ? WHERE username = ?";
        }

        // Dùng Transaction (Database Transaction) để đảm bảo an toàn dữ liệu
        // Nếu nạp tiền thành công mà update user lỗi thì phải rollback
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Update trạng thái giao dịch
                PreparedStatement ps1 = conn.prepareStatement(updateTrans);
                ps1.setInt(1, t.getId());
                ps1.executeUpdate();

                // 2. Update tiền của User
                PreparedStatement ps2 = conn.prepareStatement(updateUser);
                ps2.setDouble(1, t.getAmount());
                ps2.setString(2, t.getUsername());
                ps2.executeUpdate();

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                return false;
            }
        } catch (Exception e) { return false; }
    }
    public boolean rejectTransaction(int transactionId) {
        String sql = "UPDATE transactions SET status = 'REJECTED' WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, transactionId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}