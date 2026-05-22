package com.auction.client.service;
import com.auction.client.realtime.AuctionEvent;
import com.auction.client.realtime.AuctionEventBus;
import com.auction.client.realtime.AuctionObserver;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class WebSocketServiceTest {

    private WebSocketService service;
    private TestWebSocketClient mockWsClient;
    private AuctionEventBus bus;

    private static class TestWebSocketClient extends WebSocketClient {
        boolean closed = false;
        final java.util.List<String> sentFrames = new java.util.ArrayList<>();

        public TestWebSocketClient() {
            super(java.net.URI.create("ws://localhost"));
        }

        @Override public void send(String text) { sentFrames.add(text); }
        @Override public boolean isOpen() { return !closed; }
        @Override public void close() { this.closed = true; }
        @Override public void onOpen(org.java_websocket.handshake.ServerHandshake h) {}
        @Override public void onMessage(String message) {}
        @Override public void onClose(int code, String reason, boolean remote) {}
        @Override public void onError(Exception ex) {}
    }

    @BeforeEach
    public void setUp() throws Exception {
        Field busField = AuctionEventBus.class.getDeclaredField("instance");
        busField.setAccessible(true);
        busField.set(null, null);

        Field serviceField = WebSocketService.class.getDeclaredField("instance");
        serviceField.setAccessible(true);
        serviceField.set(null, null);

        bus = AuctionEventBus.getInstance();
        service = WebSocketService.getInstance();

        Field mapField = AuctionEventBus.class.getDeclaredField("perAuctionObservers");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(bus)).clear();

        Field globalField = AuctionEventBus.class.getDeclaredField("globalObservers");
        globalField.setAccessible(true);
        ((CopyOnWriteArrayList<?>) globalField.get(bus)).clear();

        mockWsClient = new TestWebSocketClient();
        Field wsField = service.getClass().getDeclaredField("wsClient");
        wsField.setAccessible(true);
        wsField.set(service, mockWsClient);
    }

    private void setStompConnected() throws Exception {
        Field f = service.getClass().getDeclaredField("stompConnected");
        f.setAccessible(true);
        ((AtomicBoolean) f.get(service)).set(true);
    }

    private Object invokeMethod(String name, Object arg) throws Exception {
        Method m = service.getClass().getDeclaredMethod(name, String.class);
        m.setAccessible(true);
        return m.invoke(service, arg);
    }

    @Test
    public void testSubscribeToAuctionSendsStompFrame() throws Exception {
        setStompConnected();
        service.subscribeToAuction(456L);

        assertEquals(1, mockWsClient.sentFrames.size());
        String frame = mockWsClient.sentFrames.get(0);
        assertTrue(frame.startsWith("SUBSCRIBE"));
        assertTrue(frame.contains("destination:/topic/auction/456"));
        assertTrue(frame.contains("id:sub-0"));
    }

    @Test
    public void testDisconnectSendsFrameAndCloses() throws Exception {
        service.disconnect();

        assertFalse(service.isConnected());
        assertTrue(mockWsClient.closed);
        assertEquals(1, mockWsClient.sentFrames.size());
        assertTrue(mockWsClient.sentFrames.get(0).startsWith("DISCONNECT"));
    }

    @Test
    public void testOnStompMessagePublishesEventToEventBus() throws Exception {
        AuctionObserver mockObserver = mock(AuctionObserver.class);
        bus.subscribe(999L, mockObserver);

        String frame = "MESSAGE\n"
                + "destination:/topic/auction/999\n"
                + "\n"
                + "{\"type\":\"NEW_BID\",\"auctionId\":999,"
                + "\"currentPrice\":250.50,\"leaderUsername\":\"bidder123\","
                + "\"totalBids\":14,\"endTime\":\"2026-05-22T15:00:00\"}\u0000";

        invokeMethod("handleStompFrame", frame);

        ArgumentCaptor<AuctionEvent> captor = ArgumentCaptor.forClass(AuctionEvent.class);
        verify(mockObserver).onEvent(captor.capture());

        AuctionEvent event = captor.getValue();
        assertEquals(AuctionEvent.Type.NEW_BID, event.getType());
        assertEquals(999L, event.getAuctionId());
        assertEquals(new BigDecimal("250.50"), event.getCurrentPrice());
        assertEquals("bidder123", event.getLeaderUsername());
        assertEquals(14L, event.getTotalBids());
        assertEquals(LocalDateTime.parse("2026-05-22T15:00:00"), event.getEndTime());
    }
}