package com.auction.network;

import com.auction.server.AuctionServer;
import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientManager {
    private static ClientManager instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isRunning = false;

    // Interface để các Controller đăng ký nhận thông báo cập nhật
    public interface UpdateListener {
        void onUpdateReceived(String signal);
    }

    private UpdateListener listener;

    // Singleton Pattern
    private ClientManager() {
        connect();
    }

    public static ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    private void connect() {
        try {
            // Thay đổi IP/Port nếu server chạy ở máy khác hoặc port khác
            socket = new Socket("localhost", AuctionServer.getPort());
            // Auto-flush set là true để đẩy dữ liệu đi ngay khi gọi println
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Kết nối đến Server thành công!");
        } catch (IOException e) {
            System.err.println("Lỗi: Không thể kết nối đến server tại localhost:12345");
        }
    }

    /**
     * Controller sẽ gọi hàm này để đăng ký lắng nghe tín hiệu từ server
     */
    public void setUpdateListener(UpdateListener listener) {
        this.listener = listener;
    }

    /**
     * Bắt đầu luồng lắng nghe tin nhắn từ Server gửi về
     */
    public void startListening() {
        if (isRunning) return; // Đảm bảo chỉ có 1 luồng lắng nghe duy nhất

        isRunning = true;
        Thread listenerThread = new Thread(() -> {
            try {
                String serverResponse;
                while (isRunning && (serverResponse = in.readLine()) != null) {
                    handleSignal(serverResponse);
                }
            } catch (IOException e) {
                System.err.println("Mất kết nối với Server!");
                isRunning = false;
            }
        });
        listenerThread.setDaemon(true); // Tự động đóng thread khi ứng dụng tắt
        listenerThread.start();
    }

    /**
     * Xử lý tín hiệu nhận được từ Server
     */
    private void handleSignal(String signal) {
        // Luôn chạy trong Platform.runLater để an toàn cho JavaFX UI
        Platform.runLater(() -> {
            if (listener != null) {
                listener.onUpdateReceived(signal);
            }
        });
    }

    /**
     * Gửi yêu cầu/lệnh lên Server (Ví dụ: "BID;1;10;500000")
     */
    public void sendCommand(String msg) {
        System.out.println("manager" + msg);
        if (out != null) {
            new Thread(() -> {
                out.println(msg);
            }).start();
        } else {
            System.err.println("Chưa kết nối server, không thể gửi: " + msg);
        }
    }

    /**
     * Đóng kết nối khi đăng xuất hoặc thoát ứng dụng
     */
    public void closeConnection() {
        try {
            isRunning = false;
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}