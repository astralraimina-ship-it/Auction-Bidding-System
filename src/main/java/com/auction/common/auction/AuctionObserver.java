package com.auction.common.auction;

public interface AuctionObserver {
    void onUpdate(String message);
}

// Yêu cầu 1: Triển khai Observer Pattern để thông báo trạng thái thời gian thực.
// Khi có giá mới hoặc phiên kết thúc, tất cả các bên liên quan đều nhận được cập nhật.