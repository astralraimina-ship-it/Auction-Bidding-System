package com.auction.common.auction;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.item.Item;
import com.auction.common.user.User;

import java.time.LocalTime;
import java.util.LinkedList;

public class Auction {
    private User seller;
    private Item item;
    private double startPrice;
    private LocalTime closedTime;
    private LinkedList<BidTransaction> history = new LinkedList<>();

    public Auction(User _seller,Item _item, double _startPrice, LocalTime _closedTime){
        seller = _seller;
        item = _item;
        startPrice = _startPrice;
        closedTime = _closedTime;

        BidTransaction sell = new BidTransaction(seller.getName(), startPrice, LocalTime.now());
    }
    public double getCurrentPrice() {
        double currentPrice;
        return currentPrice;
    }

    public BidTransaction getHighestBid() {
        double highestBid;
        return highestBid;

    }
//    Xử lý logic đấu giá
    public void update(User user, double bidPrice){
        try{
//            Kiểm tra thời điểm đấu giá
            LocalTime time = LocalTime.now();
            if (closedTime.compareTo(time) < 0){
                throw new AuctionClosedException();
            }

//            Lấy lượt đấu giá trước
            BidTransaction previousBid = history.peek();
            double previousPrice = previousBid.getBidPrice();

//          so sánh giá với lượt trước
            if (previousPrice > bidPrice){
                throw new InvalidBidException();
            }
            BidTransaction newBid = new BidTransaction(user.getName(), bidPrice, LocalTime.now());
            history.push(newBid);
        }
        catch (InvalidBidException e){
            System.out.println(e.getMessage());
        }
        catch (AuctionClosedException e){
            System.out.println(e.getMessage());
        }
    }

}
