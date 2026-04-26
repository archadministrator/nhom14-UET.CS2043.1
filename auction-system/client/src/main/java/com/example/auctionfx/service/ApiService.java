package com.example.auctionfx.service;

import com.example.auctionfx.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiService {
    private static final String BASE_URL = "http://127.0.0.1:8080/api";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Bỏ qua các trường thừa như createdAt, updatedAt
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean login(String username, String password) throws Exception {
        java.util.Map<String, String> credentials = new java.util.HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);
        
        String json = objectMapper.writeValueAsString(credentials);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            java.util.Map<String, String> result = objectMapper.readValue(response.body(), new TypeReference<>() {});
            SessionManager.getInstance().setJwtToken(result.get("jwt"));
            SessionManager.getInstance().setRole(result.get("role"));
            
            // Lấy thông tin user cơ bản (có thể gọi thêm API profile nếu cần)
            User user = new User() {
                @Override
                public String getRoleName() { return result.get("role"); }
            };
            user.setUsername(result.get("username"));
            SessionManager.getInstance().setCurrentUser(user);
            return true;
        } else {
            return false;
        }
    }

    public boolean register(String username, String password, String email, String role) throws Exception {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("username", username);
        data.put("password", password);
        data.put("email", email);
        data.put("role", role);
        
        String json = objectMapper.writeValueAsString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    private HttpRequest.Builder authenticatedRequestBuilder(String endpoint) {
        String token = SessionManager.getInstance().getJwtToken();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public List<Auction> getAllAuctions() throws Exception {
        HttpRequest request = authenticatedRequestBuilder("/auctions")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // Do backend trả về Page, ta cần lấy nội dung 'content'
        java.util.Map<String, Object> map = objectMapper.readValue(response.body(), new TypeReference<>() {});
        return objectMapper.convertValue(map.get("content"), new TypeReference<List<Auction>>() {});
    }

    public Auction getAuction(Long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auctions/" + id))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Auction.class);
    }

    public Bid placeBid(Long auctionId, Long userId, Double amount) throws Exception {
        BidRequest req = new BidRequest(auctionId, userId, amount);
        String json = objectMapper.writeValueAsString(req);
        HttpRequest request = authenticatedRequestBuilder("/bids")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Bid.class);
        }
        return null;
    }

    // Sử dụng Record (Java 17) để định nghĩa các DTO ngắn gọn và hiện đại
    // Giúp giải quyết triệt để cảnh báo 'unused field' vì record tự quản lý các trường này

    private record BidRequest(Long auctionId, Long userId, Double amount) {}
}