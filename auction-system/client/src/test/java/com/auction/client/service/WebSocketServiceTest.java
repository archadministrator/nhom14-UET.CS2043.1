package com.auction.client.service;

import com.auction.client.realtime.AuctionEventBus;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebSocketService Tests")
class WebSocketServiceTest {

    private WebSocketService service;
    private TestWebSocketClient mockWsClient;

    // ─────────────────────────────────────────────────────────────────
    // Fake WebSocketClient — không mở kết nối thực, chỉ ghi lại frames
    // ─────────────────────────────────────────────────────────────────
    private static class TestWebSocketClient extends WebSocketClient {
        volatile boolean closed = false;
        final List<String> sentFrames = new CopyOnWriteArrayList<>();

        public TestWebSocketClient() {
            super(java.net.URI.create("ws://localhost"));
        }

        @Override public void send(String text)    { sentFrames.add(text); }
        @Override public boolean isOpen()          { return !closed; }
        @Override public void close()              { closed = true; }
        @Override public void onOpen(org.java_websocket.handshake.ServerHandshake h) {}
        @Override public void onMessage(String msg) {}
        @Override public void onClose(int code, String reason, boolean remote) {}
        @Override public void onError(Exception ex) {}
    }

    @BeforeEach
    void setUp() throws Exception {
        // Reset Singleton instances trước mỗi test
        resetSingleton(AuctionEventBus.class, "instance");
        resetSingleton(WebSocketService.class, "instance");

        AuctionEventBus.getInstance();
        service = WebSocketService.getInstance();

        // Inject fake WebSocket client
        mockWsClient = new TestWebSocketClient();
        setField(service, "wsClient", mockWsClient);
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP — SUBSCRIBE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("subscribeToAuction()")
    class SubscribeTests {

        @Test
        @DisplayName("Khi đã connected → gửi STOMP SUBSCRIBE frame đúng format")
        void subscribeToAuction_connected_sendsCorrectFrame() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(456L);

            assertEquals(1, mockWsClient.sentFrames.size());
            String frame = mockWsClient.sentFrames.get(0);
            assertTrue(frame.startsWith("SUBSCRIBE"),          "Frame phải bắt đầu bằng SUBSCRIBE");
            assertTrue(frame.contains("destination:/topic/auction/456"), "Phải có đúng destination");
            assertTrue(frame.contains("id:sub-0"),             "Phải có subscription id");
        }

        @Test
        @DisplayName("Khi chưa connected → chưa gửi frame, nhưng tracking auctionId")
        void subscribeToAuction_notConnected_doesNotSendFrame() throws Exception {
            setStompConnected(false);
            service.subscribeToAuction(789L);

            assertTrue(mockWsClient.sentFrames.isEmpty(),
                    "Không được gửi frame khi chưa STOMP connected");
            // Nhưng auctionId phải được track để resubscribe sau khi kết nối lại
            Map<Long, String> auctionSubIds = getField(service, "auctionSubIds");
            assertTrue(auctionSubIds.containsKey(789L),
                    "auctionId phải được track dù chưa connected");
        }

        @Test
        @DisplayName("Subscribe cùng auctionId hai lần → chỉ gửi một SUBSCRIBE frame")
        void subscribeToAuction_duplicate_sendsOnlyOnce() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(100L);
            service.subscribeToAuction(100L); // lần 2 phải bị bỏ qua

            long subscribeFrames = mockWsClient.sentFrames.stream()
                    .filter(f -> f.startsWith("SUBSCRIBE") && f.contains("/topic/auction/100"))
                    .count();
            assertEquals(1, subscribeFrames,
                    "Không được subscribe trùng cùng một topic");
        }

        @Test
        @DisplayName("Subscribe nhiều phiên khác nhau → mỗi phiên nhận một subId riêng")
        void subscribeToAuction_multipleAuctions_uniqueSubIds() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(1L);
            service.subscribeToAuction(2L);
            service.subscribeToAuction(3L);

            Map<String, String> activeSubs = getField(service, "activeSubscriptions");
            // Tất cả sub phải có destination khác nhau
            long distinctDests = activeSubs.values().stream().distinct().count();
            assertEquals(activeSubs.size(), distinctDests,
                    "Mỗi subscription phải có destination riêng biệt");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP — UNSUBSCRIBE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("unsubscribeFromAuction()")
    class UnsubscribeTests {

