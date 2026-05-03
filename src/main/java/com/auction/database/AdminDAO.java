package com.auction.database;

import com.auction.common.user.*;
import com.auction.transaction.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class AdminDAO extends UserDAO {

    public ObservableList<User> getAllUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String role = rs.getString("role");
                int id = rs.getInt("id");
                String user = rs.getString("username");
                String pass = rs.getString("password");
                double bal = rs.getDouble("balance");

                User u;
                // Dựa vào role trong DB để khởi tạo đúng Class con
                if ("ADMIN".equalsIgnoreCase(role)) {
                    u = new Admin(id, user, pass);
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    u = new Seller(id, user, pass, bal);
                } else {
                    u = new Bidder(id, user, pass, bal);
                }

                // Set thêm role và status
                u.setRole(role);
                u.setStatus(rs.getString("status"));
                list.add(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean approveTransaction(Transaction t) {
        String updateTrans = "UPDATE transactions SET status = 'APPROVED' WHERE id = ?";
        String updateUser = t.getType().equals("DEPOSIT") ?
                "UPDATE users SET balance = balance + ? WHERE username = ?" :
                "UPDATE users SET balance = balance - ? WHERE username = ?";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateTrans);
                 PreparedStatement ps2 = conn.prepareStatement(updateUser)) {
                ps1.setInt(1, t.getId());
                ps1.executeUpdate();
                ps2.setDouble(1, t.getAmount());
                ps2.setString(2, t.getUsername());
                ps2.executeUpdate();
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean rejectTransaction(int transactionId) {
        String sql = "UPDATE transactions SET status = 'REJECTED' WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
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
                list.add(new Transaction(rs.getInt("id"), rs.getString("username"), rs.getString("type"),
                        rs.getDouble("amount"), rs.getDouble("fee"), rs.getDouble("net_amount"), rs.getString("status")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateUserStatus(String username, String newStatus) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Thêm hàm cập nhật Role nếu Long muốn đổi quyền trực tiếp
    public boolean updateUserRole(String username, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}