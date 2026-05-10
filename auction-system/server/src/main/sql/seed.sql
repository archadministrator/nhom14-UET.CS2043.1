USE auction_db;


INSERT INTO users (username, email, password, role, balance) VALUES
('admin',   'admin@auction.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN',  0.00),
('seller1', 'seller1@auction.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SELLER', 0.00),
('seller2', 'seller2@auction.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SELLER', 0.00),
('bidder1', 'bidder1@auction.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'BIDDER', 50000000.00),
('bidder2', 'bidder2@auction.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'BIDDER', 30000000.00),
('bidder3', 'bidder3@auction.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'BIDDER', 20000000.00);

INSERT INTO auction_items (seller_id, name, description, start_price, current_price, min_increment, start_time, end_time, status) VALUES
(2, 'MacBook Pro M3 2024',  'MacBook Pro 14 inch, chip M3, 16GB RAM, 512GB SSD, còn bảo hành.', 25000000, 25000000, 500000,  NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY),    'RUNNING'),
(2, 'iPhone 15 Pro Max',    'iPhone 15 Pro Max 256GB, màu Titan Đen, fullbox, chưa active.',     28000000, 28000000, 500000,  DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), 'OPEN'),
(3, 'Sony Alpha A7M4',      'Sony Alpha 7 IV body only, đã qua sử dụng, còn rất mới.',           35000000, 35000000, 1000000, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY),    'RUNNING'),
(3, 'Đồng hồ Seiko SBDC',  'Seiko Prospex SBDC101, full box and papers, limited edition.',       15000000, 15000000, 200000,  DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 DAY), 'OPEN');
