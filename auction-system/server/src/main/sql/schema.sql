CREATE DATABASE IF NOT EXISTS auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('BIDDER','SELLER','ADMIN') NOT NULL DEFAULT 'BIDDER',
    balance     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS auction_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id       BIGINT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    start_price     DECIMAL(15,2) NOT NULL,
    current_price   DECIMAL(15,2) NOT NULL,
    min_increment   DECIMAL(15,2) NOT NULL DEFAULT 1000,
    start_time      DATETIME NOT NULL,
    end_time        DATETIME NOT NULL,
    status          ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELED') NOT NULL DEFAULT 'OPEN',
    winner_id       BIGINT,
    image_url       VARCHAR(500),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bids (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id  BIGINT NOT NULL,
    bidder_id   BIGINT NOT NULL,
    amount      DECIMAL(15,2) NOT NULL,
    is_auto_bid BOOLEAN NOT NULL DEFAULT FALSE,
    bid_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction_items(id),
    FOREIGN KEY (bidder_id)  REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS auto_bid_configs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    bidder_id   BIGINT NOT NULL,
    auction_id  BIGINT NOT NULL,
    max_amount  DECIMAL(15,2) NOT NULL,
    increment   DECIMAL(15,2) NOT NULL DEFAULT 1000,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bidder_auction (bidder_id, auction_id),
    FOREIGN KEY (bidder_id)  REFERENCES users(id),
    FOREIGN KEY (auction_id) REFERENCES auction_items(id)
);

CREATE INDEX idx_auction_status   ON auction_items(status);
CREATE INDEX idx_auction_end_time ON auction_items(end_time);
CREATE INDEX idx_bids_auction     ON bids(auction_id);
CREATE INDEX idx_bids_bidder      ON bids(bidder_id);
