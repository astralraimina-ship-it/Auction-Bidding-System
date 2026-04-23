package com.auction.common.user;

import com.auction.common.auction.Auction;

public class Admin extends User {
    public Admin(String _name, String _id) {
        super(_name, _id); // Kế thừa id và name từ Entity thông qua User
    }

    // Admin không có balance, không có subtract/addAmount
    // Thay vào đó, Admin có các hàm đặc quyền:
    public void cancelAuction(Auction auction) {
        // Logic để hủy một phiên đấu giá bất kỳ
    }
}
