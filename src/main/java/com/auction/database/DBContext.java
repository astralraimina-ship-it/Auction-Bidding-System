package com.auction.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBContext {
    public static Connection getConnection() throws Exception {
        // Thông tin khớp chính xác với ảnh image_b892ed.jpg của Long
        String host = "mysql-2d5347b7-astralraimina-5e20.l.aivencloud.com";
        String port = "22111";
        String dbName = "defaultdb";
        String user = "avnadmin";
        String pass = "AVNS_S3aqhacyG7N3ebdemBC";

        // Chuỗi URL tối ưu:
        // 1. Thêm sslMode=REQUIRED để khớp với yêu cầu của Aiven trong ảnh
        // 2. allowPublicKeyRetrieval=true để tránh lỗi bảo mật khi kết nối lần đầu
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&sslMode=REQUIRED"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Ho_Chi_Minh"; // Set về giờ Việt Nam cho chuẩn BTL

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException e) {
            throw new Exception("Lỗi: Thiếu thư viện MySQL Connector Driver!");
        } catch (SQLException e) {
            throw new Exception("Lỗi kết nối Database: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println(">>> ĐANG KẾT NỐI ĐẾN CLOUD DATABASE...");
            // Thử tạo kết nối
            Connection conn = getConnection();

            if (conn != null && !conn.isClosed()) {
                System.out.println(">>> KẾT NỐI CLOUD THÀNH CÔNG!");
                System.out.println(">>> THÔNG TIN HOST: " + conn.getMetaData().getURL());
                // Đóng kết nối sau khi test xong
                conn.close();
            }
        } catch (Exception e) {
            System.err.println(">>> THẤT BẠI RỒI");
        }
    }
}