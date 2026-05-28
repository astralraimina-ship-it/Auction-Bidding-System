package com.auction.server;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.server.observer.BidObserver;
import com.auction.server.observer.BidPublisher;

public class ClientHandler implements Runnable, BidObserver {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UserDAO userDAO = new UserDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private BidDAO bidDAO = new BidDAO();

    ClientHandler(Socket _socket) {
        socket = _socket;
        BidPublisher.getInstance().registerObserver(this);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String request;

            while ((request = in.readLine()) != null) {
                System.out.println(request);

                if ("PING".equals(request)) {
                    continue;
                }

                // ================================================================
                // 2. XỬ LÝ ĐẶT GIÁ THỦ CÔNG (BID)
                // ================================================================
                if (request.startsWith("BID")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double bidAmount = parseDoubleSafe(part[3]);

                    AuctionState state = AuctionServer.activeAuctions.get(itemId);
                    if (state == null) {
                        this.sendMessage("ERROR;Phòng đấu giá không tồn tại hoặc đã kết thúc!");
                        continue;
                    }

                    synchronized (state){
                        double stepPrice = state.getStepPrice();

                        if (state.isTopBidderAuto()){
                            double maxAutoBudget = state.getTopAutoMaxBudget();
                            int highestBidderId = state.getHighestBidderId();
                            if (bidAmount > maxAutoBudget){
                                state.setManualTopBidder(userId, bidAmount);
                                AuctionServer.activeAutoBids.get(itemId).remove(highestBidderId);

                                bidDAO.deactivateAutoBid(itemId, highestBidderId);

                                this.sendMessage("AUTOBID_STATUS;INACTIVE;" + userId);
                                bidDAO.placeBid(itemId, userId, bidAmount);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + bidAmount);

                                if (state.isBin()){
                                    closeAuction(itemId, userId);
                                }
                            }
                            else if (bidAmount == maxAutoBudget) {
                                state.setCurrentPrice(maxAutoBudget);
                                bidDAO.placeBid(itemId, highestBidderId, maxAutoBudget);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + highestBidderId + ";" + maxAutoBudget);
                                if (state.isBin()){
                                    closeAuction(itemId, highestBidderId);
                                }
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
                            if (state.isBin()){
                                closeAuction(itemId, userId);
                            }
                        }
                        BidPublisher.getInstance().notifyObservers();
                    }
                }
                // ================================================================
                // 3. XỬ LÝ LỆNH CẤU HÌNH AUTO-BID TỪ CLIENT GỬI LÊN
                // ================================================================
                else if (request.startsWith("SET_AUTOBID")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double autoStep = parseDoubleSafe(part[3]);
                    double stopPrice = parseDoubleSafe(part[4]);

                    AuctionState state = AuctionServer.activeAuctions.get(itemId);
                    if (state == null){
                        this.sendMessage("ERROR;Phòng đấu giá không tồn tại hoặc đã kết thúc!");
                        continue;
                    }

                    synchronized (state) {
                        double stepPrice = state.getStepPrice();
                        double currentPrice = state.getCurrentPrice();
                        int highestBidderId = state.getHighestBidderId();
                        double currentTopBudget = state.getTopAutoMaxBudget();

                        if (stopPrice <= currentPrice){
                            this.sendMessage("ERROR;Ngưỡng dừng Auto-Bid (" + stopPrice + ") phải lớn hơn giá hiện tại (" + currentPrice + ")!");
                            continue;
                        }

                        if (highestBidderId == userId) {
                            state.setAutoTopBidder(userId, currentPrice, stopPrice);
                            AuctionServer.activeAutoBids.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>()).put(userId, stopPrice);

                            bidDAO.saveOrUpdateAutoBid(itemId, userId, stopPrice);

                            this.sendMessage("AUTOBID_STATUS;ACTIVE;" + userId + ";" + stopPrice);
                            BidPublisher.getInstance().notifyObservers();
                            continue;
                        }

                        if (state.isTopBidderAuto()){
                            if (stopPrice == currentTopBudget) {
                                state.setCurrentPrice(stopPrice);
                                this.sendMessage("AUTOBID_STATUS;INACTIVE;" + userId);
                                bidDAO.placeBid(itemId, highestBidderId, stopPrice);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + highestBidderId + ";" + stopPrice);
                                if (state.isBin()){
                                    closeAuction(itemId, highestBidderId);
                                }
                            }
                            else if (stopPrice > currentTopBudget){
                                double newPrice = currentTopBudget + stepPrice;
                                if (newPrice > stopPrice) {
                                    newPrice = stopPrice;
                                }
                                state.setAutoTopBidder(userId, newPrice, stopPrice);
                                AuctionServer.activeAutoBids.get(itemId).remove(highestBidderId);
                                AuctionServer.activeAutoBids.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>()).put(userId, stopPrice);

                                bidDAO.saveOrUpdateAutoBid(itemId, userId, stopPrice);
                                bidDAO.deactivateAutoBid(itemId, highestBidderId);

                                this.sendMessage("AUTOBID_STATUS;ACTIVE;" + userId + ";" + stopPrice);

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
                            if (newPrice > stopPrice) {
                                newPrice = stopPrice;
                            }
                            state.setAutoTopBidder(userId, newPrice, stopPrice);
                            AuctionServer.activeAutoBids.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>()).put(userId, stopPrice);

                            bidDAO.saveOrUpdateAutoBid(itemId, userId, stopPrice);

                            this.sendMessage("AUTOBID_STATUS;ACTIVE;" + userId + ";" + stopPrice);

                            bidDAO.placeBid(itemId, userId, newPrice);
                            AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + newPrice);
                            if (state.isBin()){
                                closeAuction(itemId, userId);
                            }
                        }
                        BidPublisher.getInstance().notifyObservers();
                    }
                }
                // ================================================================
                // 4. XỬ LÝ DỪNG AUTO-BID THỦ CÔNG
                // ================================================================
                else if (request.startsWith("STOP_AUTOBID;")) {
                    String[] parts = request.split(";");
                    int itemId = Integer.parseInt(parts[1]);
                    int userId = Integer.parseInt(parts[2]);

                    if (AuctionServer.activeAutoBids.containsKey(itemId)) {
                        AuctionServer.activeAutoBids.get(itemId).remove(userId);
                        AuctionState state = AuctionServer.activeAuctions.get(itemId);
                        if (state != null) {
                            synchronized (state) {
                                if (state.getHighestBidderId() == userId) {
                                    state.setManualTopBidder(userId, state.getCurrentPrice());
                                }
                            }
                        }
                    }

                    bidDAO.deactivateAutoBid(itemId, userId);

                    this.sendMessage("AUTOBID_STATUS;INACTIVE;" + userId);
                    System.out.println("User " + userId + " đã TẮT Auto-Bid của sản phẩm " + itemId);
                    BidPublisher.getInstance().notifyObservers();
                }
                // ================================================================
                // 5. KIỂM TRA TRẠNG THÁI AUTO-BID ĐỂ HIỂN THỊ KÈM BUDGET
                // ================================================================
                else if (request.startsWith("CHECK_AUTOBID_STATUS;")) {
                    String[] parts = request.split(";");
                    int itemId = Integer.parseInt(parts[1]);
                    int userId = Integer.parseInt(parts[2]);

                    boolean isActive = false;
                    double savedBudget = 0;
                    if (AuctionServer.activeAutoBids.containsKey(itemId)) {
                        if (AuctionServer.activeAutoBids.get(itemId).containsKey(userId)) {
                            isActive = true;
                            savedBudget = AuctionServer.activeAutoBids.get(itemId).get(userId);
                        }
                    }

                    if (isActive) {
                        this.sendMessage("AUTOBID_STATUS;ACTIVE;" + userId + ";" + savedBudget);
                    } else {
                        this.sendMessage("AUTOBID_STATUS;INACTIVE;" + userId);
                    }
                }
                // ================================================================
                // 6. XỬ LÝ MUA NGAY (BIN) TỪ BUTTON CLIENT GỬI LÊN
                // ================================================================
                else if (request.startsWith("BIN")){
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double binPrice = parseDoubleSafe(part[3]);

                    AuctionState state = AuctionServer.activeAuctions.get(itemId);
                    if (state == null) {
                        this.sendMessage("ERROR;Phòng đấu giá không tồn tại hoặc đã kết thúc!");
                        continue;
                    }

                    synchronized (state) {
                        if (state.isTopBidderAuto()) {
                            double maxAutoBudget = state.getTopAutoMaxBudget();
                            int highestBidderId = state.getHighestBidderId();

                            if (maxAutoBudget >= binPrice) {
                                state.setCurrentPrice(binPrice);
                                bidDAO.placeBid(itemId, highestBidderId, binPrice);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + highestBidderId + ";" + binPrice);
                                closeAuction(itemId, highestBidderId);
                                BidPublisher.getInstance().notifyObservers();
                                continue;
                            }
                        }

                        boolean success = bidDAO.placeBid(itemId, userId, binPrice);
                        if (success){
                            closeAuction(itemId, userId);
                        }
                        BidPublisher.getInstance().notifyObservers();
                    }
                }
                // ================================================================
                // 7. KHI CLIENT GỬI LỆNH XIN BẢNG SẢN PHẨM THAM GIA
                // ================================================================
                else if (request.startsWith("GET_PARTICIPATED_AUCTIONS")) {
                    String[] parts = request.split(";");
                    int userId = Integer.parseInt(parts[1]);

                    javafx.collections.ObservableList<com.auction.common.item.Item> items = itemDAO.getParticipatedAndLostItems(userId);

                    StringBuilder sb = new StringBuilder("PARTICIPATED_DATA;");

                    if (items != null && !items.isEmpty()) {
                        for (com.auction.common.item.Item item : items) {
                            sb.append(item.getId()).append(",")
                                    .append(item.getName()).append(",")
                                    .append(item.getCurrentPrice()).append(",")
                                    .append(item.getStep()).append(",")
                                    .append(item.getBinPrice()).append(",")
                                    .append(item.getStatus()).append("|");
                        }

                        if (sb.charAt(sb.length() - 1) == '|') {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                    }

                    this.sendMessage(sb.toString());
                }
                // ================================================================
                // 8. 🔥 THÊM MỚI: XỬ LÝ ẢNH (UPLOAD & DOWNLOAD TỪ CLIENT)
                // ================================================================
                else if (request.startsWith("UPLOAD_IMAGE;")) {
                    String[] parts = request.split(";", 3); // Cắt chuỗi thành 3 phần: UPLOAD_IMAGE, filename, base64
                    if (parts.length == 3) {
                        String fileName = parts[1];
                        String base64Data = parts[2];
                        try {
                            // Tạo thư mục "images" nếu chưa có
                            File dir = new File("images");
                            if (!dir.exists()) dir.mkdirs();

                            // Giải mã chuỗi base64 thành mảng byte và ghi ra ổ cứng
                            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
                            File imageFile = new File(dir, fileName);
                            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                                fos.write(decodedBytes);
                            }
                            System.out.println(">>> [SERVER] Đã lưu ảnh thành công: " + fileName);
                        } catch (Exception e) {
                            System.out.println(">>> [SERVER] Lỗi lưu ảnh: " + e.getMessage());
                        }
                    }
                }
                else if (request.startsWith("GET_IMAGE;")) {
                    String[] parts = request.split(";");
                    if (parts.length >= 2) {
                        String fileName = parts[1];
                        try {
                            // Tìm file ảnh trong thư mục "images"
                            File imageFile = new File("images", fileName);
                            if (imageFile.exists()) {
                                // Đọc lên, mã hóa thành base64 và gửi trả Client
                                byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
                                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                                this.sendMessage("IMAGE_RESPONSE;" + fileName + ";" + base64Data);
                                System.out.println(">>> [SERVER] Đã gửi ảnh cho client: " + fileName);
                            } else {
                                System.out.println(">>> [SERVER] Không tìm thấy ảnh yêu cầu: " + fileName);
                            }
                        } catch (Exception e) {
                            System.out.println(">>> [SERVER] Lỗi đọc/gửi ảnh: " + e.getMessage());
                        }
                    }
                }
                // ================================================================
                // 9. CÁC LỆNH KHÁC
                // ================================================================
                else if (request.startsWith("PAY")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double amount = com.auction.util.FormatUtils.parseDoubleSafe(part[3]);

                    System.out.println(">>> [SERVER XỬ LÝ PAY] ItemID: " + itemId + " | UserID: " + userId + " | Số tiền nhận: " + amount);
                    boolean success = itemDAO.payForItem(itemId, userId, amount);

                    if (success) {
                        this.sendMessage("PAY_SUCCESS");
                        BidPublisher.getInstance().notifyObservers();
                    } else {
                        this.sendMessage("PAY_FAILED;Số dư ví không đủ hoặc đơn hàng đã xử lý trước đó!");
                    }
                } else if (request.startsWith("USER_UPDATED")) {
                    BidPublisher.getInstance().notifyObservers();
                } else if (request.startsWith("NEW_ITEM")){
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    double startPrice = Double.parseDouble(part[2]);
                    double step = Double.parseDouble(part[3]);
                    double binPrice = Double.parseDouble(part[4]);

                    AuctionServer.activeAuctions.put(itemId, new AuctionState(itemId, startPrice, step, binPrice));
                    BidPublisher.getInstance().notifyObservers();
                }
                else if (request.equals("NEW_USER") || request.equals("TRANSACTION_UPDATED") || request.equals("UPDATE")){
                    BidPublisher.getInstance().notifyObservers();
                }
            }
        } catch (IOException e) {
            System.out.println("Client mất kết nối");
        }
        finally {
            try {
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

    public void sendMessage(String msg) {
        System.out.println("handler:" + msg);
        if (out != null && socket != null && !socket.isClosed()) {
            out.println(msg);
            out.flush();
        }
    }

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

    @Override
    public void onNotificationReceived() {
        this.sendMessage("REFRESH");
    }
}