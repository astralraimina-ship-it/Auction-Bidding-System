package com.auction.server;

import com.auction.database.ItemDAO;

import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args){
        new Thread(() -> {
            ItemDAO itemDAO = new ItemDAO();
            System.out.println(">>> [Hệ thống] Luồng tự động đóng phiên đấu giá đã bắt đầu chạy...");
            while (true) {
                try {
                    // Sử dụng hàm bạn đã sửa với Timestamp truyền vào
                    boolean hasExpired = itemDAO.checkAndCloseExpiredItems();

                    if (hasExpired) {
                        // Nếu có món đồ bị đóng, báo cho tất cả client load lại
                        AuctionServer.broadcast("Refresh");
                    }

                    Thread.sleep(10000); // 10 giây quét 1 lần
                } catch (Exception e) {
                    System.err.println("Lỗi luồng quét sản phẩm: " + e.getMessage());
                }
            }
        }).start();

        try (ServerSocket server = new ServerSocket(PORT)){
            while (true) {
                Socket socket = server.accept();
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                new Thread(client).start();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    //    Gửi tin nhắn cho toàn bộ client
    public static void broadcast(String msg){
        for (ClientHandler client:clients){
            client.sendMessage(msg);
        }
    }

//   loại bỏ ClientHandler khỏi danh sách quản lý khi client ngắt kết nối
//    tránh lỗi gửi tin nhắn (Broadcast) vào một kết nối đã chết
    public static void removeClient(ClientHandler client){
        clients.remove(client);
    }

    public static int getPort(){
        return PORT;
    }
}
