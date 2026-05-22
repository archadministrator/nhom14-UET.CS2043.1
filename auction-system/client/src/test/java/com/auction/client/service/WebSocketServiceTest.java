package com.auction.client.service;

import com.auction.client.model.ClientDto.BidUpdateMessage;
import com.auction.client.realtime.AuctionEvent;
import com.auction.client.realtime.AuctionEventBus;
import com.auction.client.realtime.AuctionObserver;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebSocketServiceTest {

    private WebSocketService service;
    private WebSocketClient mockWsClient;
    private AuctionEventBus bus;

    @BeforeEach
    public void setUp() throws Exception {
        Field serviceInstanceField = WebSocketService.class.getDeclaredField("instance");
        serviceInstanceField.setAccessible(true);
        serviceInstanceField.set(null, null);

        service = WebSocketService.getInstance();

        Field busInstanceField = AuctionEventBus.class.getDeclaredField("instance");
        busInstanceField.setAccessible(true);
        busInstanceField.set(null, null);

        bus = AuctionEventBus.getInstance();
        
        Field mapField = AuctionEventBus.class.getDeclaredField("perAuctionObservers");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(bus)).clear();

        Field globalField = AuctionEventBus.class.getDeclaredField("globalObservers");
        globalField.setAccessible(true);
        ((CopyOnWriteArrayList<?>) globalField.get(bus)).clear();

        mockWsClient = mock(WebSocketClient.class);
        when(mockWsClient.isOpen()).thenReturn(true);

        setField(service, "wsClient", mockWsClient);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    @Test
    public void testParserHelpersExtractCommand() throws Exception {
        String frame = "CONNECTED\nversion:1.1\n\n\u0000";
        String command = (String) invokeMethod(service, "extractCommand", new Class<?>[]{String.class}, frame);
        assertEquals("CONNECTED", command);

        String frameCrLf = "MESSAGE\r\ndestination:/topic/auctions\r\n\r\nbody\u0000";
        String commandCrLf = (String) invokeMethod(service, "extractCommand", new Class<?>[]{String.class}, frameCrLf);
        assertEquals("MESSAGE", commandCrLf);
    }

    @Test
    public void testParserHelpersExtractBody() throws Exception {
        String frame = "MESSAGE\ndestination:/topic/auctions\n\nhello world\u0000";
        String body = (String) invokeMethod(service, "extractBody", new Class<?>[]{String.class}, frame);
        assertEquals("hello world", body);

        String frameNoBody = "CONNECTED\nversion:1.1\n\n\u0000";
        String bodyNoBody = (String) invokeMethod(service, "extractBody", new Class<?>[]{String.class}, frameNoBody);
        assertEquals("", bodyNoBody);
    }

    @Test
    public void testDisconnectSendsFrameAndCloses() throws Exception {
        service.disconnect();

        AtomicBoolean intentionalClose = (AtomicBoolean) getField(service, "intentionalClose");
        assertTrue(intentionalClose.get());

        assertFalse(service.isConnected());

        ArgumentCaptor<String> frameCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWsClient).send(frameCaptor.capture());
        String sentFrame = frameCaptor.getValue();
        assertTrue(sentFrame.startsWith("DISCONNECT"));

        verify(mockWsClient).close();

        Map<?, ?> activeSubscriptions = (Map<?, ?>) getField(service, "activeSubscriptions");
        Map<?, ?> auctionSubIds = (Map<?, ?>) getField(service, "auctionSubIds");
        assertTrue(activeSubscriptions.isEmpty());
        assertTrue(auctionSubIds.isEmpty());
    }

    @Test
    public void testSubscribeToAuctionSendsStompFrame() throws Exception {
        AtomicBoolean stompConnected = (AtomicBoolean) getField(service, "stompConnected");
        stompConnected.set(true);

        long auctionId = 456L;
        service.subscribeToAuction(auctionId);

        ArgumentCaptor<String> frameCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWsClient).send(frameCaptor.capture());
        String sentFrame = frameCaptor.getValue();

        assertTrue(sentFrame.startsWith("SUBSCRIBE"));
        assertTrue(sentFrame.contains("destination:/topic/auction/" + auctionId));
        assertTrue(sentFrame.contains("id:sub-0"));

        Map<Long, String> auctionSubIds = (Map<Long, String>) getField(service, "auctionSubIds");
        Map<String, String> activeSubscriptions = (Map<String, String>) getField(service, "activeSubscriptions");

        assertEquals("sub-0", auctionSubIds.get(auctionId));
        assertEquals("/topic/auction/" + auctionId, activeSubscriptions.get("sub-0"));
    }

    @Test
    public void testUnsubscribeFromAuctionSendsStompFrame() throws Exception {
        AtomicBoolean stompConnected = (AtomicBoolean) getField(service, "stompConnected");
        stompConnected.set(true);

        Map<Long, String> auctionSubIds = (Map<Long, String>) getField(service, "auctionSubIds");
        Map<String, String> activeSubscriptions = (Map<String, String>) getField(service, "activeSubscriptions");

        long auctionId = 789L;
        auctionSubIds.put(auctionId, "sub-5");
        activeSubscriptions.put("sub-5", "/topic/auction/" + auctionId);

        service.unsubscribeFromAuction(auctionId);

        ArgumentCaptor<String> frameCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWsClient).send(frameCaptor.capture());
        String sentFrame = frameCaptor.getValue();

        assertTrue(sentFrame.startsWith("UNSUBSCRIBE"));
        assertTrue(sentFrame.contains("id:sub-5"));

        assertNull(auctionSubIds.get(auctionId));
        assertNull(activeSubscriptions.get("sub-5"));
    }

    @Test
    public void testSubscribeGlobalSendsStompFrame() throws Exception {
        AtomicBoolean stompConnected = (AtomicBoolean) getField(service, "stompConnected");
        stompConnected.set(true);

        service.subscribeGlobal();

        ArgumentCaptor<String> frameCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWsClient).send(frameCaptor.capture());
        String sentFrame = frameCaptor.getValue();

        assertTrue(sentFrame.startsWith("SUBSCRIBE"));
        assertTrue(sentFrame.contains("destination:/topic/auctions"));
        assertTrue(sentFrame.contains("id:sub-0"));
    }

    @Test
    public void testOnStompConnectedResubscribesTrackedAuctions() throws Exception {
        Map<Long, String> auctionSubIds = (Map<Long, String>) getField(service, "auctionSubIds");
        auctionSubIds.put(101L, "pending");

        String stompConnectedFrame = "CONNECTED\nversion:1.1\n\n\u0000";
        invokeMethod(service, "handleStompFrame", new Class<?>[]{String.class}, stompConnectedFrame);

        assertTrue(service.isConnected());

        ArgumentCaptor<String> frameCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockWsClient, times(2)).send(frameCaptor.capture());

        java.util.List<String> sentFrames = frameCaptor.getAllValues();
        boolean foundGlobal = false;
        boolean foundAuction = false;

        for (String frame : sentFrames) {
            if (frame.contains("destination:/topic/auctions")) {
                foundGlobal = true;
            }
            if (frame.contains("destination:/topic/auction/101")) {
                foundAuction = true;
            }
        }

        assertTrue(foundGlobal, "Global topic subscription was not sent");
        assertTrue(foundAuction, "Auction topic subscription was not sent");
    }

    @Test
    public void testOnStompMessagePublishesEventToEventBus() throws Exception {
        long auctionId = 999L;
        AuctionObserver mockObserver = mock(AuctionObserver.class);
        bus.subscribe(auctionId, mockObserver);

        String rawJson = "{"
                + "\"type\":\"NEW_BID\","
                + "\"auctionId\":" + auctionId + ","
                + "\"currentPrice\":250.50,"
                + "\"leaderUsername\":\"bidder123\","
                + "\"totalBids\":14,"
                + "\"endTime\":\"2026-05-22T15:00:00\""
                + "}";

        String rawStompMessageFrame = "MESSAGE\n"
                + "destination:/topic/auction/" + auctionId + "\n"
                + "subscription:sub-0\n"
                + "message-id:msg-111\n"
                + "content-type:application/json\n"
                + "\n"
                + rawJson + "\u0000";

        invokeMethod(service, "handleStompFrame", new Class<?>[]{String.class}, rawStompMessageFrame);

        ArgumentCaptor<AuctionEvent> eventCaptor = ArgumentCaptor.forClass(AuctionEvent.class);
        verify(mockObserver).onUpdate(eventCaptor.capture());

        AuctionEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(AuctionEvent.Type.NEW_BID, event.getType());
        assertEquals(auctionId, event.getAuctionId());
        assertEquals(new BigDecimal("250.50"), event.getCurrentPrice());
        assertEquals("bidder123", event.getLeaderUsername());
        assertEquals(14L, event.getTotalBids());
        assertEquals(LocalDateTime.parse("2026-05-22T15:00:00"), event.getEndTime());
    }
}
