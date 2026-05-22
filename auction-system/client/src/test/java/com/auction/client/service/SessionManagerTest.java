package com.auction.client.service;

import com.auction.client.model.ClientDto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    private SessionManager session;

    @BeforeEach
    public void setUp() throws Exception {
        Field instanceField = SessionManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        session = SessionManager.getInstance();
    }

    @Test
    public void testInitialStateIsNotLoggedIn() {
        assertFalse(session.isLoggedIn());
        assertNull(session.getToken());
        assertNull(session.getUsername());
    }

    @Test
    public void testLoginStoresUserInfo() {
        AuthResponse auth = new AuthResponse();
        auth.setToken("test-token-abc");
        auth.setUsername("nguyenvana");
        auth.setRole("BIDDER");
        auth.setBalance(new BigDecimal("1500000"));

        session.login(auth);

        assertEquals("test-token-abc", session.getToken());
        assertEquals("nguyenvana",     session.getUsername());
        assertEquals("BIDDER",         session.getRole());
        assertEquals(1_500_000.0,      session.getBalance(), 0.001);
    }

    @Test
    public void testIsLoggedInAfterLogin() {
        AuthResponse auth = new AuthResponse();
        auth.setToken("some-valid-token");
        auth.setUsername("testuser");
        auth.setRole("SELLER");
        auth.setBalance(BigDecimal.ZERO);

        session.login(auth);

        assertTrue(session.isLoggedIn());
    }

    @Test
    public void testRoleCheckForBidder() {
        AuthResponse auth = new AuthResponse();
        auth.setToken("bidder-token");
        auth.setUsername("nguoimuahang");
        auth.setRole("BIDDER");
        auth.setBalance(BigDecimal.ZERO);

        session.login(auth);

        assertTrue(session.isBidder());
        assertFalse(session.isSeller());
        assertFalse(session.isAdmin());
    }

    @Test
    public void testLogoutClearsSession() {
        AuthResponse auth = new AuthResponse();
        auth.setToken("logout-test-token");
        auth.setUsername("testuser");
        auth.setRole("ADMIN");
        auth.setBalance(new BigDecimal("999999"));
        session.login(auth);

        session.logout();

        assertFalse(session.isLoggedIn());
        assertNull(session.getToken());
        assertNull(session.getUsername());
        assertEquals(0.0, session.getBalance(), 0.001);
    }
}
