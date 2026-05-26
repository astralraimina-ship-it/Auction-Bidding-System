package com.auction.server.observer;

import java.util.ArrayList;
import java.util.List;

public class BidPublisher implements BidSubject {
    private static BidPublisher instance;

    // Danh sách lưu trữ tất cả các ClientHandler (người nghe) đang kết nối
    private final List<BidObserver> observers = new ArrayList<>();

    // Không cho phép khởi tạo bừa bãi bên ngoài (Singleton)
    private BidPublisher() {}

    // Hàm lấy trạm phát sóng dùng chung
    public static synchronized BidPublisher getInstance() {
        if (instance == null) {
            instance = new BidPublisher();
        }
        return instance;
    }

    @Override
    public synchronized void registerObserver(BidObserver o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    @Override
    public synchronized void removeObserver(BidObserver o) {
        observers.remove(o);
    }

    @Override
    public synchronized void notifyObservers() {
        // Vòng lặp duyệt qua toàn bộ danh sách để bắt từng người phát lệnh
        for (BidObserver observer : observers) {
            observer.onNotificationReceived();
        }
    }
}