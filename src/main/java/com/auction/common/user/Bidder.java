package com.auction.common.user;

public class Bidder extends User implements Payable {
    private static final long serialVersionUID = 1L;
    private double balance;

    public Bidder(int id, String username, String password, double balance) {
        super(id, username, password, "BIDDER");
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
    public double getBalance() { return balance; }

    public void setBalance(double balance) { this.balance = balance; }

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
        return String.format("Bidder: %s | Số dư: %.2f", getUsername(), balance);
    }
}