package com.auction.common.user;

public interface Payable {
    void deposit(double amount);
    boolean withdraw(double amount);
    double getBalance();

    // Chuyển tiền sang một đối tượng Payable khác
    boolean transferTo(Payable receiver, double amount);
}