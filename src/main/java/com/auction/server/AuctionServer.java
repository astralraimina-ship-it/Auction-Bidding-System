package com.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class AuctionServer {
    private static final int PORT = 12345;
    private static ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args){
        try (ServerSocket server = new ServerSocket(PORT)){
            Socket socket = server.accept();
            ClientHandler client = new ClientHandler(socket);
            clients.add(client);
            new Thread(client).start();
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

    public static void removeClient(ClientHandler client){
        clients.remove(client);
    }
}
