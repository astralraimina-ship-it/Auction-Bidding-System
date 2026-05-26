package com.auction.server;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.auction.server.observer.BidObserver;
import com.auction.server.observer.BidPublisher;

// ĐÃ SỬA: Thực thi giao diện BidObserver để biến Class này thành một "Người nghe đài"
public class ClientHandler implements Runnable, BidObserver {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserDAO userDAO = new UserDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private BidDAO bidDAO = new BidDAO();

    ClientHandler(Socket _socket) {
        socket = _socket;
        // THÊM ĐỒNG BỘ: Tự động đăng ký luồng Client này vào Trạm phát sóng trung tâm khi kết nối thành công
        BidPublisher.getInstance().registerObserver(this);
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

                        // ĐÃ NÂNG CẤP: Sử dụng Observer Pattern thay thế cho hàm broadcast cũ để cập nhật UI Realtime
                        BidPublisher.getInstance().notifyObservers();

                        // Giữ nguyên cơ chế tín hiệu đặc biệt khác nếu có
                        AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + bidAmount);
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
                    double amount = com.auction.util.FormatUtils.parseDoubleSafe(part[3]);

                    System.out.println(">>> [SERVER XỬ LÝ PAY] ItemID: " + itemId + " | UserID: " + userId + " | Số tiền nhận: " + amount);

                    // Gọi hàm xử lý Transaction dưới DB
                    boolean success = itemDAO.payForItem(itemId, userId, amount);

                    if (success) {
                        // Gửi mã chính xác để Client chặn xử lý luồng hiển thị Alert
                        this.sendMessage("PAY_SUCCESS");

                        // ĐÃ NÂNG CẤP: Sử dụng Observer Pattern để yêu cầu tất cả các máy làm mới bảng dữ liệu
                        BidPublisher.getInstance().notifyObservers();
                    } else {
                        this.sendMessage("PAY_FAILED;Số dư ví không đủ hoặc đơn hàng đã xử lý trước đó!");
                    }
                } else if (request.startsWith("USER_UPDATED")) {
                    String[] part = request.split(";");
                    String id = part[1];
                    String status = part[2];

                    // ĐÃ NÂNG CẤP: Dùng Observer Pattern thông báo cập nhật dữ liệu người dùng
                    BidPublisher.getInstance().notifyObservers();
                } else if (request.equals("NEW_ITEM")){
                    // ĐÃ NÂNG CẤP: Dùng Observer Pattern thông báo có món đồ mới được đăng bán
                    BidPublisher.getInstance().notifyObservers();
                }
                else if (request.equals("NEW_USER")){
                    BidPublisher.getInstance().notifyObservers();
                }
                else if (request.equals("TRANSACTION_UPDATED")){
                    // ĐÃ NÂNG CẤP: Dùng Observer Pattern thông báo có giao dịch mới vừa hoàn thành
                    BidPublisher.getInstance().notifyObservers();
                }
                else if (request.equals("UPDATE")){
                    BidPublisher.getInstance().notifyObservers();
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
                // THÊM ĐỒNG BỘ: Hủy đăng ký khỏi Trạm phát sóng khi Client thoát ứng dụng để tránh rò rỉ bộ nhớ
                BidPublisher.getInstance().removeObserver(this);

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

    // THÊM HÀM NÀY: Hàm bắt buộc triển khai của interface BidObserver
    @Override
    public void onNotificationReceived() {
        // Khi Trạm phát sóng trung tâm hô "notify", gửi lệnh REFRESH viết hoa chuẩn giao thức cũ xuống Client
        this.sendMessage("REFRESH");
    }
}