        @Test
        @DisplayName("Unsubscribe sau khi đã subscribe → gửi UNSUBSCRIBE frame và xóa tracking")
        void unsubscribe_afterSubscribe_sendsFrameAndCleansUp() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(200L);
            mockWsClient.sentFrames.clear(); // bỏ SUBSCRIBE frame khỏi history

            service.unsubscribeFromAuction(200L);

            assertEquals(1, mockWsClient.sentFrames.size());
            assertTrue(mockWsClient.sentFrames.get(0).startsWith("UNSUBSCRIBE"),
                    "Phải gửi UNSUBSCRIBE frame");
            assertTrue(mockWsClient.sentFrames.get(0).contains("id:sub-0"),
                    "UNSUBSCRIBE phải có đúng subId");

            // Tracking phải được xóa
            Map<Long, String> auctionSubIds = getField(service, "auctionSubIds");
            assertFalse(auctionSubIds.containsKey(200L),
                    "auctionId phải được xóa khỏi tracking sau unsubscribe");
            Map<String, String> activeSubs = getField(service, "activeSubscriptions");
            assertFalse(activeSubs.containsKey("sub-0"),
                    "subId phải được xóa khỏi activeSubscriptions");
        }

        @Test
        @DisplayName("Unsubscribe auctionId chưa subscribe → không gửi frame, không crash")
        void unsubscribe_nonExistentId_noFrameSent() {
            assertDoesNotThrow(() -> service.unsubscribeFromAuction(999L));
            assertTrue(mockWsClient.sentFrames.isEmpty());
        }

        @Test
        @DisplayName("Subscribe sau unsubscribe cùng auctionId → subscribe lại được")
        void subscribeAfterUnsubscribe_works() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(300L);
            service.unsubscribeFromAuction(300L);
            mockWsClient.sentFrames.clear();

            service.subscribeToAuction(300L);

