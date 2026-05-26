package com.auction.server.observer;

public interface BidObserver {
    // Hàm này sẽ tự động kích hoạt khi Trạm phát sóng hô "Refresh"
    void onNotificationReceived();
}