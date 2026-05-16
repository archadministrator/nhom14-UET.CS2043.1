    package com.auction.client.service;

    import com.auction.client.model.ClientDto.AuthResponse;

    public class SessionManager {

        private static volatile SessionManager instance;

        private String token;
        private String username;
        private String role;
        private double balance;

        private SessionManager() {}

        public static SessionManager getInstance() {
            if (instance == null) {
                synchronized (SessionManager.class) {
                    if (instance == null) {
                        instance = new SessionManager();
                    }
                }
            }
            return instance;
        }

        public void login(AuthResponse auth) {
            this.token    = auth.getToken();
            this.username = auth.getUsername();
            this.role     = auth.getRole();
            this.balance  = auth.getBalance() != null ? auth.getBalance().doubleValue() : 0.0;
        }

        public void logout() {
            this.token    = null;
            this.username = null;
            this.role     = null;
            this.balance  = 0;
        }

        public boolean isLoggedIn() { return token != null; }
        public boolean isBidder()   { return "BIDDER".equals(role); }
        public boolean isSeller()   { return "SELLER".equals(role); }
        public boolean isAdmin()    { return "ADMIN".equals(role); }

        public String getToken()    { return token; }
        public String getUsername() { return username; }
        public String getRole()     { return role; }
        public double getBalance()  { return balance; }
        public void setBalance(double b) { this.balance = b; }

        public String bearerToken() { return "Bearer " + token; }
    }