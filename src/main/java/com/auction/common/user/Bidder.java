package com.auction.common.user;

import com.auction.common.auction.Auction;
import com.auction.common.interfaces.Transaction;
import java.util.concurrent.locks.ReentrantLock;

public class Bidder extends User implements Transaction {
    private double balance = 0.0;

    // Yêu cầu 5: Sử dụng ReentrantLock để bảo vệ số dư tài khoản
    // Đảm bảo tại một thời điểm chỉ có 1 luồng được thay đổi tiền của người này
    private final ReentrantLock lock = new ReentrantLock();

    public Bidder(String _name, String _id) {
        super(_name, _id);
    }

    // Hàm Bid đã lược bỏ Scanner để linh hoạt hơn cho Socket/GUI sau này
    public void bid(Auction auction, double bidPrice) {
        // Kiểm tra số dư trước khi gửi lệnh bid đi
        if (bidPrice <= balance) {
            auction.update(this, bidPrice);
        } else {
            System.out.println(name + ": Khong du tien de dat gia " + bidPrice + " (So du hien tai: " + balance + ")");
        }
    }

    @Override
    public void addAmount(double _amount) {
        lock.lock(); // Khóa lại trước khi cộng tiền
        try {
            balance += _amount;
            System.out.println(name + " nhan tien. So du moi: " + balance);
        } finally {
            lock.unlock(); // Luôn mở khóa trong finally
        }
    }

    @Override
    public void subtract(double _amount) {
        lock.lock(); // Khóa lại trước khi trừ tiền
        try {
            balance -= _amount;
            System.out.println(name + " bi tru tien. So du con lai: " + balance);
        } finally {
            lock.unlock(); // Luôn mở khóa trong finally
        }
    }

    // Getter an toàn để kiểm tra số dư
    public double getBalance() {
        return balance;
    }
}



// Yêu cầu 5: Sử dụng ReentrantLock để bảo vệ tài sản người dùng.
// Khác với synchronized, Lock cho phép kiểm soát luồng linh hoạt hơn,
// đảm bảo an toàn cho các giao dịch tài chính khi xảy ra tranh chấp đa luồng.