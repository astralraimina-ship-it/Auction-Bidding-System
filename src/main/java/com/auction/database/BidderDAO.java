package com.auction.database;

import java.sql.*;

public class BidderDAO extends UserDAO {
    public boolean requestDeposit(String username, double amount) {
        String sql = "INSERT INTO transactions (username, type, amount, fee, net_amount, status) VALUES (?, 'DEPOSIT', ?, 0, ?, 'PENDING')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDouble(2, amount);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean requestWithdraw(String username, double amount) {
        if (getUserBalance(username) < amount) return false;
        double net = amount * 0.9;
        String sql = "INSERT INTO transactions (username, type, amount, fee, net_amount, status) VALUES (?, 'WITHDRAW', ?, ?, ?, 'PENDING')";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setDouble(2, amount);
            ps.setDouble(3, amount * 0.1);
            ps.setDouble(4, net);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}