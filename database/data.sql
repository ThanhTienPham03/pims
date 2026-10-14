USE ims;

-- 1. ROLES
INSERT INTO roles (name, description) VALUES
    ('ADMIN',      'Toàn quyền hệ thống'),
    ('USER',       'Tạo phiếu, xem phiếu, xem tồn kho, xem báo cáo'),
    ('CONTROLLER', 'Khóa tháng, mở khóa tháng, kiểm soát dữ liệu')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 2. USERS
INSERT INTO users (username, password, full_name, email, is_active) VALUES
    ('admin',      '$2a$10$vhdT7j7GM3o9pspChw.GL.SF4yhChZXkXNsjakl3mFJoYuHE12P9S', 'Quản trị viên',  'admin@pims.local',      TRUE),
    ('user01',     '$2a$10$vhdT7j7GM3o9pspChw.GL.SF4yhChZXkXNsjakl3mFJoYuHE12P9S', 'Nhân viên kho',  'user01@pims.local',     TRUE),
    ('controller', '$2a$10$vhdT7j7GM3o9pspChw.GL.SF4yhChZXkXNsjakl3mFJoYuHE12P9S', 'Kiểm soát viên', 'controller@pims.local', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 3. USER_ROLES
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE (u.username = 'admin'      AND r.name = 'ADMIN')
   OR (u.username = 'admin'      AND r.name = 'USER')
   OR (u.username = 'admin'      AND r.name = 'CONTROLLER')
   OR (u.username = 'user01'     AND r.name = 'USER')
   OR (u.username = 'controller' AND r.name = 'CONTROLLER');

-- 4. PRODUCT_GROUPS & UNITS
INSERT INTO product_groups (code, name, is_active) VALUES
    ('VPP',  'Văn phòng phẩm',        TRUE),
    ('ACTT', 'Ấn chỉ thông thường',   TRUE),
    ('ACQT', 'Ấn chỉ quan trọng',     TRUE),
    ('VLK',  'Vật liệu khác',         TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO units (code, name, is_active) VALUES
    ('CAI',   'Cái',   TRUE),
    ('HOP',   'Hộp',   TRUE),
    ('QUYEN', 'Quyển', TRUE),
    ('BO',    'Bộ',    TRUE),
    ('REAM',  'Ream',  TRUE),
    ('CUON',  'Cuộn',  TRUE),
    ('BINH',  'Bình',  TRUE),
    ('GOI',   'Gói',   TRUE),
    ('THUNG', 'Thùng', TRUE),
    ('CAY',   'Cây',   TRUE),
    ('TO',    'Tờ',    TRUE),
    ('KG',    'Kg',    TRUE),
    ('LIT',   'Lít',   TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 5. PRODUCTS
INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.7100001.001', 'Giấy A4 Double A',
       (SELECT id FROM product_groups WHERE code = 'VPP'),
       (SELECT id FROM units WHERE code = 'REAM'),
       90000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.7100001.001');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.7100001.002', 'Bút bi Thiên Long TL-027',
       (SELECT id FROM product_groups WHERE code = 'VPP'),
       (SELECT id FROM units WHERE code = 'CAY'),
       5000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.7100001.002');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.7100001.003', 'Băng dính trong 2cm',
       (SELECT id FROM product_groups WHERE code = 'VPP'),
       (SELECT id FROM units WHERE code = 'CUON'),
       8000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.7100001.003');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.9020100.001', 'Máy tính Casio FX-580VNX',
       (SELECT id FROM product_groups WHERE code = 'ACTT'),
       (SELECT id FROM units WHERE code = 'CAI'),
       350000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.9020100.001');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.9020100.002', 'Sổ tay kẻ ngang A5',
       (SELECT id FROM product_groups WHERE code = 'ACTT'),
       (SELECT id FROM units WHERE code = 'QUYEN'),
       25000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.9020100.002');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.9080117.001', 'Vật liệu lót sàn kho',
       (SELECT id FROM product_groups WHERE code = 'VLK'),
       (SELECT id FROM units WHERE code = 'CAI'),
       150000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.9080117.001');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.5010000.001', 'Tài liệu quản trị mật',
       (SELECT id FROM product_groups WHERE code = 'ACQT'),
       (SELECT id FROM units WHERE code = 'BO'),
       500000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.5010000.001');

-- 6. INVENTORY
INSERT INTO inventory (product_id, opening_quantity, opening_amount, ending_quantity, ending_amount)
SELECT id, 0, 0, 0, 0 FROM products
WHERE id NOT IN (SELECT product_id FROM inventory);

-- 7. KKTS
INSERT INTO kkts (product_id, year, month, opening_quantity, opening_amount, ending_quantity, ending_amount)
SELECT id,
       YEAR(CURDATE()),
       MONTH(CURDATE()),
       0, 0, 0, 0
FROM products
WHERE id NOT IN (
    SELECT product_id FROM kkts
    WHERE year = YEAR(CURDATE()) AND month = MONTH(CURDATE())
);

-- 8. MONTHLY_CLOSINGS
INSERT IGNORE INTO monthly_closings (year, month, is_locked)
VALUES (YEAR(CURDATE()), MONTH(CURDATE()), FALSE);

-- 9. DOCUMENT_SEQUENCES
INSERT IGNORE INTO document_sequences (year, group_code, doc_type, last_sequence) VALUES
    (YEAR(CURDATE()), 'VPP',  'IMPORT', 0),
    (YEAR(CURDATE()), 'VPP',  'EXPORT', 0),
    (YEAR(CURDATE()), 'ACTT', 'IMPORT', 0),
    (YEAR(CURDATE()), 'ACTT', 'EXPORT', 0),
    (YEAR(CURDATE()), 'ACQT', 'IMPORT', 0),
    (YEAR(CURDATE()), 'ACQT', 'EXPORT', 0),
    (YEAR(CURDATE()), 'VLK',  'IMPORT', 0),
    (YEAR(CURDATE()), 'VLK',  'EXPORT', 0);
