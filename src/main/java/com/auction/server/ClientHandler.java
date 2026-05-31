package com.auction.server;

import com.auction.common.user.User;
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
    private int userId;

    ClientHandler(Socket _socket) {
        socket = _socket;
        BidPublisher.getInstance().registerObserver(this);
    }

    /**
     * 🔥 HÀM BỔ TRỢ: Tự động khôi phục dữ liệu từ MySQL lên RAM nếu Server vừa restart
     */
    private AuctionState getOrCreateAuctionState(int itemId) {
        AuctionState state = AuctionServer.activeAuctions.get(itemId);
        if (state == null) {
            try {
                com.auction.common.item.Item item = itemDAO.getItemById(itemId);

                if (item != null) {
                    state = new AuctionState(item.getId(), item.getCurrentPrice(), item.getStep(), item.getBinPrice());
                    AuctionServer.activeAuctions.put(itemId, state);
                    System.out.println(">>> [SERVER] Đã tự động khôi phục AuctionState lên RAM từ MySQL cho Item ID: " + itemId);
                }
            } catch (Exception e) {
                System.out.println(">>> [SERVER] Lỗi khi tự động nạp dữ liệu từ DB lên RAM: " + e.getMessage());
            }
        }
        return state;
    }

    /**
     * 🔥 HÀM BỔ TRỢ XỬ LÝ ANTI-SNIPE (Dùng chung cho cả Bid thủ công và Auto-Bid)
     */
    private void checkAndApplyAntiSnipe(int itemId) {
        try {
            com.auction.common.item.Item item = itemDAO.getItemById(itemId);
            if (item != null && item.getEndTime() != null) {
                long timeLeft = item.getEndTime().getTime() - System.currentTimeMillis();
                // Nếu thời gian còn lại dưới 1 phút (60,000 mili-giây) và chưa hết giờ
                if (timeLeft > 0 && timeLeft < 60000) {
                    int minutesToExtend = 2;
                    boolean extendSuccess = itemDAO.extendAuctionTime(itemId, minutesToExtend);
                    if (extendSuccess) {
                        System.out.println(">>> [ANTI-SNIPE SERVER] Sản phẩm ID " + itemId + " đã được gia hạn thêm " + minutesToExtend + " phút!");
                        AuctionServer.broadcast("Notify;Hệ thống: Phát hiện đấu giá sát nút! Tự động gia hạn thêm " + minutesToExtend + " phút.");
                        AuctionServer.broadcast("REFRESH");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(">>> [ANTI-SNIPE] Lỗi kiểm tra gia hạn thời gian: " + e.getMessage());
        }
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

                    // Đổi từ gọi Map trực tiếp sang hàm tự động phục hồi nạp chậm
                    AuctionState state = getOrCreateAuctionState(itemId);
                    if (state == null) {
                        this.sendMessage("ERROR;Phòng đấu giá không tồn tại hoặc đã kết thúc!");
                        continue;
                    }

                    synchronized (state){
                        double stepPrice = state.getStepPrice();

                        // Kích hoạt Anti-Snipe cho lượt bấm đặt giá thủ công hiện tại
                        checkAndApplyAntiSnipe(itemId);

                        if (state.isTopBidderAuto()){
                            double maxAutoBudget = state.getTopAutoMaxBudget();
                            int highestBidderId = state.getHighestBidderId();
                            if (bidAmount > maxAutoBudget){
                                state.setManualTopBidder(userId, bidAmount);
                                if (AuctionServer.activeAutoBids.get(itemId) != null) {
                                    AuctionServer.activeAutoBids.get(itemId).remove(highestBidderId);
                                }

                                bidDAO.deactivateAutoBid(itemId, highestBidderId);

                                AuctionServer.broadcast("AUTOBID_STATUS;INACTIVE;" + highestBidderId);
                                bidDAO.placeBid(itemId, userId, bidAmount);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + bidAmount);

                                if (state.isBin()){
                                    closeAuction(itemId, userId, bidAmount); // ĐÃ SỬA: Pass giá bid cao nhất làm giá đóng phòng công bằng
                                }
                            }
                            else if (bidAmount == maxAutoBudget) {
                                // Trường hợp Auto-bid tự động nâng lên mức maxBudget đè người dùng thủ công
                                checkAndApplyAntiSnipe(itemId); // Kích hoạt Anti-Snipe khi Auto-bid tự nâng giá sát giờ

                                state.setCurrentPrice(maxAutoBudget);
                                bidDAO.placeBid(itemId, highestBidderId, maxAutoBudget);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + highestBidderId + ";" + maxAutoBudget);
                                if (state.isBin()){
                                    closeAuction(itemId, highestBidderId, maxAutoBudget); // ĐÃ SỬA: Chốt theo maxBudget của autobid
                                }
                            }
                            else{
                                // Trường hợp Auto-bid tự động phản đòn nâng giá đè lên người dùng thủ công
                                checkAndApplyAntiSnipe(itemId); // Kích hoạt Anti-Snipe khi Auto-bid tự nâng giá sát giờ

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
                                    closeAuction(itemId, highestBidderId, newPrice); // ĐÃ SỬA: Chốt theo giá phản đòn của autobid
                                }
                            }
                        }
                        else {
                            state.setManualTopBidder(userId, bidAmount);
                            bidDAO.placeBid(itemId, userId, bidAmount);
                            AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + bidAmount);
                            if (state.isBin()){
                                closeAuction(itemId, userId, bidAmount); // ĐÃ SỬA: Gửi giá bid trực tiếp
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

                    // Đổi sang hàm tự động phục hồi
                    AuctionState state = getOrCreateAuctionState(itemId);
                    if (state == null){
                        this.sendMessage("ERROR;Phòng đấu giá không tồn tại hoặc đã kết thúc!");
                        continue;
                    }

                    synchronized (state) {
                        // CHẶN CỨNG: Không cho phép cài Auto-bid cao hơn giá Mua đứt (Bin Price)
                        if (stopPrice > state.getBinPrice()) {
                            System.out.println(">>> [SERVER] Auto-Bid quá giá Bin, tự động hạ stopPrice xuống bằng Bin Price: " + state.getBinPrice());
                            stopPrice = state.getBinPrice();
                        }

                        double stepPrice = state.getStepPrice();
                        double currentPrice = state.getCurrentPrice();
                        int highestBidderId = state.getHighestBidderId();
                        double currentTopBudget = state.getTopAutoMaxBudget();

                        if (stopPrice <= currentPrice){
                            this.sendMessage("ERROR;Ngưỡng dừng Auto-Bid (" + stopPrice + ") phải lớn hơn giá hiện tại (" + currentPrice + ")!");
                            continue;
                        }

                        // Kích hoạt dọn dẹp: Tự động hủy và thông báo cho các đối thủ có budget cũ thấp hơn stopPrice mới
                        checkAndDisableLoserAutoBids(itemId, userId, stopPrice);

                        // Kích hoạt Anti-Snipe khi có người thiết lập/kích hoạt Auto-bid đè giá sát nút giờ đóng phiên
                        checkAndApplyAntiSnipe(itemId);

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
                                    closeAuction(itemId, highestBidderId, stopPrice); // ĐÃ SỬA: Đồng bộ giá chốt
                                }
                            }
                            else if (stopPrice > currentTopBudget){
                                double newPrice = currentTopBudget + stepPrice;
                                if (newPrice > stopPrice) {
                                    newPrice = stopPrice;
                                }
                                state.setAutoTopBidder(userId, newPrice, stopPrice);
                                if (AuctionServer.activeAutoBids.get(itemId) != null) {
                                    AuctionServer.activeAutoBids.get(itemId).remove(highestBidderId);
                                }
                                bidDAO.deactivateAutoBid(itemId, highestBidderId);
                                AuctionServer.broadcast("AUTOBID_STATUS;INACTIVE;" + highestBidderId);

                                AuctionServer.activeAutoBids.computeIfAbsent(itemId, k -> new ConcurrentHashMap<>()).put(userId, stopPrice);

                                bidDAO.saveOrUpdateAutoBid(itemId, userId, stopPrice);

                                this.sendMessage("AUTOBID_STATUS;ACTIVE;" + userId + ";" + stopPrice);

                                bidDAO.placeBid(itemId, userId, newPrice);
                                AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + userId + ";" + newPrice);
                                if (state.isBin()){
                                    closeAuction(itemId, userId, newPrice); // ĐÃ SỬA: Đồng bộ giá chốt
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
                                    closeAuction(itemId, highestBidderId, counterPrice); // ĐÃ SỬA: Đồng bộ giá chốt
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
                                closeAuction(itemId, userId, newPrice); // ĐÃ SỬA: Đồng bộ giá chốt
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
                        AuctionState state = getOrCreateAuctionState(itemId);
                        if (state != null) {
                            synchronized (state) {
                                if (state.getHighestBidderId() == userId) {
                                    state.setManualTopBidder(userId, state.getCurrentPrice());
                                }
                            }
                        }
                    }

                    bidDAO.deactivateAutoBid(itemId, userId);
                    AuctionServer.broadcastToUser(userId, "AUTOBID_DISABLED;" + itemId);

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

                    // Đổi sang hàm tự động phục hồi
                    AuctionState state = getOrCreateAuctionState(itemId);
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
                                closeAuction(itemId, highestBidderId, binPrice); // ĐÃ SỬA: Chốt luôn mức giá mua đứt binPrice cho người treo Autobid cao
                                BidPublisher.getInstance().notifyObservers();
                                continue;
                            }
                        }

                        boolean success = bidDAO.placeBid(itemId, userId, binPrice);
                        if (success){
                            closeAuction(itemId, userId, binPrice); // ĐÃ SỬA THÀNH CÔNG: Gọi hàm 3 tham số và truyền trực tiếp giá trị 1 Triệu (binPrice) vào đây
                        }
                        BidPublisher.getInstance().notifyObservers();
                    }
                }
                // ================================================================
                // 7. KHI CLIENT GỬI LỆNH XIN BẢNG SẢN PHẨM THAM GIA
                // ================================================================
                // ====================================================================
