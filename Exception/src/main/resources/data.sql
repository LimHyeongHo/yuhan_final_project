-- 테스트용 계정 세팅
INSERT INTO USERS (user_id, email, nickname, password, role, miss_count, created_at)
VALUES (1, 'admin@demo.com',  '관리자', 'pass', 'ADMIN',  0, CURRENT_TIMESTAMP);
INSERT INTO USERS (user_id, email, nickname, password, role, miss_count, created_at)
VALUES (2, 'hong@demo.com',   '홍홍홍', 'pass', 'SELLER', 0, CURRENT_TIMESTAMP);
INSERT INTO USERS (user_id, email, nickname, password, role, miss_count, created_at)
VALUES (3, 'gil@demo.com',    '길길길', 'pass', 'SELLER', 0, CURRENT_TIMESTAMP);
INSERT INTO USERS (user_id, email, nickname, password, role, miss_count, created_at)
VALUES (4, 'buyer1@demo.com', '홍홍홍', 'pass', 'BUYER',  0, CURRENT_TIMESTAMP);
INSERT INTO USERS (user_id, email, nickname, password, role, miss_count, created_at)
VALUES (5, 'buyer2@demo.com', '길길길', 'pass', 'BUYER',  0, CURRENT_TIMESTAMP);
INSERT INTO USERS (user_id, email, nickname, password, role, miss_count, created_at)
VALUES (6, 'dong@demo.com',   '동동동', 'pass', 'BUYER',  2, CURRENT_TIMESTAMP);

-- 상품 정보 (시나리오별 다르게 세팅)
INSERT INTO PRODUCT (product_id, seller_id, title, image_url, base_price, target_qty, content, created_at)
VALUES (1, 2, '컴퓨터공학 교재', NULL, 37500, 3, '2026년도 전공 교재 공동구매 - 시나리오 A', CURRENT_TIMESTAMP);

INSERT INTO PRODUCT (product_id, seller_id, title, image_url, base_price, target_qty, content, created_at)
VALUES (2, 3, '컴퓨터공학 교재', NULL, 28000, 2, '2026년도 전공 교재 공동구매 - 시나리오 B', CURRENT_TIMESTAMP);

-- 공동구매 룸 생성
INSERT INTO GROUP_PURCHASE (gp_id, product_id, status, current_qty, discount_rate, end_at, min_qty)
VALUES (1, 1, '모집중', 3, 10, DATEADD('SECOND', -5, CURRENT_TIMESTAMP), 2);

INSERT INTO GROUP_PURCHASE (gp_id, product_id, status, current_qty, discount_rate, end_at, min_qty)
VALUES (2, 2, '모집중', 2, 10, DATEADD('SECOND', -5, CURRENT_TIMESTAMP), 2);

-- 초기 참여자 세팅 (첫 금액 N빵 적용)
INSERT INTO PARTICIPATION (part_id, gp_id, buyer_id, qty, payment_status, final_price, applied_at)
VALUES (1, 1, 4, 1, '완료', 12500, CURRENT_TIMESTAMP);  -- 37500 / 3
INSERT INTO PARTICIPATION (part_id, gp_id, buyer_id, qty, payment_status, final_price, applied_at)
VALUES (2, 1, 5, 1, '완료', 12500, CURRENT_TIMESTAMP);
INSERT INTO PARTICIPATION (part_id, gp_id, buyer_id, qty, payment_status, final_price, applied_at)
VALUES (3, 1, 6, 1, '대기', 12500, CURRENT_TIMESTAMP);

INSERT INTO PARTICIPATION (part_id, gp_id, buyer_id, qty, payment_status, final_price, applied_at)
VALUES (4, 2, 5, 1, '완료', 14000, CURRENT_TIMESTAMP);  -- 28000 / 2
INSERT INTO PARTICIPATION (part_id, gp_id, buyer_id, qty, payment_status, final_price, applied_at)
VALUES (5, 2, 6, 1, '대기', 14000, CURRENT_TIMESTAMP);