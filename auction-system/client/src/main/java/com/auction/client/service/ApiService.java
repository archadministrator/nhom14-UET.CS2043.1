package com.auction.client.service;

import com.auction.client.model.ClientDto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiService {

    private static final String BASE_URL = "http://localhost:8080/api";

    private static ApiService instance;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final SessionManager session = SessionManager.getInstance();

    public static ApiService getInstance() {
        if (instance == null) instance = new ApiService();
        return instance;
    }

    private ApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── AUTH ──────────────────────────────────────────────────────────

    public AuthResponse register(String username, String email, String password, String role)
            throws IOException, InterruptedException {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        body.put("role", role);
        return post("/auth/register", body, AuthResponse.class, false);
    }

    public AuthResponse login(String username, String password)
            throws IOException, InterruptedException {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        return post("/auth/login", body, AuthResponse.class, false);
    }

    // ── AUCTIONS ──────────────────────────────────────────────────────

    public List<AuctionDto> getAllAuctions() throws IOException, InterruptedException {
        return getList("/auctions", new TypeReference<>() {});
    }

    public List<AuctionDto> getActiveAuctions() throws IOException, InterruptedException {
        return getList("/auctions/active", new TypeReference<>() {});
    }

    public List<AuctionDto> getMySales() throws IOException, InterruptedException {
        return getList("/auctions/my-sales", new TypeReference<>() {});
    }

    public List<AuctionDto> searchAuctions(String keyword) throws IOException, InterruptedException {
        return getList("/auctions?keyword=" + keyword, new TypeReference<>() {});
    }

    public AuctionDto getAuction(Long id) throws IOException, InterruptedException {
        return get("/auctions/" + id, AuctionDto.class);
    }

    public AuctionDto createAuction(Map<String, Object> data) throws IOException, InterruptedException {
        return post("/auctions", data, AuctionDto.class, true);
    }

    public AuctionDto updateAuction(Long id, Map<String, Object> data)
            throws IOException, InterruptedException {
        return put("/auctions/" + id, data, AuctionDto.class);
    }

    public void deleteAuction(Long id) throws IOException, InterruptedException {
        delete("/auctions/" + id);
    }

    // ── BIDS ──────────────────────────────────────────────────────────

    public BidDto placeBid(Long auctionId, BigDecimal amount)
            throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        return post("/bids/" + auctionId, body, BidDto.class, true);
    }

    public List<BidDto> getBidHistory(Long auctionId) throws IOException, InterruptedException {
        return getList("/bids/" + auctionId + "/history", new TypeReference<>() {});
    }

    public List<BidDto> getMyBids() throws IOException, InterruptedException {
        return getList("/bids/my", new TypeReference<>() {});
    }

    // ── AUTO-BID ──────────────────────────────────────────────────────

    public void setupAutoBid(Long auctionId, BigDecimal maxAmount, BigDecimal increment)
            throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("maxAmount", maxAmount);
        body.put("increment", increment);
        post("/autobid/" + auctionId, body, Void.class, true);
    }

    public void cancelAutoBid(Long auctionId) throws IOException, InterruptedException {
        delete("/autobid/" + auctionId);
    }

    // ── USER ──────────────────────────────────────────────────────────

    public UserDto getMyProfile() throws IOException, InterruptedException {
        return get("/users/me", UserDto.class);
    }

    public UserDto topUp(BigDecimal amount) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        return post("/users/me/topup", body, UserDto.class, true);
    }

    // ── ADMIN ─────────────────────────────────────────────────────────

    public List<UserDto> getAllUsers() throws IOException, InterruptedException {
        return getList("/admin/users", new TypeReference<>() {});
    }

    public void toggleUserActive(Long userId, boolean active) throws IOException, InterruptedException {
        put("/admin/users/" + userId + "/active?active=" + active, null, Void.class);
    }

    public List<AuctionDto> getAllAuctionsAdmin() throws IOException, InterruptedException {
        return getList("/admin/auctions", new TypeReference<>() {});
    }

    public void markAuctionPaid(Long auctionId) throws IOException, InterruptedException {
        put("/admin/auctions/" + auctionId + "/paid", null, Void.class);
    }

    // ── HTTP HELPERS ──────────────────────────────────────────────────

    private <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder(path, true).GET().build();
        return execute(req, type);
    }

    private <T> List<T> getList(String path, TypeReference<List<T>> typeRef)
            throws IOException, InterruptedException {
        HttpRequest req = requestBuilder(path, true).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return mapper.readValue(resp.body(), typeRef);
    }

    private <T> T post(String path, Object body, Class<T> type, boolean auth)
            throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = requestBuilder(path, auth)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return execute(req, type);
    }

    private <T> T put(String path, Object body, Class<T> type)
            throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = requestBuilder(path, true)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return execute(req, type);
    }

    private void delete(String path) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder(path, true).DELETE().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
    }

    private HttpRequest.Builder requestBuilder(String path, boolean auth) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15));
        if (auth && session.isLoggedIn())
            builder.header("Authorization", session.bearerToken());
        return builder;
    }

    private <T> T execute(HttpRequest req, Class<T> type)
            throws IOException, InterruptedException {
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        if (type == Void.class || resp.body() == null || resp.body().isBlank()) return null;
        return mapper.readValue(resp.body(), type);
    }

    private void checkStatus(HttpResponse<String> resp) throws IOException {
        if (resp.statusCode() >= 400) {
            String message = "Lỗi server (" + resp.statusCode() + ")";
            try {
                ErrorResponse err = mapper.readValue(resp.body(), ErrorResponse.class);
                if (err.getMessage() != null) message = err.getMessage();
            } catch (Exception ignored) {}
            throw new IOException(message);
        }
    }
}