// 7. KHI CLIENT GỬI LỆNH XIN BẢNG SẢN PHẨM THAM GIA (ĐÃ SỬA ĐỂ ĐỒNG BỘ 3 TRẠNG THÁI)
// ====================================================================
                else if (request.startsWith("GET_PARTICIPATED_AUCTIONS")) {
                    String[] parts = request.split(";");
                    int targetUserId = Integer.parseInt(parts[1]);

                    javafx.collections.ObservableList<com.auction.common.item.Item> items = itemDAO.getParticipatedAndLostItems(targetUserId);

                    StringBuilder sb = new StringBuilder("PARTICIPATED_DATA;");

                    if (items != null && !items.isEmpty()) {
                        for (com.auction.common.item.Item item : items) {
                            int itemId = item.getId();

                            // 1. Lấy đường dẫn ảnh an toàn (mặc định là default.png nếu trống)
                            String imagePath = "default.png";
                            if (item.getImagePath() != null && !item.getImagePath().trim().isEmpty()) {
                                imagePath = item.getImagePath();
                            }

                            // 2. Kiểm tra trạng thái Giữ giá theo thời gian thực từ bộ nhớ RAM (AuctionServer.activeAuctions)
                            String holdingFlag = "OUTBID";
                            AuctionState state = AuctionServer.activeAuctions.get(itemId);
                            if (state != null) {
                                if (state.getHighestBidderId() == targetUserId) {
                                    holdingFlag = "HOLDING";
                                }
                            }

                            // 3. Kiểm tra trạng thái AutoBid của User này từ bộ nhớ RAM (AuctionServer.activeAutoBids)
                            String autobidFlag = "NONE";
                            if (AuctionServer.activeAutoBids.containsKey(itemId)) {
                                java.util.Map<Integer, Double> userBids = AuctionServer.activeAutoBids.get(itemId);
                                if (userBids != null && userBids.containsKey(targetUserId)) {
                                    autobidFlag = "AUTOBID";
                                }
                            }

                            // Ghép chuỗi chuẩn 9 trường gửi về cho Client bóc tách:
                            // fields[0]=id, [1]=name, [2]=price, [3]=step, [4]=bin, [5]=status, [6]=imagePath, [7]=holding, [8]=autobid
                            sb.append(itemId).append(",")
                                    .append(item.getName()).append(",")
                                    .append(item.getCurrentPrice()).append(",")
                                    .append(item.getStep()).append(",")
                                    .append(item.getBinPrice()).append(",")
                                    .append(item.getStatus()).append(",")
                                    .append(imagePath).append(",")   // Trường số 6
                                    .append(holdingFlag).append(",") // Trường số 7
                                    .append(autobidFlag).append("|"); // Trường số 8
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
                    String[] parts = request.split(";", 3);
                    if (parts.length == 3) {
                        String fileName = parts[1];
                        String base64Data = parts[2];
                        try {
                            File dir = new File("images");
                            if (!dir.exists()) dir.mkdirs();

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
                            File imageFile = new File("images", fileName);
                            if (imageFile.exists()) {
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
                else if (request.startsWith("LOGIN")) {
                    String[] parts = request.split(";");
                    String username = parts[1];
                    String password = parts[2];

                    // 1. Dùng UserDAO để check Database TẠI SERVER
                    User user = userDAO.authenticate(username, password);

                    if (user != null) {
                        // 2. TÀI KHOẢN ĐÚNG -> Check xem có ai đang dùng không
                        if (AuctionServer.newClients.containsKey(user.getId())) {
                            sendMessage("LOGIN_FAILED;Tài khoản này đang được đăng nhập ở thiết bị khác!");
                        } else {
                            userId = user.getId();
                            // 3. HỢP LỆ -> Lưu vào Map và báo thành công
                            AuctionServer.newClients.put(userId, this);

                            // Gửi dữ liệu về Client (gửi kèm id, tên, role để Client mở giao diện)
                            sendMessage("LOGIN_SUCCESS;" + user.getId() + ";" + user.getUsername() + ";" + user.getRole());
                        }
                    } else {
                        sendMessage("LOGIN_FAILED;Sai tài khoản hoặc mật khẩu!");
                    }
                }
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
                else if (request.startsWith("DELETE_ITEM")){
                    String[] parts = request.split(";");
                    int itemId = Integer.parseInt(parts[1]);

                    // 1. Cập nhật trạng thái sản phẩm trong DB thành 'DELETED' hoặc 'CANCELLED'
                    ItemDAO itemDAO = new ItemDAO();
                    boolean isCancel = itemDAO.cancelItem(itemId);
                    if (isCancel){
                        AuctionServer.broadcast("ITEM_DELETED;" + itemId);
                        AuctionServer.activeAuctions.remove(itemId);
                        AuctionServer.activeAutoBids.remove(itemId);
                    }
                    else {
                        this.sendMessage("ERROR;Không thể xóa sản phẩm đã kết thúc");
                    }

                    BidPublisher.getInstance().notifyObservers();
                }
                else if (request.equals("NEW_USER") || request.equals("TRANSACTION_UPDATED") || request.equals("UPDATE")){
                    BidPublisher.getInstance().notifyObservers();
                }
                else if (request.equals("LOGOUT")){
                    AuctionServer.newClients.remove(userId);
                }
            }
        } catch (IOException e) {
            System.out.println("Client mất kết nối");
        }
        finally {
            try {
                BidPublisher.getInstance().removeObserver(this);
                AuctionServer.removeClient(this);
                AuctionServer.newClients.remove(userId);
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Luồng hủy Auto-Bid và đồng bộ gửi tín hiệu hạ trạng thái nút về client chỉ định
    private void deactivateAutoBidInternal(int itemId, int targetUserId) {
        if (AuctionServer.activeAutoBids.containsKey(itemId)) {
            AuctionServer.activeAutoBids.get(itemId).remove(targetUserId);
        }
        bidDAO.deactivateAutoBid(itemId, targetUserId);

        // SỬA TẠI ĐÂY: Duyệt danh sách Client đang Online trên Server, tìm đúng ông bị hủy để gửi trực tiếp vào socket ông đó
        for (ClientHandler client : AuctionServer.clients) {
            // Giả sử trong ClientHandler của ông có lưu biến userId hoặc có hàm getUserId()
            // Nếu ClientHandler của ông chưa có biến global 'userId' lưu lúc login, hãy dùng AuctionServer.broadcastToUser như cũ nhưng phải check lại hàm đó bên file AuctionServer nhé!
            // Đoạn dưới đây dùng phương thức an toàn nhất:
        }
        // Để an toàn và không lo ông thiếu hàm getUserId, ta sửa lại lệnh broadcast nhắm mục tiêu chuẩn xác:
        AuctionServer.broadcastToUser(targetUserId, "AUTOBID_DISABLED;" + itemId);
    }

    // Duyệt qua map RAM tìm các đối thủ đang treo Auto-bid cấu hình thấp hơn giới hạn mới để loại bỏ trước
    private void checkAndDisableLoserAutoBids(int itemId, int currentUserId, double newLimit) {
        if (AuctionServer.activeAutoBids.containsKey(itemId)) {
            // SỬA TẠI ĐÂY: Chỉ vô hiệu hóa nếu otherUserId KHÁC với currentUserId (người vừa đặt giá)
            AuctionServer.activeAutoBids.get(itemId).forEach((otherUserId, limit) -> {
                if (otherUserId != currentUserId && limit < newLimit) {
                    deactivateAutoBidInternal(itemId, otherUserId);
                }
            });
        }
    }

    // --- ĐÃ SỬA HOÀN CHỈNH: Hàm đóng phiên Overloading 2 tham số để tương thích luồng cũ ---
    public void closeAuction(int itemId, int userId){
        closeAuction(itemId, userId, 0.0);
    }

    // --- ĐÃ THÊM MỚI: Hàm đóng phiên đầy đủ nhận giá thực tế (Thỏa mãn yêu cầu gán giá 1M khi bấm BIN) ---
    public void closeAuction(int itemId, int userId, double realWinPrice){
        boolean updateSuccess = itemDAO.closeAuction(itemId, userId, realWinPrice);
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