CREATE TABLE IF NOT EXISTS users (
                                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username    TEXT NOT NULL UNIQUE,
                                     email       TEXT NOT NULL UNIQUE,
                                     password    TEXT NOT NULL,
                                     role        TEXT NOT NULL DEFAULT 'BIDDER'
                                     CHECK(role IN ('BIDDER','SELLER','ADMIN')),
    balance     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active   BOOLEAN NOT NULL DEFAULT 1
    );

CREATE TABLE IF NOT EXISTS auction_items (
                                             id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                             seller_id       INTEGER NOT NULL,
                                             name            TEXT NOT NULL,
                                             description     TEXT,
                                             start_price     DECIMAL(15,2) NOT NULL,
    current_price   DECIMAL(15,2) NOT NULL,
    min_increment   DECIMAL(15,2) NOT NULL DEFAULT 1000,
    start_time      DATETIME NOT NULL,
    end_time        DATETIME NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OPEN'
    CHECK(status IN ('OPEN','RUNNING','FINISHED','PAID','CANCELED')),
    winner_id       INTEGER,
    image_url       TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS bids (
                                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                    auction_id  INTEGER NOT NULL,
                                    bidder_id   INTEGER NOT NULL,
                                    amount      DECIMAL(15,2) NOT NULL,
    is_auto_bid BOOLEAN NOT NULL DEFAULT 0,
    bid_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (auction_id) REFERENCES auction_items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS auto_bid_configs (
                                                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                                                bidder_id   INTEGER NOT NULL,
                                                auction_id  INTEGER NOT NULL,
                                                max_amount  DECIMAL(15,2) NOT NULL,
    increment   DECIMAL(15,2) NOT NULL DEFAULT 1000,
    is_active   BOOLEAN NOT NULL DEFAULT 1,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (bidder_id, auction_id),

    FOREIGN KEY (bidder_id) REFERENCES users(id),
    FOREIGN KEY (auction_id) REFERENCES auction_items(id)
    );

CREATE INDEX IF NOT EXISTS idx_auction_status
    ON auction_items(status);

CREATE INDEX IF NOT EXISTS idx_auction_end_time
    ON auction_items(end_time);

CREATE INDEX IF NOT EXISTS idx_bids_auction
    ON bids(auction_id);

CREATE INDEX IF NOT EXISTS idx_bids_bidder
    ON bids(bidder_id);