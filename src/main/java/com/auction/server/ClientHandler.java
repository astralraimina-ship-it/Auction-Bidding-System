package com.auction.server;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

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

                // 1. XỬ LÝ GÓI TIN KIỂM TRA MẠNG ẨN (PING): Im lặng bỏ qua, tránh crash cắt chuỗi dấu ;
                if ("PING".equals(request)) {
                    continue;
                }

                // 2. XỬ LÝ ĐẶT GIÁ THỦ CÔNG (BID)
                if (request.startsWith("BID")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);

                    // SỬA AN TOÀN: Đọc số double bất chấp cấu hình máy Server
                    double bidAmount = parseDoubleSafe(part[3]);
                    AuctionState state = AuctionServer.activeAuctions.get(itemId);
                    if (state == null) {
                        this.sendMessage("ERROR;Phòng đấu giá không tồn tại hoặc đã kết thúc!");
                        return;
                    }

                    synchronized (state){
                        double currentPrice = state.getCurrentPrice();
                        double stepPrice = state.getStepPrice();

                        if (state.isTopBidderAuto()){
                            double maxAutoBudget = state.getTopAutoMaxBudget();
                            int highestBidderId = state.getHighestBidderId();
                            if (bidAmount > maxAutoBudget){
                                state.setManualTopBidder(userId, bidAmount);
                                AuctionServer.activeAutoBids.get(itemId).remove(highestBidderId);
                                this.sendMessage("AUTOBID_STATUS;INACTIVE;" + userId);
                                bidDAO.placeBid(itemId, userId, bidAmount);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + bidAmount);
                            }
                            else{
                                double newPrice = bidAmount + stepPrice;
                                if (newPrice > maxAutoBudget){
                                    newPrice = maxAutoBudget;
                                }
                                state.setCurrentPrice(newPrice);
                                bidDAO.placeBid(itemId, userId, bidAmount);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + bidAmount);
                                bidDAO.placeBid(itemId, highestBidderId, newPrice);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + highestBidderId + ";" + newPrice);
                                if (state.isBin()){
                                    closeAuction(itemId, highestBidderId);
                                }
                            }
                        }
                        else {
                            state.setManualTopBidder(userId, bidAmount);
                            bidDAO.placeBid(itemId, userId, bidAmount);
                            AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + bidAmount);
                        }
                        BidPublisher.getInstance().notifyObservers();
                    }
                }
                // 3. XỬ LÝ LỆNH CẤU HÌNH AUTO-BID TỪ CLIENT GỬI LÊN
                else if (request.startsWith("SET_AUTOBID")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double autoStep = parseDoubleSafe(part[3]);
                    double stopPrice = parseDoubleSafe(part[4]);

                    AuctionState state = AuctionServer.activeAuctions.get(itemId);
                    if (state == null){
                        this.sendMessage("ERROR;Phòng đấu giá không tồn tại hoặc đã kết thúc!");
                        return;
                    }
                    // 2. KHÓA BỘ NHỚ (Đảm bảo Trọng tài xử lý từng lệnh một, không bị dẫm đạp dữ liệu)
                    synchronized (state) {
                        double stepPrice = state.getStepPrice();
                        double currentPrice = state.getCurrentPrice();
                        int highestBidderId = state.getHighestBidderId();
                        double currentTopBudget = state.getTopAutoMaxBudget();
                        if (stopPrice <= currentPrice){
                            this.sendMessage("ERROR;Ngưỡng dừng Auto-Bid (" + stopPrice + ") phải lớn hơn giá hiện tại (" + currentPrice + ")!");
                            return;
                        }
                        if (state.isTopBidderAuto()){
                            if (stopPrice > currentTopBudget){
                                double newPrice = currentTopBudget + stepPrice;
                                state.setAutoTopBidder(userId, newPrice, stopPrice);
                                AuctionServer.activeAutoBids.get(itemId).remove(highestBidderId);
                                AuctionServer.activeAutoBids.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>()).put(userId, stopPrice);
                                this.sendMessage("AUTOBID_STATUS;INACTIVE;" + highestBidderId);
                                bidDAO.placeBid(itemId, userId, newPrice);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + newPrice);
                                if (state.isBin()){
                                    closeAuction(itemId, userId);
                                }
                            }
                            else{
                                double counterPrice = stopPrice + stepPrice;
                                if (counterPrice > currentTopBudget){
                                    counterPrice = currentTopBudget;
                                }
                                state.setCurrentPrice(counterPrice);
                                this.sendMessage("AUTOBID_STATUS;INACTIVE;" + userId);
                                bidDAO.placeBid(itemId, highestBidderId, counterPrice);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + highestBidderId + ";" + counterPrice);
                                if (state.isBin()){
                                    closeAuction(itemId, highestBidderId);
                                }
                            }
                        }
                        else{
                            double newPrice = currentPrice + stepPrice;
                            state.setAutoTopBidder(userId, newPrice, stopPrice);
                            AuctionServer.activeAutoBids.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>()).put(userId, stopPrice);
                            bidDAO.placeBid(itemId, userId, newPrice);
                            AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + newPrice);
                            if (state.isBin()){
                                closeAuction(itemId, userId);
                            }
                        }
                        BidPublisher.getInstance().notifyObservers();
                    }
                }
                else if (request.startsWith("STOP_AUTOBID;")) {
                    String[] parts = request.split(";");
                    int itemId = Integer.parseInt(parts[1]);
                    int userId = Integer.parseInt(parts[2]);

                    // Xóa khỏi Map trên RAM Server
                    if (AuctionServer.activeAutoBids.containsKey(itemId)) {
                        AuctionServer.activeAutoBids.get(itemId).remove(userId);
                        AuctionState state = AuctionServer.activeAuctions.get(itemId);
                        state.setManualTopBidder(userId, state.getCurrentPrice());
                    }

                    System.out.println("User " + userId + " đã TẮT Auto-Bid của sản phẩm " + itemId);
                }
                else if (request.startsWith("CHECK_AUTOBID_STATUS;")) {
                    String[] parts = request.split(";");
                    int itemId = Integer.parseInt(parts[1]);
                    int userId = Integer.parseInt(parts[2]);

                    boolean isActive = false;
                    if (AuctionServer.activeAutoBids.containsKey(itemId)) {
                        isActive = AuctionServer.activeAutoBids.get(itemId).containsKey(userId);
                    }

                    // Gửi câu trả lời ngược về cho chính Client này
                    // Giả sử hàm gửi dữ liệu về client của bạn tên là 'out.println()' hoặc 'sendToClient()'
                    if (isActive) {
                        this.sendMessage("AUTOBID_STATUS;ACTIVE;" + userId);
                    } else {
                        this.sendMessage("AUTOBID_STATUS;INACTIVE;" + userId);
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
                        closeAuction(itemId, userId);
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
                } else if (request.startsWith("NEW_ITEM")){
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    double startPrice = Double.parseDouble(part[2]);
                    double step = Double.parseDouble(part[3]);
                    double binPrice = Double.parseDouble(part[4]);

                    AuctionServer.activeAuctions.put(itemId, new AuctionState(itemId, startPrice, step, binPrice));
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
    public void closeAuction(int itemId, int userId){
        boolean updateSuccess = itemDAO.closeAuction(itemId, userId);
        if (updateSuccess){
            AuctionServer.broadcast("Closed");
            AuctionServer.activeAuctions.remove(itemId);
            BidPublisher.getInstance().notifyObservers();
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
            try (java.util.Scanner scanner = new java.util.Scanner(value)) {
                scanner.useLocale(java.util.Locale.US);
                if (scanner.hasNextDouble()) {
                    return scanner.nextDouble();
                }
            }
            return Double.parseDouble(value.replace(",", "."));
        } catch (Exception e) {
            return Double.parseDouble(value);
        }
    }

    // Hàm bắt buộc triển khai của interface BidObserver
    @Override
    public void onNotificationReceived() {
        this.sendMessage("REFRESH");
    }
}