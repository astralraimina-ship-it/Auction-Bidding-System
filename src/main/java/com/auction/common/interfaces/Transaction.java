package com.auction.common.interfaces;

public interface Transaction {
//    Thêm chức năng chuyển tiền
    void subtract(double _money);
    void addAmount(double _money);
}
