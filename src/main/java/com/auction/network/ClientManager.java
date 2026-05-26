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

    public interface UpdateListener {
        void onUpdateReceived(String signal);
    }

    private final java.util.List<UpdateListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ClientManager() {
        connect();
    }

    public static synchronized ClientManager getInstance() { // Thêm synchronized cho an toàn Thread-safe Singleton
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    private void connect() {
        try {
            // Đã sửa thông báo lỗi cho đúng với IP cấu hình thực tế
            String host = "26.196.202.201";
            socket = new Socket(host, AuctionServer.getPort());
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Kết nối đến Server thành công!");
        } catch (IOException e) {
            System.err.println("Lỗi: Không thể kết nối đến server tại 26.196.202.201:" + AuctionServer.getPort());
        }
    }

    public void addUpdateListener(UpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // Thêm hàm xóa khi tắt màn hình để tránh rác bộ nhớ
    public void removeUpdateListener(UpdateListener listener) {
        listeners.remove(listener);
    }

    public void startListening() {
        if (isRunning) return;

        isRunning = true;
        Thread listenerThread = new Thread(() -> {
            try {
                String serverResponse;
                while (isRunning && (serverResponse = in.readLine()) != null) {
                    handleSignal(serverResponse);
                }
            } catch (IOException e) {
                System.err.println("Mất kết nối với Server hoặc Socket đã đóng.");
            } finally {
                closeConnection();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleSignal(String signal) {
        Platform.runLater(() -> {
            for (UpdateListener listener : listeners) {
                listener.onUpdateReceived(signal);
            }
        });
    }

    /**
     * Gửi yêu cầu lên server.
     * Dùng synchronized trên đối tượng 'out' để tránh việc nhiều thread ghi đè luồng của nhau.
     */
    public void sendCommand(String msg) {
        if (out != null) {
            synchronized (out) {
                out.println(msg);
            }
        } else {
            System.err.println("Chưa kết nối server, không thể gửi: " + msg);
        }
    }

    /**
     * Đóng kết nối theo đúng thứ tự từ trong ra ngoài
     */
    public void closeConnection() {
        try {
            isRunning = false;
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Đã đóng kết nối Client an toàn.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}