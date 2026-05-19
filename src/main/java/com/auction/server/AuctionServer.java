package com.auction.server;

import com.auction.database.ItemDAO;
import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionServer {
    private static final int PORT = 12345;
    // Dùng Set bình thường vì chúng ta sẽ tự synchronized bằng tay khi duyệt vòng lặp
    private static final Set<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args){
        new Thread(() -> {
            ItemDAO itemDAO = new ItemDAO();
            System.out.println(">>> [Hệ thống] Luồng tự động đóng phiên & quét phạt 24h đã bắt đầu chạy...");

            // Tạo biến đếm để giảm tần suất quét phạt (đóng phiên cần nhanh 10s/lần, phạt bùng hàng chỉ cần 5 phút/lần là đủ)
            int checkViolationCounter = 0;

            while (true) {
                try {
                    // 1. Tự động kiểm tra đóng các phiên hết hạn (Chạy liên tục mỗi 10 giây)
                    boolean hasExpired = itemDAO.checkAndCloseExpiredItems();
                    if (hasExpired) {
                        AuctionServer.broadcast("Refresh");
                    }

                    // 2. Tự động kiểm tra phạt bùng hàng 24h (Cứ mỗi 30 chu kỳ = 300 giây = 5 phút quét 1 lần)
                    checkViolationCounter++;
                    if (checkViolationCounter >= 30) {
                        System.out.println(">>> [Hệ thống] Đang tự động kiểm tra các đơn hàng quá hạn thanh toán (24h)...");
                        ItemDAO.processExpiredPayments();
                        checkViolationCounter = 0; // Reset bộ đếm
                    }

                    Thread.sleep(10000); // 10 giây quét 1 lần
                } catch (InterruptedException e) {
                    System.err.println("Luồng quét bị ngắt: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    System.err.println("Lỗi kết nối DB trong luồng quét sản phẩm/phạt: " + e.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {} // Đợi thêm trước khi thử lại tránh treo máy
                }
            }
        }).start();

        try (ServerSocket server = new ServerSocket(PORT)){
            System.out.println("Server đang chạy tại port " + PORT + "...");
            while (true) {
                Socket socket = server.accept();
                ClientHandler client = new ClientHandler(socket);

                // Đồng bộ hóa khi thêm client mới
                synchronized (clients) {
                    clients.add(client);
                    System.out.println("Số lượng client kết nối: " + clients.size());
                }

                new Thread(client).start();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Gửi tin nhắn cho toàn bộ client một cách an toàn (Thread-safe)
     */
    public static void broadcast(String msg){
        List<ClientHandler> clientsCopy;

        // Chỉ lock danh sách trong tích tắc để copy, tránh block các luồng kết nối khác
        synchronized (clients) {
            clientsCopy = new ArrayList<>(clients);
        }

        // Duyệt trên bản copy nên không sợ lỗi ConcurrentModificationException
        for (ClientHandler client : clientsCopy){
            client.sendMessage(msg);
        }
    }

    public static void removeClient(ClientHandler client){
        synchronized (clients) {
            clients.remove(client);
            System.out.println("Một client đã thoát. Còn lại: " + clients.size());
        }
    }

    public static int getPort(){
        return PORT;
    }
}