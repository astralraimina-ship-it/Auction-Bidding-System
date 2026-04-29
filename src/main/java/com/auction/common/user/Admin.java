package com.auction.common.user;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(int id, String username, String password) {
        super(id, username, password, "ADMIN");
    }

    @Override
    public String getUserDetails() {
        return "[ADMIN] " + getUsername() + " | Trạng thái: " + getStatus();
    }
}