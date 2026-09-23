-- ============================================================
-- PIMS - Sample Data (đồng bộ với schema v2.0)
-- ============================================================
-- Sử dụng sau khi chạy schema.sql
-- Bảng categories thay thế product_groups + units
-- Bảng inventory_transactions thay thế imports + exports
-- ============================================================

USE pims_db;

-- ============================================================
-- 1. ROLES (seed nếu chưa có — schema.sql đã INSERT IGNORE)
-- ============================================================
INSERT INTO roles (name, description) VALUES
    ('ADMIN',      'Toàn quyền hệ thống'),
    ('USER',       'Tạo phiếu, xem phiếu, xem tồn kho, xem báo cáo'),
    ('CONTROLLER', 'Khóa tháng, mở khóa tháng, kiểm soát dữ liệu')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ============================================================
-- 2. USERS (password: Admin@123 - BCrypt encoded)
-- ============================================================
INSERT INTO users (username, password, full_name, email, is_active) VALUES
    ('admin',      '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/.H/b6OiT2CbQ.VCCS', 'Quản trị viên',  'admin@pims.local',      TRUE),
    ('user01',     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/.H/b6OiT2CbQ.VCCS', 'Nhân viên kho',  'user01@pims.local',     TRUE),
    ('controller', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/.H/b6OiT2CbQ.VCCS', 'Kiểm soát viên', 'controller@pims.local', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- ============================================================
-- 3. USER_ROLES
-- ============================================================
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE (u.username = 'admin'      AND r.name = 'ADMIN')
   OR (u.username = 'admin'      AND r.name = 'USER')
   OR (u.username = 'admin'      AND r.name = 'CONTROLLER')
   OR (u.username = 'user01'     AND r.name = 'USER')
   OR (u.username = 'controller' AND r.name = 'CONTROLLER');

-- ============================================================
-- 4. CATEGORIES — Nhóm hàng + Đơn vị tính
-- (schema.sql đã seed cơ bản, đây là bổ sung nếu cần)
-- ============================================================
INSERT INTO categories (type, code, name, is_active) VALUES
    ('PRODUCT_GROUP', 'VPP',  'Văn phòng phẩm',        TRUE),
    ('PRODUCT_GROUP', 'ACTT', 'Ấn chỉ thông thường',   TRUE),
    ('PRODUCT_GROUP', 'ACQT', 'Ấn chỉ quan trọng',     TRUE),
    ('PRODUCT_GROUP', 'VLK',  'Vật liệu khác',         TRUE),
    ('UNIT', 'CAI',   'Cái',   TRUE),
    ('UNIT', 'HOP',   'Hộp',   TRUE),
    ('UNIT', 'QUYEN', 'Quyển', TRUE),
    ('UNIT', 'BO',    'Bộ',    TRUE),
    ('UNIT', 'REAM',  'Ream',  TRUE),
    ('UNIT', 'CUON',  'Cuộn',  TRUE),
    ('UNIT', 'BINH',  'Bình',  TRUE),
    ('UNIT', 'GOI',   'Gói',   TRUE),
    ('UNIT', 'THUNG', 'Thùng', TRUE),
    ('UNIT', 'CAY',   'Cây',   TRUE),
    ('UNIT', 'TO',    'Tờ',    TRUE),
    ('UNIT', 'KG',    'Kg',    TRUE),
    ('UNIT', 'LIT',   'Lít',   TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ============================================================
-- 5. PRODUCTS
-- Sử dụng categories thay vì product_groups / units
-- ============================================================
INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.7100001.001', 'Giấy A4 Double A',
       (SELECT id FROM categories WHERE type = 'PRODUCT_GROUP' AND code = 'VPP'),
       (SELECT id FROM categories WHERE type = 'UNIT' AND code = 'REAM'),
       90000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.7100001.001');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.7100001.002', 'Bút bi Thiên Long TL-027',
       (SELECT id FROM categories WHERE type = 'PRODUCT_GROUP' AND code = 'VPP'),
       (SELECT id FROM categories WHERE type = 'UNIT' AND code = 'CAY'),
       5000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.7100001.002');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.7100001.003', 'Băng dính trong 2cm',
       (SELECT id FROM categories WHERE type = 'PRODUCT_GROUP' AND code = 'VPP'),
       (SELECT id FROM categories WHERE type = 'UNIT' AND code = 'CUON'),
       8000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.7100001.003');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.9020100.001', 'Máy tính Casio FX-580VNX',
       (SELECT id FROM categories WHERE type = 'PRODUCT_GROUP' AND code = 'ACTT'),
       (SELECT id FROM categories WHERE type = 'UNIT' AND code = 'CAI'),
       350000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.9020100.001');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.9020100.002', 'Sổ tay kẻ ngang A5',
       (SELECT id FROM categories WHERE type = 'PRODUCT_GROUP' AND code = 'ACTT'),
       (SELECT id FROM categories WHERE type = 'UNIT' AND code = 'QUYEN'),
       25000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.9020100.002');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.9080117.001', 'Vật liệu lót sàn kho',
       (SELECT id FROM categories WHERE type = 'PRODUCT_GROUP' AND code = 'VLK'),
       (SELECT id FROM categories WHERE type = 'UNIT' AND code = 'CAI'),
       150000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.9080117.001');

INSERT INTO products (ma_so, ten_hang, product_group_id, unit_id, don_gia_nhap, trang_thai, created_by)
SELECT '32F.5010000.001', 'Tài liệu quản trị mật',
       (SELECT id FROM categories WHERE type = 'PRODUCT_GROUP' AND code = 'ACQT'),
       (SELECT id FROM categories WHERE type = 'UNIT' AND code = 'BO'),
       500000, 'HOAT_DONG', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE ma_so = '32F.5010000.001');

-- ============================================================
-- 6. INVENTORY — Tồn ban đầu = 0 cho tất cả sản phẩm
-- ============================================================
INSERT INTO inventory (product_id, opening_quantity, opening_amount, ending_quantity, ending_amount)
SELECT id, 0, 0, 0, 0 FROM products
WHERE id NOT IN (SELECT product_id FROM inventory);

-- ============================================================
-- 7. KKTS — Tháng hiện tại (tồn ban đầu = 0)
-- ============================================================
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

-- ============================================================
-- 8. MONTHLY_CLOSINGS — Init tháng hiện tại (chưa khóa)
-- ============================================================
INSERT IGNORE INTO monthly_closings (year, month, is_locked)
VALUES (YEAR(CURDATE()), MONTH(CURDATE()), FALSE);

-- ============================================================
-- 9. DOCUMENT_SEQUENCES — Khởi tạo cho năm hiện tại
-- ============================================================
INSERT IGNORE INTO document_sequences (year, group_code, doc_type, last_sequence) VALUES
    (YEAR(CURDATE()), 'VPP',  'IMPORT', 0),
    (YEAR(CURDATE()), 'VPP',  'EXPORT', 0),
    (YEAR(CURDATE()), 'ACTT', 'IMPORT', 0),
    (YEAR(CURDATE()), 'ACTT', 'EXPORT', 0),
    (YEAR(CURDATE()), 'ACQT', 'IMPORT', 0),
    (YEAR(CURDATE()), 'ACQT', 'EXPORT', 0),
    (YEAR(CURDATE()), 'VLK',  'IMPORT', 0),
    (YEAR(CURDATE()), 'VLK',  'EXPORT', 0);
