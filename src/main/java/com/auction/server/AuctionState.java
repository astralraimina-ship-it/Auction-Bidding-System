package com.auction.server;
/**
 * Lớp lưu trữ trạng thái của một phiên đấu giá trong RAM (Trọng tài xử lý)
 */
public class AuctionState {

    // ==========================================
    // 1. THÔNG SỐ CƠ BẢN CỦA MÓN HÀNG
    // ==========================================
    private int itemId;
    private double stepPrice; // Bước giá (Yêu cầu phải chia hết cho 10,000)
    private double binPrice;

    // ==========================================
    // 2. THÔNG SỐ CÔNG KHAI (Mọi Client đều thấy)
    // ==========================================
    private double currentPrice;
    private int highestBidderId;

    // ==========================================
    // 3. THÔNG SỐ BÍ MẬT (Chỉ Trọng tài Server biết)
    // ==========================================
    private boolean isTopBidderAuto; // Đánh dấu xem người Top 1 hiện tại có dùng Auto-Bid không
    private double topAutoMaxBudget; // "Ví mật" của người Top 1 (chỉ có giá trị nếu isTopBidderAuto = true)

    // --- Constructor khởi tạo phiên đấu giá ---
    public AuctionState(int itemId, double startPrice, double stepPrice, double binPrice) {
        this.itemId = itemId;
        this.currentPrice = startPrice;
        this.stepPrice = stepPrice;
        this.binPrice = binPrice;
        this.highestBidderId = -1; // Ban đầu chưa có ai đấu giá
        this.isTopBidderAuto = false;
        this.topAutoMaxBudget = 0.0;
        System.out.println(currentPrice);
        System.out.println(isTopBidderAuto);
        System.out.println(topAutoMaxBudget);
        System.out.println(binPrice);
    }

    // ==========================================
    // CÁC HÀM GETTER / SETTER ĐỂ TRỌNG TÀI THAO TÁC
    // ==========================================

    public int getItemId() { return itemId; }

    public double getStepPrice() { return stepPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public int getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(int highestBidderId) { this.highestBidderId = highestBidderId; }

    public boolean isTopBidderAuto() { return isTopBidderAuto; }

    public double getTopAutoMaxBudget() { return topAutoMaxBudget; }

    /**
     * Hàm cập nhật người dẫn đầu LÀ MỘT NGƯỜI ĐẶT THỦ CÔNG
     */
    public void setManualTopBidder(int bidderId, double newPrice) {
        this.highestBidderId = bidderId;
        this.currentPrice = newPrice;
        this.isTopBidderAuto = false; // Tắt trạng thái Auto của top 1
        this.topAutoMaxBudget = 0.0;  // Xóa ví mật
        System.out.println(currentPrice);
        System.out.println(isTopBidderAuto);
        System.out.println(topAutoMaxBudget);
    }

    /**
     * Hàm cập nhật người dẫn đầu LÀ MỘT NGƯỜI DÙNG AUTO-BID
     */
    public void setAutoTopBidder(int bidderId, double newPrice, double maxBudget) {
        this.highestBidderId = bidderId;
        this.currentPrice = newPrice;
        this.isTopBidderAuto = true; // Bật trạng thái Auto
        this.topAutoMaxBudget = maxBudget; // Lưu ví mật
        System.out.println(currentPrice);
        System.out.println(isTopBidderAuto);
        System.out.println(topAutoMaxBudget);
    }
    public boolean isBin(){
        if (currentPrice == binPrice){
            return true;
        }
        return false;
    }
    public double getBinPrice() { return this.binPrice; }
}