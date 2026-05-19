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
                System.out.println(request);
//                Sử dụng định dạng BID;itemId;userId;bidAmount
                if (request.startsWith("BID")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);

                    // SỬA AN TOÀN: Đọc số double bất chấp cấu hình máy Server
                    double bidAmount = parseDoubleSafe(part[3]);

                    boolean success = bidDAO.placeBid(itemId, userId, bidAmount);
                    if (success){
                        this.sendMessage("Notify;BẠN đã đặt giá thành công: " + String.format("%,.0f", bidAmount) + " VNĐ");
                        AuctionServer.broadcast("Refresh");
                        AuctionServer.broadcast("AntiSnipe");
                    }
                    else{
                        this.sendMessage("Error;Đặt giá thất bại! Có thể phiên đấu giá đã đóng trên máy chủ.");
                    }
                }
                else if (request.startsWith("BIN")){
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);

                    // SỬA AN TOÀN: Đọc số double bất chấp cấu hình máy Server
                    double binPrice = parseDoubleSafe(part[3]);

                    boolean success = bidDAO.placeBid(itemId, userId, binPrice);
                    if (success){
                        boolean updateSuccess = itemDAO.closeAuction(itemId, userId);
                        if (updateSuccess){
                            this.sendMessage("Notify;CHÚC MỪNG! Bạn đã mua đứt thành công.");
                            AuctionServer.broadcast("Closed");
                        }
                    }
                }
                // ĐÃ SỬA ĐỒNG BỘ: Gửi đúng mã lệnh PAY_SUCCESS / PAY_FAILED về Client
                else if (request.startsWith("PAY")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);

                    // ĐÃ SỬA CHÍ MẠNG: Ép Server bóc tách số tiền theo định dạng chuẩn US (dấu chấm thập phân)
                    double amount = parseDoubleSafe(part[3]);

                    System.out.println(">>> [SERVER XỬ LÝ PAY] ItemID: " + itemId + " | UserID: " + userId + " | Số tiền nhận: " + amount);

                    // Gọi hàm xử lý Transaction dưới DB
                    boolean success = itemDAO.payForItem(itemId, userId, amount);

                    if (success) {
                        // Gửi mã chính xác để Client chặn xử lý luồng hiển thị Alert
                        this.sendMessage("PAY_SUCCESS");
                        // Đồng bộ làm mới bảng của mọi Client khác đang mở app
                        AuctionServer.broadcast("Refresh");
                    } else {
                        this.sendMessage("PAY_FAILED;Số dư ví không đủ hoặc đơn hàng đã xử lý trước đó!");
                    }
                }
                else if (request.equals("NEW_ITEM")){
                    AuctionServer.broadcast("Refresh");
                }
            }
        } catch (IOException e) {
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
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Gửi tin nhắn
    public void sendMessage(String msg) {
        System.out.println("handler:" + msg);
        if (out != null && socket != null && !socket.isClosed()) {
            out.println(msg);
            out.flush();
        }
    }

    // Hàm phụ trợ bóc tách số double an toàn, ép sử dụng dấu chấm thập phân theo chuẩn quốc tế
    private double parseDoubleSafe(String value) {
        try {
            // Sử dụng java.util.Scanner với Locale.US để ép dấu chấm làm dấu thập phân cố định
            try (java.util.Scanner scanner = new java.util.Scanner(value)) {
                scanner.useLocale(java.util.Locale.US);
                if (scanner.hasNextDouble()) {
                    return scanner.nextDouble();
                }
            }
            // Fallback nếu scanner lỗi
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) {
            return Double.parseDouble(value);
        }
    }
}