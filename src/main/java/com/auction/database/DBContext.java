package com.auction.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBContext {
    public static Connection getConnection() throws Exception {
        Properties props = new Properties();
        String pass = "";

        // Đọc mật khẩu từ file cấu hình bên ngoài
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
            pass = props.getProperty("db.password");
        } catch (IOException e) {
            // Nếu không có file, có thể dùng tạm biến môi trường hoặc báo lỗi
            throw new Exception("Lỗi: Không tìm thấy file db.properties chứa mật khẩu!");
        }

        String host = "mysql-2d5347b7-astralraimina-5e20.l.aivencloud.com";
        String port = "22111";
        String dbName = "defaultdb";
        String user = "avnadmin";

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&sslMode=REQUIRED"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Ho_Chi_Minh";

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
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println(">>> KẾT NỐI CLOUD THÀNH CÔNG!");
                conn.close();
            }
        } catch (Exception e) {
            System.err.println(">>> THẤT BẠI: " + e.getMessage());
        }
    }
}