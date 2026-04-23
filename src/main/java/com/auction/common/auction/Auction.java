package com.auction.common.auction;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.exception.InvalidBINPriceException;
import com.auction.common.item.Item;
import com.auction.common.user.Bidder;
import com.auction.common.user.Seller;
import com.auction.common.user.User;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Auction {
    private final User seller;
    private final Item item;
    private final double startPrice;
    private final double binPrice;
    private LocalTime closedTime;

    // Quản lý lịch sử đặt giá
    private final LinkedList<BidTransaction> history = new LinkedList<>();

    // Yêu cầu 3: Quản lý trạng thái phiên
    private AuctionStatus status = AuctionStatus.OPEN;

    // Yêu cầu 1: Danh sách các Observer (Người theo dõi)
    private final List<AuctionObserver> observers = new ArrayList<>();

    public Auction(User _seller, Item _item, double _startPrice, double _binPrice, LocalTime _closedTime)
            throws InvalidBINPriceException {

        // Kiểm tra logic giá BIN
        if (_binPrice <= _startPrice) {
            throw new InvalidBINPriceException("Gia BIN phai lon hon gia khoi diem!");
        }

        this.seller = _seller;
        this.item = _item;
        this.startPrice = _startPrice;
        this.binPrice = _binPrice;
        this.closedTime = _closedTime;

        // Bản ghi khởi tạo từ người bán
        history.push(new BidTransaction(seller, startPrice, LocalTime.now()));
    }

    // --- LOGIC OBSERVER ---
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(String message) {
        for (AuctionObserver obs : observers) {
            obs.onUpdate(message);
        }
    }

    // --- LOGIC ĐẤU GIÁ (THREAD-SAFE) ---

    public synchronized void startAuction() {
        this.status = AuctionStatus.RUNNING;
        notifyObservers("Phien dau gia vat pham [" + item.getName() + "] da bat dau!");
    }

    public double getCurrentPrice() {
        BidTransaction highestBid = history.peek();
        return (highestBid != null) ? highestBid.getBidPrice() : startPrice;
    }

    public BidTransaction getHighestBid() {
        return history.peek();
    }

    // Hàm Mua Ngay (BIN)
    public synchronized void buyItNow(User user) {
        if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.PAID) return;

        System.out.println(user.getName() + " da chon Mua Ngay!");
        BidTransaction binBid = new BidTransaction(user, binPrice, LocalTime.now());
        history.push(binBid);

        notifyObservers(user.getName() + " da chot mua ngay voi gia " + binPrice);
        this.closed();
    }

    // Yêu cầu 4 & 5: Đồng bộ hóa hàm update để tránh Race Condition
    public synchronized void update(User user, double bidPrice) {
        try {
            LocalTime now = LocalTime.now();

            // 1. Kiểm tra trạng thái/thời gian
            if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.PAID || now.isAfter(closedTime)) {
                this.status = AuctionStatus.FINISHED;
                throw new AuctionClosedException();
            }

            // 2. Kiểm tra giá BIN (Chặn nếu >= BIN và gợi ý mua ngay)
            if (bidPrice >= binPrice) {
                System.out.println("Gia " + bidPrice + " dat nguong BIN. He thong khong ghi nhan bid.");
                notifyObservers("Goi y cho " + user.getName() + ": Ban co muon Mua Ngay voi gia " + binPrice + "?");
                return;
            }

            // 3. Kiểm tra giá bid hợp lệ
            if (bidPrice <= getCurrentPrice()) {
                throw new InvalidBidException();
            }

            // 4. Cập nhật lịch sử
            BidTransaction newBid = new BidTransaction(user, bidPrice, now);
            history.push(newBid);
            this.status = AuctionStatus.RUNNING;

            notifyObservers("Muc gia moi: " + bidPrice + " (boi " + user.getName() + ")");

            // 5. Logic Anti-snipe
            if (now.isAfter(closedTime.minusSeconds(30))) {
                this.closedTime = this.closedTime.plusMinutes(1);
                notifyObservers("Phien dau gia duoc tu dong gia han den " + closedTime);
            }

        } catch (InvalidBidException e) {
            System.out.println("[THAT BAI]: Gia dat " + bidPrice + " khong hop le (phai cao hon " + getCurrentPrice() + ")");
        } catch (AuctionClosedException e) {
            System.out.println("[THAT BAI]: Phien dau gia da ket thuc, khong the dat gia!");
        } catch (Exception e) {
            System.out.println("[LOI HE THONG]: " + e.getMessage());
        }
    }

    // Hàm chốt phiên
    public synchronized void closed() {
        if (this.status == AuctionStatus.PAID) return;

        BidTransaction highestBid = this.getHighestBid();

        // Kiểm tra nếu có người đặt giá (khác seller ban đầu)
        if (highestBid != null && highestBid.getBidder() != seller) {
            User bidder = highestBid.getBidder();
            double price = this.getCurrentPrice();

            if (bidder instanceof Bidder && seller instanceof Seller) {
                ((Bidder) bidder).subtract(price);
                ((Seller) seller).addAmount(price);

                this.status = AuctionStatus.PAID;
                notifyObservers("CHOT PHIEN: " + bidder.getName() + " thang voi gia " + price);
            }
        } else {
            this.status = AuctionStatus.CANCELED;
            notifyObservers("Phien dau gia ket thuc ma khong co nguoi mua.");
        }
    }

    // Getters cho GUI
    public AuctionStatus getStatus() { return status; }
    public double getBinPrice() { return binPrice; }
    public Item getItem() { return item; }
}


// Yêu cầu 4 & 5: Sử dụng synchronized để chặn Race Condition.
// Đảm bảo tại một thời điểm chỉ có một luồng được cập nhật giá hoặc đóng phiên,
// duy trì tính nhất quán của dữ liệu theo nguyên tắc First-Come, First-Served.