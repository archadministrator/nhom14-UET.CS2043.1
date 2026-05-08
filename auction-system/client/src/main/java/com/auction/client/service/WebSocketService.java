package com.auction.client.service;

import com.auction.client.model.ClientDto.BidUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WebSocketService {

    private static WebSocketService instance;

    public static WebSocketService getInstance() {
        if (instance == null) instance = new WebSocketService();
        return instance;
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<Long, Consumer<BidUpdateMessage>> auctionListeners = new ConcurrentHashMap<>();
    private Consumer<BidUpdateMessage> globalListener;
    private WebSocketClient wsClient;

    private WebSocketService() {}

    public void connect() {
        if (wsClient != null && wsClient.isOpen()) return;
        try {
            wsClient = new WebSocketClient(URI.create("ws://localhost:8080/ws/websocket")) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("[WS] Kết nối thành công");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        BidUpdateMessage msg = mapper.readValue(message, BidUpdateMessage.class);
                        dispatch(msg);
                    } catch (Exception e) {
                        System.err.println("[WS] Parse error: " + e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[WS] Đóng kết nối: " + reason);
                    reconnectLater();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[WS] Lỗi: " + ex.getMessage());
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            System.err.println("[WS] Không thể kết nối: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    public void subscribeToAuction(Long auctionId, Consumer<BidUpdateMessage> listener) {
        auctionListeners.put(auctionId, listener);
    }

    public void unsubscribeFromAuction(Long auctionId) {
        auctionListeners.remove(auctionId);
    }

    public void setGlobalListener(Consumer<BidUpdateMessage> listener) {
        this.globalListener = listener;
    }

    private void dispatch(BidUpdateMessage msg) {
        Consumer<BidUpdateMessage> listener = auctionListeners.get(msg.getAuctionId());
        if (listener != null) listener.accept(msg);
        if (globalListener != null) globalListener.accept(msg);
    }

    private void reconnectLater() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3000);
                connect();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(true);
        t.start();
    }
}