package com.auction.client.service;

import com.auction.client.realtime.AuctionEventBus;
import com.auction.client.realtime.AuctionObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionEventBusTest {

    private AuctionEventBus bus;

    @BeforeEach
    public void setUp() throws Exception {
        Field instanceField = AuctionEventBus.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        bus = AuctionEventBus.getInstance();
        Field mapField = AuctionEventBus.class.getDeclaredField("perAuctionObservers");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(bus)).clear();

        Field globalField = AuctionEventBus.class.getDeclaredField("globalObservers");
        globalField.setAccessible(true);
        ((CopyOnWriteArrayList<?>) globalField.get(bus)).clear();
    }

    @Test
    public void testSubscribeIncreasesObserverCount() {
        long auctionId = 1L;
        AuctionObserver observer = event -> {};

        bus.subscribe(auctionId, observer);

        assertEquals(1, bus.observerCount(auctionId));
    }

    @Test
    public void testUnsubscribeDecreasesObserverCount() {
        long auctionId = 2L;
        AuctionObserver observer = event -> {};

        bus.subscribe(auctionId, observer);
        bus.unsubscribe(auctionId, observer);

        assertEquals(0, bus.observerCount(auctionId));
    }

    @Test
    public void testSubscribeMultipleObservers() {
        long auctionId = 3L;
        AuctionObserver observer1 = event -> {};
        AuctionObserver observer2 = event -> {};

        bus.subscribe(auctionId, observer1);
        bus.subscribe(auctionId, observer2);

        assertEquals(2, bus.observerCount(auctionId));
    }

    @Test
    public void testUnsubscribeAllClearsObservers() {
        long auctionId = 4L;

        bus.subscribe(auctionId, event -> {});
        bus.subscribe(auctionId, event -> {});
        bus.subscribe(auctionId, event -> {});

        assertEquals(3, bus.observerCount(auctionId));

        bus.unsubscribeAll(auctionId);

        assertEquals(0, bus.observerCount(auctionId));
    }

    @Test
    public void testGlobalObserverDoesNotAffectPerAuctionCount() {
        long auctionId = 5L;
        AuctionObserver globalObserver = event -> {};
        bus.subscribeGlobal(globalObserver);

        assertEquals(0, bus.observerCount(auctionId));
    }
}
