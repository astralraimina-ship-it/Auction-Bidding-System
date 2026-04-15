package com.auction.common.auction;

import java.time.LocalTime;

// Lớp này chứa thông tin về tên, giá và thời điểm đặt
public class BidTransaction{
    private String bidderName;
    private double bidPrice;
    private LocalTime time;

    public BidTransaction(String _bidderName, double _bidPrice, LocalTime _time){
        bidderName = _bidderName;
        bidPrice = _bidPrice;
        time = _time;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public LocalTime getTime() {
        return time;
    }
}
