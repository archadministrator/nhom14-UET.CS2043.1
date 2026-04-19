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
    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public User login(String username, String password) throws Exception {
        String json = objectMapper.writeValueAsString(new LoginRequest(username, password));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), User.class);
        }
        return null;
    }

    public List<Auction> getAllAuctions() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auctions"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), new TypeReference<List<Auction>>() {});
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/bids"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Bid.class);
        }
        return null;
    }

    // Helper classes for serialization
    private static class LoginRequest {
        public String username;
        public String password;
        public LoginRequest(String u, String p) { username = u; password = p; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    private static class BidRequest {
        public Long auctionId;
        public Long userId;
        public Double amount;
        public BidRequest(Long a, Long u, Double amt) {
            auctionId = a; userId = u; amount = amt;
        }
        public Long getAuctionId() { return auctionId; }
        public Long getUserId() { return userId; }
        public Double getAmount() { return amount; }
    }
}