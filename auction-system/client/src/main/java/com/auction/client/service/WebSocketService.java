package com.auction.client.service;

import com.auction.client.model.ClientDto.BidUpdateMessage;
import com.auction.client.realtime.AuctionEvent;
import com.auction.client.realtime.AuctionEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocketService — kết nối STOMP over raw WebSocket tới Spring server.
 *
 * Luồng STOMP đúng chuẩn:
 *   1. WebSocket connect → ws://host/ws/websocket
 *   2. Gửi STOMP CONNECT frame
 *   3. Nhận CONNECTED → gửi SUBSCRIBE cho từng topic đang chờ
 *   4. Nhận MESSAGE frame → parse JSON body → dispatch tới listeners
 *   5. Khi unsubscribe → gửi STOMP UNSUBSCRIBE
 *   6. Khi disconnect → gửi STOMP DISCONNECT rồi đóng WebSocket
 */
public class WebSocketService {

    private static final String WS_URL   = "ws://localhost:8080/ws/websocket";
    private static final String GLOBAL_TOPIC  = "/topic/auctions";
    private static final String AUCTION_TOPIC = "/topic/auction/";

    // STOMP frame delimiters
    private static final char   NULL  = '\u0000';
    private static final String CRLF  = "\n";

    private static WebSocketService instance;
    public static WebSocketService getInstance() {
        if (instance == null) instance = new WebSocketService();
        return instance;
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // subscriptionId → destination (để UNSUBSCRIBE đúng)
    private final Map<String, String> activeSubscriptions = new ConcurrentHashMap<>();
    // auctionId → subscriptionId
    private final Map<Long, String> auctionSubIds = new ConcurrentHashMap<>();

    private final AuctionEventBus eventBus = AuctionEventBus.getInstance();

    private WebSocketClient wsClient;

    private final AtomicBoolean stompConnected = new AtomicBoolean(false);
    private final AtomicBoolean intentionalClose = new AtomicBoolean(false);
    private final AtomicInteger subCounter = new AtomicInteger(0);

    private WebSocketService() {}

    // ─────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────

    public void connect() {
        if (wsClient != null && wsClient.isOpen()) return;
        intentionalClose.set(false);
        stompConnected.set(false);
        try {
            wsClient = new WebSocketClient(URI.create(WS_URL)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("[STOMP] WebSocket opened → sending CONNECT");
                    sendRaw(stompConnect());
                }

                @Override
                public void onMessage(String raw) {
                    handleStompFrame(raw);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    stompConnected.set(false);
                    System.out.println("[STOMP] Closed: " + reason + " (remote=" + remote + ")");
                    if (!intentionalClose.get()) {
                        reconnectLater();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[STOMP] Error: " + ex.getMessage());
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            System.err.println("[STOMP] Cannot connect: " + e.getMessage());
        }
    }

    public void disconnect() {
        intentionalClose.set(true);
        if (wsClient != null && wsClient.isOpen()) {
            sendRaw("DISCONNECT" + CRLF + CRLF + NULL);
            wsClient.close();
        }
        stompConnected.set(false);
        activeSubscriptions.clear();
        auctionSubIds.clear();
    }

    /** Subscribe STOMP topic cho một phiên — EventBus nhận notify */
    public void subscribeToAuction(Long auctionId) {
        if (stompConnected.get()) {
            doSubscribe(AUCTION_TOPIC + auctionId, auctionId);
        }
        // Nếu chưa connected: sẽ subscribe sau khi nhận CONNECTED (xem resubscribeAll)
        // Track auctionId để resubscribe sau reconnect
        auctionSubIds.putIfAbsent(auctionId, "pending");
    }

    /** Hủy STOMP subscription và xóa khỏi tracking */
    public void unsubscribeFromAuction(Long auctionId) {
        String subId = auctionSubIds.remove(auctionId);
        if (subId != null && !"pending".equals(subId)) {
            activeSubscriptions.remove(subId);
            if (stompConnected.get()) {
                sendRaw(stompUnsubscribe(subId));
            }
        }
    }

    /** Subscribe STOMP topic toàn cục /topic/auctions */
    public void subscribeGlobal() {
        if (stompConnected.get()) doSubscribeGlobal();
        // Sẽ subscribe sau CONNECTED nếu chưa kết nối
    }

    public boolean isConnected() {
        return stompConnected.get();
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP Frame Handling
    // ─────────────────────────────────────────────────────────────────

    private void handleStompFrame(String raw) {
        if (raw.isBlank() || raw.equals("\n") || raw.equals("\r\n")) return; // heartbeat

        String command = extractCommand(raw);
        switch (command) {
            case "CONNECTED" -> onStompConnected();
            case "MESSAGE"   -> onStompMessage(raw);
            case "ERROR"     -> System.err.println("[STOMP] SERVER ERROR:\n" + raw);
            default          -> System.out.println("[STOMP] Unhandled frame: " + command);
        }
    }

    private void onStompConnected() {
        stompConnected.set(true);
        System.out.println("[STOMP] Connected — resubscribing " + auctionSubIds.size() + " topic(s)");
        resubscribeAll();
    }

    private void onStompMessage(String raw) {
        String body = extractBody(raw);
        if (body == null || body.isBlank()) return;

        try {
            BidUpdateMessage msg = mapper.readValue(body, BidUpdateMessage.class);
            // Publish lên EventBus — dispatch đến tất cả observers đã đăng ký
            eventBus.publish(AuctionEvent.from(msg));
        } catch (Exception e) {
            System.err.println("[STOMP] Parse error: " + e.getMessage() + "\nBody: " + body);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP Frame Builders
    // ─────────────────────────────────────────────────────────────────

    private String stompConnect() {
        return "CONNECT" + CRLF +
               "accept-version:1.1,1.0" + CRLF +
               "heart-beat:0,0" + CRLF +
               CRLF + NULL;
    }

    private String stompSubscribe(String destination, String subId) {
        return "SUBSCRIBE" + CRLF +
               "id:" + subId + CRLF +
               "destination:" + destination + CRLF +
               CRLF + NULL;
    }

    private String stompUnsubscribe(String subId) {
        return "UNSUBSCRIBE" + CRLF +
               "id:" + subId + CRLF +
               CRLF + NULL;
    }

    // ─────────────────────────────────────────────────────────────────
    // Subscribe helpers
    // ─────────────────────────────────────────────────────────────────

    private void doSubscribe(String destination, Long auctionId) {
        if (activeSubscriptions.containsValue(destination)) return; // already subscribed
        String subId = "sub-" + subCounter.getAndIncrement();
        activeSubscriptions.put(subId, destination);
        if (auctionId != null) auctionSubIds.put(auctionId, subId);
        sendRaw(stompSubscribe(destination, subId));
        System.out.println("[STOMP] Subscribed → " + destination + " (" + subId + ")");
    }

    private void doSubscribeGlobal() {
        if (!activeSubscriptions.containsValue(GLOBAL_TOPIC)) {
            doSubscribe(GLOBAL_TOPIC, null);
        }
    }

    /** Sau khi (re)connect: đăng ký lại tất cả topic đang track */
    private void resubscribeAll() {
        // Lấy danh sách auctionId đang track trước khi reset
        java.util.Set<Long> trackedAuctions = new java.util.HashSet<>(auctionSubIds.keySet());

        activeSubscriptions.clear();
        auctionSubIds.clear();
        subCounter.set(0);

        // Global topic
        doSubscribeGlobal();

        // Từng phiên đấu giá đang được theo dõi
        for (Long auctionId : trackedAuctions) {
            doSubscribe(AUCTION_TOPIC + auctionId, auctionId);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP Frame Parser helpers
    // ─────────────────────────────────────────────────────────────────

    private String extractCommand(String frame) {
        int idx = frame.indexOf('\n');
        return idx < 0 ? frame.trim() : frame.substring(0, idx).trim();
    }

    private String extractHeader(String frame, String name) {
        for (String line : frame.split("\n")) {
            if (line.startsWith(name + ":")) {
                return line.substring(name.length() + 1).trim();
            }
        }
        return null;
    }

    private String extractBody(String frame) {
        int bodyStart = frame.indexOf("\n\n");
        if (bodyStart < 0) return null;
        String body = frame.substring(bodyStart + 2);
        // Remove trailing null byte
        int nullIdx = body.indexOf(NULL);
        return nullIdx >= 0 ? body.substring(0, nullIdx) : body;
    }

    // ─────────────────────────────────────────────────────────────────
    // Reconnect
    // ─────────────────────────────────────────────────────────────────

    private void sendRaw(String frame) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(frame);
        }
    }

    private void reconnectLater() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("[STOMP] Reconnecting...");
                connect();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(true);
        t.start();
    }
}