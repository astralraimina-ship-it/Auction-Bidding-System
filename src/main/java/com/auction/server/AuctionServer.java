package com.auction.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args){
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
