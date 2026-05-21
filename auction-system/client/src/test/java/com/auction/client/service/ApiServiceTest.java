package com.auction.client.service;

import com.auction.client.model.ClientDto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiServiceTest {

    private ApiService apiService;
    private HttpClient mockHttpClient;

    @BeforeEach
    public void setUp() throws Exception {
        apiService = ApiService.getInstance();

        mockHttpClient = mock(HttpClient.class);

        Field httpClientField = ApiService.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(apiService, mockHttpClient);
    }

    @Test
    public void testLoginSuccess() throws Exception {
        String jsonResponse = "{"
                + "\"token\":\"mocked-jwt-token-12345\","
                + "\"username\":\"danghung\","
                + "\"email\":\"hung@example.com\","
                + "\"role\":\"BIDDER\","
                + "\"balance\":5000000"
                + "}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        AuthResponse response = apiService.login("danghung", "password123");

        assertNotNull(response);
        assertEquals("mocked-jwt-token-12345", response.getToken());
        assertEquals("danghung", response.getUsername());
        assertEquals("BIDDER", response.getRole());
        assertEquals(0, response.getBalance().compareTo(new java.math.BigDecimal("5000000")));
    }

    @Test
    public void testLoginFailure() throws Exception {
        // Giả lập dữ liệu JSON báo lỗi từ Server
        String errorJson = "{"
                + "\"status\":400,"
                + "\"message\":\"Tên đăng nhập hoặc mật khẩu không chính xác\""
                + "}";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn(errorJson);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        IOException exception = assertThrows(IOException.class, () -> {
            apiService.login("danghung", "wrong_password");
        });

        assertEquals("Tên đăng nhập hoặc mật khẩu không chính xác", exception.getMessage());
    }

    @Test
    public void testGetAllAuctionsSuccess() throws Exception {
        String jsonResponse = "["
                + "{"
                + "  \"id\":1,"
                + "  \"name\":\"Bức tranh sơn dầu cổ\","
                + "  \"description\":\"Tranh vẽ phong cảnh thế kỷ 19\","
                + "  \"startPrice\":1000000,"
                + "  \"currentPrice\":1200000,"
                + "  \"status\":\"RUNNING\""
                + "},"
                + "{"
                + "  \"id\":2,"
                + "  \"name\":\"Đồng hồ cổ Thụy Sĩ\","
                + "  \"description\":\"Đồng hồ sản xuất năm 1950\","
                + "  \"startPrice\":5000000,"
                + "  \"currentPrice\":5000000,"
                + "  \"status\":\"OPEN\""
                + "}"
                + "]";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        List<AuctionDto> auctions = apiService.getAllAuctions();

        assertNotNull(auctions);
        assertEquals(2, auctions.size());

        AuctionDto a1 = auctions.get(0);
        assertEquals(1L, a1.getId());
        assertEquals("Bức tranh sơn dầu cổ", a1.getName());
        assertEquals("RUNNING", a1.getStatus());
        assertTrue(a1.isRunning());

        AuctionDto a2 = auctions.get(1);
        assertEquals(2L, a2.getId());
        assertEquals("Đồng hồ cổ Thụy Sĩ", a2.getName());
        assertEquals("OPEN", a2.getStatus());
        assertTrue(a2.isOpen());
    }
}
