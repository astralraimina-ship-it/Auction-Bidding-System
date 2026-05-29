package com.auction.server;

import com.auction.database.ItemDAO;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionServer {
    private static final int PORT = 12345;
    // Dùng Set bình thường vì chúng ta sẽ tự synchronized bằng tay khi duyệt vòng lặp
    private static final Set<ClientHandler> clients = new HashSet<>();

    // ====================================================================
    // THÊM MỚI: CẤU TRÚC LƯU TRỮ VÀ BỘ NHỚ ĐỆM CHO TÍNH NĂNG AUTO-BID
    // ====================================================================

    /**
     * Lớp đối tượng đại diện cho một cấu hình Đấu giá tự động
     */
    public static class AutoBidConfig {
        public int itemId;
        public int userId;
        public double autoStep;
        public double stopPrice;

        public AutoBidConfig(int itemId, int userId, double autoStep, double stopPrice) {
            this.itemId = itemId;
            this.userId = userId;
            this.autoStep = autoStep;
            this.stopPrice = stopPrice;
        }
    }

    /**
     * Trạm quản lý dữ liệu Auto-Bid tập trung toàn hệ thống (Thread-safe)
     * Key: itemId (Mã sản phẩm) -> Value: Cấu hình Auto-Bid được kích hoạt gần nhất
     */
    public static final ConcurrentHashMap<Integer, AuctionState> activeAuctions = new ConcurrentHashMap<>();
    public static final Map<Integer, Map<Integer, Double>> activeAutoBids = new ConcurrentHashMap<>();

    // ====================================================================

    public static void main(String[] args){
        try (java.sql.Connection conn = com.auction.database.DBContext.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id, current_price, step, startPrice, binPrice FROM items WHERE status = 'OPEN'");
             java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                double currentPrice = rs.getDouble("current_price"); // Lấy giá hiện hành
                double step = rs.getDouble("step");
                double startPrice = rs.getDouble("startPrice");
                if (currentPrice == 0){
                    currentPrice = startPrice;
                }
                double binPrice = rs.getDouble("binPrice");

                // Khởi tạo Trọng tài cho từng món hàng đang bán
                AuctionState state = new AuctionState(id, currentPrice, step, binPrice);
                activeAuctions.put(id, state);
            }
            System.out.println(">>> [SERVER] Đã nạp " + activeAuctions.size() + " phòng đấu giá lên RAM.");

            // 🔥 THÊM CHÍ MẠNG: Hồi sinh dữ liệu Auto-Bid từ DB đút lên RAM ngay khi khởi động xong các phòng
            loadActiveAutoBidsFromDB();

        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            ItemDAO itemDAO = new ItemDAO();
            System.out.println(">>> [Hệ thống] Luồng tự động đóng phiên & quét phạt 24h đã bắt đầu chạy...");

            // Tạo biến đếm để giảm tần suất quét phạt (đóng phiên cần nhanh 10s/lần, phạt bùng hàng chỉ cần 5 phút/lần là đủ)
            int checkViolationCounter = 0;

            while (true) {
                try {
                    // 1. Tự động kiểm tra đóng các phiên hết hạn (Chạy liên tục mỗi 1 giây)
                    boolean hasExpired = itemDAO.checkAndCloseExpiredItems();
                    if (hasExpired) {
                        AuctionServer.broadcast("REFRESH");
                    }

                    // 2. Tự động kiểm tra phạt bùng hàng 24h (Cứ mỗi 30 chu kỳ = 300 giây = 5 phút quét 1 lần)
                    checkViolationCounter++;
                    if (checkViolationCounter >= 300) {
                        System.out.println(">>> [Hệ thống] Đang tự động kiểm tra các đơn hàng quá hạn thanh toán (24h)...");
                        ItemDAO.processExpiredPayments();
                        checkViolationCounter = 0; // Reset bộ đếm
                    }

                    Thread.sleep(1000); // 10 giây quét 1 lần
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
        ItemDAO itemDAO = new ItemDAO();
        itemDAO.createAuctionState();
        System.out.println(">>> [SERVER] Đã nạp danh sách phòng đấu giá từ MySQL lên RAM thành công!");
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

    /**
     * 🔥 THÊM MỚI TƯƠNG ỨNG: Gửi tin nhắn định danh đến một User cụ thể.
     * Gói tin được đính kèm userId đích ở cuối để phía Client nhận diện và lọc UI tự động.
     */
    public static void broadcastToUser(int userId, String msg){
        broadcast(msg + ";" + userId);
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

    /**
     * Hàm nạp cấu hình Auto-Bid đang hoạt động từ DB lên RAM khi khởi động Server
     */
    public static void loadActiveAutoBidsFromDB() {
        String sql = "SELECT item_id, user_id, max_budget FROM autobids WHERE is_active = TRUE";
        try (java.sql.Connection conn = com.auction.database.DBContext.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                int userId = rs.getInt("user_id");
                double maxBudget = rs.getDouble("max_budget");

                // 1. Nạp ngược lại vào Map RAM quản lý Auto-Bid của Server
                activeAutoBids.computeIfAbsent(itemId, k -> new java.util.concurrent.ConcurrentHashMap<>())
                        .put(userId, maxBudget);

                // 2. Đồng bộ trạng thái Bot vào đối tượng quản lý phòng đấu giá tương ứng (nếu phòng đó đang mở)
                AuctionState state = activeAuctions.get(itemId);
                if (state != null) {
                    // Nếu mức budget này cao hơn giá hiện tại, nạp trạng thái Auto cho phòng đấu giá
                    if (maxBudget > state.getCurrentPrice()) {
                        state.setAutoTopBidder(userId, state.getCurrentPrice(), maxBudget);
                    }
                }
                count++;
            }
            System.out.println(">>> [SERVER STARTUP] Đã hồi sinh thành công " + count + " cấu hình Auto-Bid từ Database lên RAM!");
        } catch (Exception e) {
            System.out.println(">>> [LỖI] Không thể nạp cấu hình Auto-Bid từ DB khi khởi động Server!");
            e.printStackTrace();
        }
    }
}