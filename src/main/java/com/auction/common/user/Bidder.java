package com.auction.common.user;

import com.auction.common.auction.*;
import com.auction.common.interfaces.Transaction;

import java.time.LocalTime;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Bidder extends User implements Transaction {
    private double money = 0.0;
    public Bidder(String _name, String _id, String _password){
        super(_name, _id, _password);
    }

    public void bid(Auction auction){
        Scanner sc = new Scanner(System.in);
        try{
            System.out.print(name + "bid: ");
            double bidPrice = sc.nextDouble();
            if (bidPrice < money){
                auction.update(this, bidPrice);
            }
            else{
                System.out.println("Not enough money");
            }
        }
        catch (InputMismatchException e){
            System.out.println("Invalid bid");
        }
    }

    @Override
    public void addAmount(double _money) {
        money += _money;
        System.out.println(name + " balance: " + money);
    }

    @Override
    public void subtract(double _money) {
        money -= _money;
        System.out.println(name + " balance: " + money);
    }
}
