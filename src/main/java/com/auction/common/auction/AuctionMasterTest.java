package com.auction.common.auction;

import com.auction.common.item.Electronics;
import com.auction.common.item.Item;
import com.auction.common.user.Bidder;
import com.auction.common.user.Seller;
import java.time.LocalTime;

public class AuctionMasterTest {
    public static void main(String[] args) {
        try {
            // 1. Khởi tạo đối tượng
            Seller seller = new Seller("Dung", "S01");
            Item laptop = new Electronics("Macbook M3", "Laptop cao cap", "lap");
            Bidder bidderA = new Bidder("Long", "B01");
            Bidder bidderB = new Bidder("Tuan", "B02");

            // Nạp tiền cho các bidder
            bidderA.addAmount(10000.0);
            bidderB.addAmount(10000.0);

            // 2. Tạo phiên đấu giá (Giá khởi điểm 1000, BIN 5000, kết thúc sau 10 giây)
            Auction auction = new Auction(seller, laptop, 1000.0, 5000.0, LocalTime.now().plusSeconds(10));

            // Đăng ký nhận thông báo (Observer)
            auction.addObserver(msg -> System.out.println("[CLIENT NOTIFICATION]: " + msg));

            System.out.println("--- BAT DAU TEST CAC TRUONG HOP ---");

            // TRƯỜNG HỢP 1: Đặt giá thấp hơn giá hiện tại (Lỗi)
            System.out.println("\nCase 1: Dat gia thap hon hien tai (900 < 1000)");
            auction.update(bidderA, 900.0);

            // TRƯỜNG HỢP 2: Đặt giá hợp lệ
            System.out.println("\nCase 2: Dat gia hop le (1200)");
            auction.update(bidderA, 1200.0);

            // TRƯỜNG HỢP 3: Đặt giá chạm ngưỡng BIN (Chặn và gợi ý)
            System.out.println("\nCase 3: Dat gia bang hoac hon BIN (5500 >= 5000)");
            auction.update(bidderB, 5500.0);

            // TRƯỜNG HỢP 4: Test Anti-snipe (Gia hạn thời gian)
            // Ta set closedTime về rất gần hiện tại (còn 5 giây) để kích hoạt logic
            System.out.println("\nCase 4: Test Anti-snipe (Dat gia khi con duoi 30s)");
            auction.update(bidderB, 2000.0);

            // TRƯỜNG HỢP 5: Tranh chấp đa luồng (Yêu cầu 5)
            // Hai người cùng nhấn bid 3000 cùng lúc
            System.out.println("\nCase 5: Tranh chap da luong (A va B cung bid 3000)");
            Thread t1 = new Thread(() -> auction.update(bidderA, 3000.0));
            Thread t2 = new Thread(() -> auction.update(bidderB, 3000.0));
            t1.start();
            t2.start();
            t1.join();
            t2.join();

            // TRƯỜNG HỢP 6: Mua ngay (BIN)
            System.out.println("\nCase 6: Bidder A quyet dinh Mua Ngay");
            auction.buyItNow(bidderA);

            // TRƯỜNG HỢP 7: Đóng phiên và kiểm tra tiền
            System.out.println("\nCase 7: Kiem tra so du sau khi thanh toan");
            System.out.println("So du Bidder A: " + bidderA.getBalance());
            System.out.println("So du Seller: " + seller.getBalance());

        } catch (Exception e) {
            System.err.println("Loi Test: " + e.getMessage());
        }
    }
}