package com.auction.common.user;

public class Seller extends User implements Payable {
    private static final long serialVersionUID = 1L;
    private double balance;

    public Seller(int id, String username, String password, double balance) {
        super(id, username, password, "SELLER");
        this.balance = balance;
    }

    @Override
    public void deposit(double amount) {
        if (amount > 0) this.balance += amount;
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    @Override
    public double getBalance() {
        return this.balance;
    }

    @Override
    public boolean transferTo(Payable receiver, double amount) {
        if (this.withdraw(amount)) {
            receiver.deposit(amount);
            return true;
        }
        return false;
    }

    @Override
    public String getUserDetails() {
        return "Seller: " + getUsername() + " | Ví: $" + balance;
    }
}