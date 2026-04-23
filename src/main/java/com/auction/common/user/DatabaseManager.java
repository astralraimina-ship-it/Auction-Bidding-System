package com.auction.common.user;

import java.util.HashMap;

public class DatabaseManager {
    // Lưu ID -> Password để phục vụ Login
    private static HashMap<String, String> credentials = new HashMap<>();

    // Lưu ID -> Đối tượng User (Bidder/Seller) để phục vụ đấu giá
    private static HashMap<String, User> userStorage = new HashMap<>();

    public static void saveUser(User user, String password) {
        credentials.put(user.getId(), password); // getId() kế thừa từ Entity
        userStorage.put(user.getId(), user);
    }

    public static boolean checkLogin(String id, String password) {
        return password.equals(credentials.get(id));
    }

    public static User getUser(String id) {
        return userStorage.get(id);
    }
}