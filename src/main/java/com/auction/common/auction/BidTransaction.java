package com.auction.common.auction;

import com.auction.common.user.Bidder;
import com.auction.common.user.User;

import java.time.LocalTime;

// Lớp này chứa thông tin về tên, giá và thời điểm đặt
public class BidTransaction{
    private final User bidder;
    private double bidPrice;
    private LocalTime time;

    public BidTransaction(User _bidder, double _bidPrice, LocalTime _time){
        bidder = _bidder;
        bidPrice = _bidPrice;
        time = _time;
    }

    public User getBidder(){
        return bidder;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public LocalTime getTime() {
        return time;
    }
}
