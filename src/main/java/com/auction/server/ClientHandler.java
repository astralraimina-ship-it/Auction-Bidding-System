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

                // 1. XỬ LÝ GÓI TIN KIỂM TRA MẠNG ẨN (PING): Im lặng bỏ qua, tránh crash cắt chuỗi dấu ;
                if ("PING".equals(request)) {
                    continue;
                }

                System.out.println(request);

                // 2. XỬ LÝ ĐẶT GIÁ THỦ CÔNG (BID)
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

                        // ====================================================================
                        // KÍCH HOẠT HỆ THỐNG AUTO-BID: Có người đặt giá mới -> Đánh thức Auto trả đòn!
                        processAutoBidSystem(itemId);
                        // ====================================================================
                    }
                    else{
                        this.sendMessage("Error;Đặt giá thất bại! Có thể phiên đấu giá đã đóng trên máy chủ.");
                    }
                }
                // 3. XỬ LÝ LỆNH CẤU HÌNH AUTO-BID TỪ CLIENT GỬI LÊN
                else if (request.startsWith("SET_AUTOBID")) {
                    String[] part = request.split(";");
                    int itemId = Integer.parseInt(part[1]);
                    int userId = Integer.parseInt(part[2]);
                    double autoStep = parseDoubleSafe(part[3]);
                    double stopPrice = parseDoubleSafe(part[4]);

                    // ĐỒNG BỘ MỚI: Dùng Key kết hợp itemId-userId để không bị ghi đè khi nhiều người bật Auto cùng phòng
                    String key = itemId + "-" + userId;
                    AuctionServer.AutoBidConfig config = new AuctionServer.AutoBidConfig(itemId, userId, autoStep, stopPrice);
                    AuctionServer.activeAutoBids.put(key, config);
                    System.out.println(">>> [SERVER] Đã ghi nhận cấu hình AutoBid cho khóa: " + key);
                    processAutoBidSystem(itemId);
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
                            BidPublisher.getInstance().notifyObservers();

                            // ĐỒNG BỘ MỚI: Xóa sạch toàn bộ cấu hình Auto ngầm của sản phẩm này vì phiên đã đóng hẳn
                            AuctionServer.activeAutoBids.keySet().removeIf(key -> key.startsWith(itemId + "-"));
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

    /**
     * THUẬT TOÁN ĐẤU GIÁ TỰ ĐỘNG (AUTO-BID SYSTEM) ĐÃ SỬA ĐỒNG BỘ ĐA NGƯỜI DÙNG
     * Tự động quét tìm đối thủ và thực hiện chuỗi đẩy giá qua lại thời gian thực.
     */
    private void processAutoBidSystem(int itemId) {
        while (true) {
            int currentHighestBidderId = -1;
            double currentPrice = 0.0;

            // 1. Truy vấn trực tiếp từ DB lượt đặt giá cao nhất hiện tại để đảm bảo tính chính xác tuyệt đối
            String sqlBid = "SELECT user_id, bid_amount FROM bids WHERE item_id = ? ORDER BY bid_amount DESC, bid_time DESC LIMIT 1";
            try (java.sql.Connection conn = com.auction.database.DBContext.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sqlBid)) {
                ps.setInt(1, itemId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentHighestBidderId = rs.getInt("user_id");
                        currentPrice = rs.getDouble("bid_amount");
                    } else {
                        // Nếu chưa có lượt đặt nào, lấy giá khởi điểm (start_price) làm gốc
                        String sqlItem = "SELECT startPrice FROM items WHERE id = ?";
                        try (java.sql.PreparedStatement ps2 = conn.prepareStatement(sqlItem)) {
                            ps2.setInt(1, itemId);
                            try (java.sql.ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    currentPrice = rs2.getDouble("startPrice");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            // 2. THAY ĐỔI QUAN TRỌNG: Quét bộ nhớ activeAutoBids để tìm ra NGƯỜI DÙNG BỊ ĐÈ GIÁ
            // có cài Auto-Bid cho món đồ này (userId khác với currentHighestBidderId) nhằm kích hoạt trả đòn
            AuctionServer.AutoBidConfig config = null;
            for (AuctionServer.AutoBidConfig c : AuctionServer.activeAutoBids.values()) {
                if (c.itemId == itemId && c.userId != currentHighestBidderId) {
                    config = c;
                    break; // Tìm thấy ứng viên thích hợp để tự động nạp giá đè lại mốc cao nhất
                }
            }

            // Nếu không tìm thấy ai cài Auto hoặc người cài Auto chính là người đang giữ giá cao nhất -> Kết thúc chuỗi nhảy Auto
            if (config == null) {
                break;
            }

            // 3. Lấy Bước giá gốc (Original Step) và Giá mua đứt (BIN Price) từ DB
            double originalStep = 0.0;
            double binPrice = 0.0;
            String sqlItemDetails = "SELECT step, binPrice FROM items WHERE id = ?";
            try (java.sql.Connection conn = com.auction.database.DBContext.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sqlItemDetails)) {
                ps.setInt(1, itemId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        originalStep = rs.getDouble("step");
                        binPrice = rs.getDouble("binPrice");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            // Tính toán mức giá dự kiến nhảy tiếp theo
            double nextAutoPrice = currentPrice + config.autoStep;

            // ====================================================================
            // ĐIỀU KIỆN 1: Nếu ngưỡng dừng = bin price thì chốt luôn giá mua đứt
            if (config.stopPrice == binPrice && nextAutoPrice >= binPrice) {
                boolean success = bidDAO.placeBid(itemId, config.userId, binPrice);
                if (success) {
                    itemDAO.closeAuction(itemId, config.userId);
                    AuctionServer.broadcast("Notify;Sản phẩm [ID: " + itemId + "] đã được chốt mua đứt thành công qua chế độ Auto-BIN!");
                    AuctionServer.broadcast("Closed");
                    BidPublisher.getInstance().notifyObservers();
                }
                // Xóa cấu hình của người dùng này dựa vào Key chuỗi (itemId-userId)
                AuctionServer.activeAutoBids.remove(itemId + "-" + config.userId);
                break;
            }

            // ĐIỀU KIỆN 2: Khi mức giá nhảy tiếp theo vượt quá Ngưỡng dừng (Stop Price)
            if (nextAutoPrice > config.stopPrice) {
                boolean finalBidPlaced = false;
                // Kiểm tra xem khoảng cách: Ngưỡng dừng - Giá sàn hiện tại có lớn hơn hoặc bằng Bước nhảy gốc không
                if (config.stopPrice - currentPrice >= originalStep) {
                    boolean success = bidDAO.placeBid(itemId, config.userId, config.stopPrice);
                    if (success) {
                        BidPublisher.getInstance().notifyObservers();
                        AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + config.userId + ";" + config.stopPrice);
                        finalBidPlaced = true;
                    }
                } else {
                    System.out.println(">>> [AUTO-BID] Ngừng đặt giá cho User " + config.userId + " do khoảng cách đến ngưỡng dừng nhỏ hơn bước giá sản phẩm.");
                }

                // Xóa cấu hình của người dùng này ra khỏi Map tĩnh vì đã hết tài nguyên biên
                AuctionServer.activeAutoBids.remove(itemId + "-" + config.userId);
                if (finalBidPlaced) {
                    continue;
                } else {
                    break;
                }
            }

            // ĐIỀU KIỆN 3: Trường hợp thông thường (Mức giá tiếp theo vẫn nằm dưới ngưỡng dừng)
            if (nextAutoPrice <= config.stopPrice) {
                boolean success = bidDAO.placeBid(itemId, config.userId, nextAutoPrice);
                if (success) {
                    BidPublisher.getInstance().notifyObservers();
                    AuctionServer.broadcast("BID_UPDATE;" + itemId + ";" + config.userId + ";" + nextAutoPrice);
                    // LƯU Ý: Không dùng lệnh `break;` ở đây để vòng lặp `while(true)` quay lại kiểm tra tiếp,
                    // giúp kích hoạt Auto trả đòn của người khác nếu họ cũng bật Auto đấu đá lẫn nhau!
                } else {
                    break;
                }
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