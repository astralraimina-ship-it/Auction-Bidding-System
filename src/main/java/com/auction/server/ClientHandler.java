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
            // Vòng lặp lắng nghe lệnh từ Client
            while ((request = in.readLine()) != null) {
//                Sử dụng định dạng BID;itemId;userId;bidAmount
                String[] part = request.split(";");
                String command = part[0];
                if (command.equals("BID")) {
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double bidAmount = Double.parseDouble(part[3]);
                    boolean success = bidDAO.placeBid(itemId, userId, bidAmount);
                    if (success){
                        this.sendMessage("Notify;BẠN đã đặt giá thành công: " + String.format("%,.0f", bidAmount) + " VNĐ");
                        AuctionServer.broadcast("Update");
                    }
                    else{
                        this.sendMessage("Error;Đặt giá thất bại! Có thể phiên đấu giá đã đóng trên máy chủ.");
                    }
                }
            }
        } catch (IOException e) {
            // Lỗi này xảy ra khi Client tắt đột ngột (nhấn X, mất mạng)
            System.out.println("Client mất kết nối");
        }
        /*
         * KHỐI FINALLY: Luôn chạy dù có lỗi hay không.
         * Đảm bảo khi Thread kết thúc, client PHẢI được xóa khỏi danh sách của Server
         * và giải phóng các tài nguyên (Socket, Stream).
         */
        finally {
            try {
                AuctionServer.removeClient(this);
                socket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
