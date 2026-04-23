package com.auction.common.user;

import com.auction.common.auction.Auction;
import com.auction.common.exception.InvalidBINPriceException;
import com.auction.common.interfaces.Transaction;
import com.auction.common.item.Item;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Seller extends User implements Transaction {
    private double balance = 0.0;
    private ArrayList<Item> items = new ArrayList<>();
    private ArrayList<Auction> auctions = new ArrayList<>();

    // Yêu cầu 5: Sử dụng ReentrantLock để bảo vệ tài khoản người bán
    private final ReentrantLock lock = new ReentrantLock();

    public Seller(String _name, String _id) {
        super(_name, _id);
    }

    // Tạo phiên đấu giá mới (Có xử lý Exception từ Auction)
    public Auction createAuction(Item item, double startPrice, double binPrice, LocalTime closedTime) {
        try {
            Auction newAuction = new Auction(this, item, startPrice, binPrice, closedTime);
            auctions.add(newAuction);
            return newAuction;
        } catch (InvalidBINPriceException e) {
            System.err.println("Loi khi tao phien dau gia: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void addAmount(double _amount) {
        lock.lock(); // Khóa để đảm bảo việc cộng tiền từ nhiều nguồn không bị lỗi
        try {
            balance += _amount;
            System.out.println(name + " (Seller) nhan thanh toan. So du moi: " + balance);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void subtract(double _amount) {
        lock.lock();
        try {
            balance -= _amount;
            System.out.println(name + " (Seller) bi tru tien/phi. So du con lai: " + balance);
        } finally {
            lock.unlock();
        }
    }
    // Đảm bảo khóa (Lock) luôn được giải phóng trong khối finally,
// tránh tình trạng Deadlock (treo luồng) kể cả khi có lỗi giao dịch xảy ra.

    // Getter an toàn
    public double getBalance() {
        return balance;
    }

    public ArrayList<Auction> getAuctions() {
        return auctions;
    }
}



// Yêu cầu 5: Sử dụng ReentrantLock để bảo vệ tài sản người dùng.
// Khác với synchronized, Lock cho phép kiểm soát luồng linh hoạt hơn,
// đảm bảo an toàn cho các giao dịch tài chính khi xảy ra tranh chấp đa luồng.