package com.auction.server.observer;

public interface BidSubject {
    void registerObserver(BidObserver o); // Đăng ký người nghe
    void removeObserver(BidObserver o);   // Xóa người nghe
    void notifyObservers();               // Phát loa thông báo cho tất cả
}