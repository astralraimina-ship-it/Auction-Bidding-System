package com.auction.server;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserDAO userDAO = new UserDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private BidDAO bidDAO = new BidDAO();

    ClientHandler(Socket _socket) {
        socket = _socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println(request);
//                Sử dụng định dạng BID;itemId;userId;bidAmount
                String[] part = request.split(";");
                String command = part[0];
                if (command.equals("BID")) {
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double bidAmount = Double.parseDouble(part[3]);
                    boolean success = bidDAO.placeBid(itemId, userId, bidAmount);
                    if (success){
                        AuctionServer.broadcast("UPDATE");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client mất kết nối");
            AuctionServer.removeClient(this);
        }finally {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    //    Gửi tin nhắn
    public void sendMessage(String msg) {
        System.out.println("handler:" + msg);
        if (out != null && !socket.isClosed()) {
            out.println(msg);
            out.flush();
        }
    }
}
