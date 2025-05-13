-- 유저 등록
INSERT INTO users(user_id, password) VALUES ('durian', 'durian');
INSERT INTO users(user_id, password) VALUES ('fig', 'fig');

-- 유저 판매 품목 (경매 시작 순)
INSERT INTO items VALUES (101, 'BOOKS', 'Cherry Tree', 'GOOD', 'cherry', null);
INSERT INTO auctions VALUES (101, 101, 3, 3, 10, '2024-02-14 00:00:00', '2024-04-30 00:00:00', 'SOLD');
UPDATE items SET auction_id = 101 WHERE item_id = 101; -- items TABLE과 auctions TABLE 간의 순환 참조 때문에

INSERT INTO items VALUES (102, 'OTHERS', 'Real Banana', 'NEW', 'banana', null);
INSERT INTO auctions VALUES (102, 102, 2, 2, 10, '2024-08-03 00:00:00', '2024-08-05 00:00:00', 'SOLD');
UPDATE items SET auction_id = 102 WHERE item_id = 102;

INSERT INTO items VALUES (103, 'HOME', 'Durian Perfume', 'LIKE_NEW', 'durian', null);
INSERT INTO auctions VALUES (103, 103, 20, 20, 70, '2024-09-12 00:00:00', '2024-10-30 00:00:00', 'SOLD');
UPDATE items SET auction_id = 103 WHERE item_id = 103;

INSERT INTO items VALUES (104, 'CLOTHING', 'Banana Costume', 'LIKE_NEW', 'banana', null);
INSERT INTO auctions VALUES (104, 104, 10, 10, 15, '2025-03-21 00:00:00', '2025-04-01 00:00:00', 'EXPIRED');
UPDATE items SET auction_id = 104 WHERE item_id = 104;

INSERT INTO items VALUES (105, 'OTHERS', 'Real Durian', 'NEW', 'durian', null);
INSERT INTO auctions VALUES (105, 105, 5, 5, 20, '2025-04-30 00:00:00', '2025-05-01 00:00:00', 'SOLD');
UPDATE items SET auction_id = 105 WHERE item_id = 105;

INSERT INTO items VALUES (106, 'ELECTRONICS', 'iPad Pro', 'NEW', 'apple', null);
INSERT INTO auctions VALUES (106, 106, 900, 900, 1200, '2025-05-03 00:00:00', '2025-05-07 00:00:00', 'EXPIRED');
UPDATE items SET auction_id = 106 WHERE item_id = 106;

INSERT INTO items VALUES (107, 'ELECTRONICS', 'CherryPods', 'ACCEPTABLE', 'cherry', null);
INSERT INTO auctions VALUES (107, 107, 4, 4, 25, '2025-05-03 01:00:00', '2025-06-30 00:00:00', 'LISTED');
UPDATE items SET auction_id = 107 WHERE item_id = 107;

INSERT INTO items VALUES (108, 'ELECTRONICS', 'Mac Book Air', 'LIKE_NEW', 'apple', null);
INSERT INTO auctions VALUES (108, 108, 500, 500, 1000, '2025-05-05 00:00:00', '2025-06-25 00:00:00', 'BIDDING');
UPDATE items SET auction_id = 108 WHERE item_id = 108;

INSERT INTO items VALUES (109, 'ELECTRONICS', 'AirPods', 'GOOD', 'apple', null);
INSERT INTO auctions VALUES (109, 109, 100, 100, 200, '2025-05-05 00:00:00', '2025-05-08 00:00:00', 'SOLD');
UPDATE items SET auction_id = 109 WHERE item_id = 109;

INSERT INTO items VALUES (110, 'ELECTRONICS', 'Apple Watch', 'LIKE_NEW', 'apple', null);
INSERT INTO auctions VALUES (110, 110, 300, 300, 800, '2025-05-10 00:00:00', '2025-05-12 00:00:00', 'BIDDING');
UPDATE items SET auction_id = 110 WHERE item_id = 110;


-- 유저 구매 품목
INSERT INTO bids VALUES (101, 'banana', 101, 4, '2024-03-08 00:00:00', 'OUTBID'); -- 서적 체리 나무
INSERT INTO bids VALUES (102, 'apple', 101, 5, '2024-04-15 00:00:00', 'WON'); -- 서적 체리 나무
INSERT INTO billings VALUES (101, 101, 'apple', 'cherry', 5, '2024-04-30 00:00:00', 'COMPLETED'); -- 서적 체리 나무

INSERT INTO bids VALUES (103, 'apple', 102, 3, '2024-08-03 09:00:00', 'OUTBID'); -- 기타 바나나 과일
INSERT INTO bids VALUES (104, 'durian', 102, 8, '2024-08-04 12:00:00', 'WON'); -- 기타 바나나 과일
INSERT INTO billings VALUES (102, 102, 'durian', 'banana', 5, '2024-08-05 00:00:00', 'COMPLETED'); -- 기타 바나나 과일

INSERT INTO bids VALUES (105, 'banana', 103, 30, '2024-09-30 00:00:00', 'WON'); -- 가정 두리안 향수
INSERT INTO billings VALUES (103, 103, 'banana', 'durian', 30, '2024-10-30 00:00:00', 'COMPLETED'); -- 가정 두리안 향수

INSERT INTO bids VALUES (106, 'cherry', 105, 7, '2025-04-30 00:00:00', 'WON'); -- 기타 두리안 과일
INSERT INTO billings VALUES (104, 105, 'cherry', 'durian', 7, '2025-05-01 00:00:00', 'COMPLETED'); -- 기타 두리안 과일

INSERT INTO bids VALUES (107, 'cherry', 109, 200, '2025-05-06 00:00:00', 'WON'); -- 전자제품 에어팟
INSERT INTO billings VALUES (105, 109, 'cherry', 'apple', 200, '2025-05-06 00:00:00', 'COMPLETED'); -- 전자제품 에어팟 (즉시 구매)

INSERT INTO bids VALUES (108, 'banana', 110, 350, '2025-05-10 09:00:00', 'OUTBID'); -- 전자제품 애플 워치
INSERT INTO bids VALUES (109, 'durian', 110, 700, '2025-05-10 13:00:00', 'ACTIVE'); -- 전자제품 애플 워치
-- INSERT INTO billings VALUES (106, 110, 'durian', 'apple', 700, '2025-05-12 00:00:00', 'COMPLETED'); -- 전자제품 애플 워치 (자동 최신화, 현재 BIDDIG)

INSERT INTO bids VALUES (110, 'durian', 108, 600, '2025-05-11 00:00:00', 'ACTIVE'); -- 전자제품 맥북 에어