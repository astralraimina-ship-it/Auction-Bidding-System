package com.auction.common.auction;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.item.Item;
import com.auction.common.user.Bidder;
import com.auction.common.user.Seller;
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

        BidTransaction sell = new BidTransaction(seller, startPrice, LocalTime.now());
    }

//    Lấy giá cao nhất hiện tại
    public double getCurrentPrice() {
        BidTransaction highestBid = this.getHighestBid();
        if (highestBid == null){
            return startPrice;
        }
        double currentPrice = highestBid.getBidPrice();
        System.out.println(currentPrice);
        return currentPrice;
    }

//    method peekLast để lấy phần tử cuối cùng trong Linkedlist
    public BidTransaction getHighestBid() {
        BidTransaction highestBid = history.peek();
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

            double currentPrice = this.getCurrentPrice();

//          so sánh giá với giá cao nhất hiện tại
            if (currentPrice > bidPrice){
                throw new InvalidBidException();
            }
            BidTransaction newBid = new BidTransaction(user, bidPrice, time);
            history.push(newBid);
        }
        catch (InvalidBidException e){
            System.out.println(e.getMessage());
        }
        catch (AuctionClosedException e){
            System.out.println(e.getMessage());
        }
    }

//    Khi đóng phiên đấu giá thì chuyển tiền bidder -> seller
    public void closed(){
        BidTransaction highestBid = this.getHighestBid();
        if (highestBid != null) {
            User bidder = highestBid.getBidder();
            double price = this.getCurrentPrice();
            ((Bidder) bidder).subtract(price);
            ((Seller) seller).addAmount(price);
            System.out.println("Paid");
        }
        else{
            System.out.println("Cancel");
        }
    }

}
