package com.auction.client.service;

import com.auction.client.realtime.AuctionEventBus;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class WebSocketServiceTest {

    private WebSocketService service;
    private TestWebSocketClient mockWsClient;

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

        AuctionEventBus.getInstance();
        service = WebSocketService.getInstance();

        mockWsClient = new TestWebSocketClient();
        Field wsField = WebSocketService.class.getDeclaredField("wsClient");
        wsField.setAccessible(true);
        wsField.set(service, mockWsClient);
    }

    private void setStompConnected() throws Exception {
        Field f = WebSocketService.class.getDeclaredField("stompConnected");
        f.setAccessible(true);
        ((AtomicBoolean) f.get(service)).set(true);
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
}