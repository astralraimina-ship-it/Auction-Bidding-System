package com.auction.common.user;

public class UserFactory {

    /**
     * Tạo User dựa trên vai trò (Role).
     * @param id Thường là 0 khi đăng ký mới (chờ DB cấp), hoặc lấy từ DB khi đăng nhập.
     * @param role "ADMIN", "BIDDER", "SELLER"
     */
    public static User createUser(int id, String username, String password, String role, double balance) {

        switch (role.toUpperCase()) {
            case "ADMIN":
                return new Admin(id, username, password);

            case "BIDDER":
                return new Bidder(id, username, password, balance);

            case "SELLER":
                return new Seller(id, username, password, balance);

            default:
                throw new IllegalArgumentException("Vai trò người dùng không hợp lệ: " + role);
        }
    }
}