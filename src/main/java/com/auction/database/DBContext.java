package com.auction.database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBContext {
    public static Connection getConnection() throws Exception {
        // Kiểm tra đúng tên db 'uet_auction_system' và pass '123456'
        String url = "jdbc:mysql://localhost:3306/uet_auction_system";
        String user = "root";
        String pass = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, pass);
    }

    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                System.out.println(">>> KẾT NỐI DATABASE THÀNH CÔNG!");
            }
        } catch (Exception e) {
            System.err.println(">>> THẤT BẠI RỒI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}