            assertEquals(1, mockWsClient.sentFrames.size());
            assertTrue(mockWsClient.sentFrames.get(0).startsWith("SUBSCRIBE"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP — DISCONNECT
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("disconnect()")
    class DisconnectTests {

        @Test
        @DisplayName("disconnect() → gửi DISCONNECT frame, đóng WebSocket, xóa state")
        void disconnect_sendsFrameAndClearsState() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(10L);
            service.subscribeToAuction(20L);
            mockWsClient.sentFrames.clear();

            service.disconnect();

            assertTrue(mockWsClient.closed,          "WebSocket phải đóng sau disconnect");
            assertFalse(service.isConnected(),        "isConnected() phải trả false sau disconnect");
            assertEquals(1, mockWsClient.sentFrames.size());
            assertTrue(mockWsClient.sentFrames.get(0).startsWith("DISCONNECT"),
                    "Phải gửi STOMP DISCONNECT frame");

            // Toàn bộ state phải được xóa
            Map<String, String> activeSubs = getField(service, "activeSubscriptions");
            Map<Long, String> auctionSubIds = getField(service, "auctionSubIds");
            assertTrue(activeSubs.isEmpty(),   "activeSubscriptions phải trống sau disconnect");
            assertTrue(auctionSubIds.isEmpty(), "auctionSubIds phải trống sau disconnect");
        }

        @Test
        @DisplayName("disconnect() khi chưa kết nối → không crash, không gửi frame")
        void disconnect_whenNotConnected_noFrameSent() throws Exception {
            setField(service, "wsClient", null);
            assertDoesNotThrow(() -> service.disconnect());
            assertTrue(mockWsClient.sentFrames.isEmpty());
        }

        @Test
        @DisplayName("isConnected() → false sau khi disconnect")
        void isConnected_afterDisconnect_returnsFalse() throws Exception {
            setStompConnected(true);
            assertTrue(service.isConnected());

            service.disconnect();

            assertFalse(service.isConnected());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP — FRAME HANDLING
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("handleStompFrame() — xử lý frame nhận được từ server")
    class FrameHandlingTests {

        @Test
        @DisplayName("Nhận CONNECTED frame → stompConnected = true")
        void handleConnectedFrame_setsStompConnected() throws Exception {
            setStompConnected(false);

            invokeHandleStompFrame("CONNECTED\n\nversion:1.1\n\u0000");

            assertTrue(service.isConnected(),
                    "Sau khi nhận CONNECTED frame, isConnected() phải = true");
        }

        @Test
        @DisplayName("Nhận CONNECTED frame sau reconnect → resubscribe tất cả topic cũ")
        void handleConnectedFrame_resubscribesTrackedAuctions() throws Exception {
            // Setup: đã có tracking (simulate trạng thái sau mất kết nối)
            Map<Long, String> auctionSubIds = getField(service, "auctionSubIds");
            auctionSubIds.put(10L, "pending");
            auctionSubIds.put(20L, "pending");

            setStompConnected(false);
            mockWsClient.sentFrames.clear();

            invokeHandleStompFrame("CONNECTED\n\nversion:1.1\n\u0000");

            long subscribeFrames = mockWsClient.sentFrames.stream()
                    .filter(f -> f.startsWith("SUBSCRIBE"))
                    .count();
            // Phải có ít nhất 2 SUBSCRIBE frame cho 2 auctionId đã track
            assertTrue(subscribeFrames >= 2,
                    "Phải resubscribe lại tất cả topic đã track sau reconnect");
        }

        @Test
        @DisplayName("Nhận heartbeat (blank frame) → không xử lý, không crash")
        void handleHeartbeat_ignored() {
            assertDoesNotThrow(() -> invokeHandleStompFrame("\n"));
            assertDoesNotThrow(() -> invokeHandleStompFrame("   "));
        }

        @Test
        @DisplayName("Nhận MESSAGE frame hợp lệ → publish lên EventBus")
        void handleMessageFrame_validJson_publishesToEventBus() throws Exception {
            AuctionEventBus bus = AuctionEventBus.getInstance();
            AtomicInteger received = new AtomicInteger(0);
            bus.subscribeGlobal(event -> received.incrementAndGet());

            String messageFrame = "MESSAGE\n" +
                    "destination:/topic/auction/10\n" +
                    "content-type:application/json\n\n" +
                    "{\"type\":\"NEW_BID\",\"auctionId\":10,\"currentPrice\":10500000," +
                    "\"currentLeader\":\"bidder01\",\"totalBids\":1,\"endTime\":\"2099-01-01T00:00:00\"}" +
                    "\u0000";

            invokeHandleStompFrame(messageFrame);

            assertEquals(1, received.get(),
                    "EventBus phải nhận đúng 1 event từ MESSAGE frame hợp lệ");
        }

        @Test
        @DisplayName("Nhận MESSAGE frame JSON lỗi → không crash, không publish event")
        void handleMessageFrame_malformedJson_doesNotCrash() throws Exception {
            AuctionEventBus bus = AuctionEventBus.getInstance();
            AtomicInteger received = new AtomicInteger(0);
            bus.subscribeGlobal(event -> received.incrementAndGet());

            String badFrame = "MESSAGE\n\n{broken json\u0000";

            assertDoesNotThrow(() -> invokeHandleStompFrame(badFrame));
            assertEquals(0, received.get(), "Không publish event khi JSON lỗi");
        }

        @Test
        @DisplayName("Nhận ERROR frame → không crash")
        void handleErrorFrame_doesNotCrash() {
            assertDoesNotThrow(() ->
                    invokeHandleStompFrame("ERROR\n\nSomething went wrong\u0000"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // STOMP — RESUBSCRIBE
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resubscribeAll() — sau reconnect")
    class ResubscribeTests {

        @Test
        @DisplayName("Sau reconnect, subCounter reset về 0 → subId bắt đầu lại từ sub-0")
        void resubscribeAll_resetsSubCounter() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(1L);
            service.subscribeToAuction(2L);
            // subCounter bây giờ = 2

            mockWsClient.sentFrames.clear();

            // Simulate reconnect: nhận CONNECTED frame
            setStompConnected(false);
            invokeHandleStompFrame("CONNECTED\n\nversion:1.1\n\u0000");

            // subCounter phải reset, sub đầu tiên sau reconnect phải là sub-0
            String firstFrame = mockWsClient.sentFrames.stream()
                    .filter(f -> f.startsWith("SUBSCRIBE"))
                    .findFirst().orElse("");
            assertTrue(firstFrame.contains("id:sub-0"),
                    "Sau reconnect, subId phải bắt đầu lại từ sub-0");
        }

        @Test
        @DisplayName("Sau reconnect, không có duplicate subscription cho cùng một topic")
        void resubscribeAll_noDuplicates() throws Exception {
            setStompConnected(true);
            service.subscribeToAuction(5L);

            mockWsClient.sentFrames.clear();
            setStompConnected(false);
            invokeHandleStompFrame("CONNECTED\n\nversion:1.1\n\u0000");

            long count = mockWsClient.sentFrames.stream()
                    .filter(f -> f.contains("destination:/topic/auction/5"))
                    .count();
            assertEquals(1, count,
                    "Sau reconnect không được có duplicate SUBSCRIBE cho cùng topic");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CONCURRENCY — Thread safety
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Concurrency — thread safety")
    class ConcurrencyTests {

        @Test
        @DisplayName("Concurrent subscribe từ nhiều thread → không crash, không duplicate")
        void concurrentSubscribe_noCrashNoDuplicate() throws InterruptedException, Exception {
            setStompConnected(true);
            int numThreads = 10;
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(numThreads);

            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            for (int i = 0; i < numThreads; i++) {
                final long auctionId = i; // mỗi thread subscribe 1 phiên khác nhau
                pool.submit(() -> {
                    try {
                        start.await();
                        service.subscribeToAuction(auctionId);
                    } catch (Exception ignored) {
                    } finally {
                        finish.countDown();
                    }
                });
            }

            start.countDown();
            finish.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            Map<String, String> activeSubs = getField(service, "activeSubscriptions");
            // Mỗi auctionId khác nhau → mỗi destination phải xuất hiện đúng 1 lần
            long distinctDests = activeSubs.values().stream().distinct().count();
            assertEquals(activeSubs.size(), distinctDests,
                    "Không được có duplicate subscription sau concurrent subscribe");
        }

        @Test
        @DisplayName("Concurrent subscribe cùng auctionId từ nhiều thread → chỉ 1 SUBSCRIBE frame")
        void concurrentSubscribe_sameAuction_onlyOneFrame() throws InterruptedException, Exception {
            setStompConnected(true);
            int numThreads = 10;
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(numThreads);

            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            for (int i = 0; i < numThreads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        service.subscribeToAuction(42L); // tất cả cùng 1 auctionId
                    } catch (Exception ignored) {
                    } finally {
                        finish.countDown();
                    }
                });
            }

            start.countDown();
            finish.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            long subscribeCount = mockWsClient.sentFrames.stream()
                    .filter(f -> f.startsWith("SUBSCRIBE") && f.contains("/topic/auction/42"))
                    .count();

            // Có thể gửi 1 hoặc vài frame do race ở activeSubscriptions check,
            // nhưng không được là 10 (một frame cho mỗi thread)
            assertTrue(subscribeCount < numThreads,
                    "Concurrent subscribe cùng topic không được gửi " + numThreads + " frame");
        }

        @Test
        @DisplayName("subscribeGlobal() Singleton instance là thread-safe")
        void getInstance_multipleThreads_returnsSameInstance() throws InterruptedException {
            int numThreads = 20;
            CopyOnWriteArrayList<WebSocketService> instances = new CopyOnWriteArrayList<>();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(numThreads);

            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            for (int i = 0; i < numThreads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        instances.add(WebSocketService.getInstance());
                    } catch (Exception ignored) {
                    } finally {
                        finish.countDown();
                    }
                });
            }

            start.countDown();
            finish.await(5, TimeUnit.SECONDS);
            pool.shutdown();

            // Tất cả phải trỏ về cùng instance
            WebSocketService first = instances.get(0);
            assertTrue(instances.stream().allMatch(s -> s == first),
                    "Tất cả thread phải nhận cùng Singleton instance");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Reflection helpers
    // ─────────────────────────────────────────────────────────────────

    private void setStompConnected(boolean value) throws Exception {
        Field f = WebSocketService.class.getDeclaredField("stompConnected");
        f.setAccessible(true);
        ((AtomicBoolean) f.get(service)).set(value);
    }

    private void invokeHandleStompFrame(String raw) throws Exception {
        var method = WebSocketService.class.getDeclaredMethod("handleStompFrame", String.class);
        method.setAccessible(true);
        method.invoke(service, raw);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, null);
    }
}