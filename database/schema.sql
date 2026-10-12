-- ============================================================
-- PIMS - Publication Inventory Management System
-- Database Schema - MariaDB (đồng bộ với JPA Entities)
-- ============================================================
-- Encoding:  UTF-8
-- Collation: utf8mb4_unicode_ci
-- Timezone:  Asia/Ho_Chi_Minh
-- Version:   2.0 — Optimized & synced with Spring Boot entities
-- ============================================================
--
-- THAY ĐỔI SO VỚI SCHEMA 1.0:
--   • product_groups + units  → categories (unified, có type ENUM)
--   • imports + exports       → inventory_transactions (unified, có type ENUM)
--   • import_details + export_details → inventory_transaction_details
--   • stock_card_entries      → XÓA (tính toán on-the-fly từ transaction details)
--   • account_codes           → XÓA (chưa dùng trong business logic)
--   • Giảm từ 17 bảng → 11 bảng
--   • Thêm CHECK constraints, composite indexes tối ưu truy vấn
-- ============================================================

CREATE DATABASE IF NOT EXISTS ims
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ims;

-- ============================================================
-- 1. ROLES
-- ============================================================
-- Vai trò hệ thống: ADMIN, USER, CONTROLLER
-- Entity: Role.java
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200),

    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. USERS
-- ============================================================
-- Tài khoản người dùng hệ thống
-- Entity: User.java
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(200),
    email      VARCHAR(200),
    phone      VARCHAR(20),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_users_username (username),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. USER_ROLES (junction table)
-- ============================================================
-- Quan hệ N:N giữa users và roles
-- Entity: User.java @ManyToMany
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. CATEGORIES (danh mục chung — thay thế product_groups + units)
-- ============================================================
-- Bảng danh mục thống nhất cho:
--   • PRODUCT_GROUP: Nhóm hàng (VPP, ACTT, ACQT, VLK)
--   • UNIT:          Đơn vị tính (Cái, Hộp, Quyển, Ream, ...)
-- Entity: Category.java
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL,      -- 'PRODUCT_GROUP' | 'UNIT'
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Unique: mỗi (type, code) chỉ xuất hiện 1 lần
    UNIQUE KEY uq_categories_type_code (type, code),
    INDEX idx_categories_type (type),
    INDEX idx_categories_active (type, is_active),

    -- Data integrity
    CONSTRAINT chk_categories_type CHECK (type IN ('PRODUCT_GROUP', 'UNIT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. PRODUCTS (hàng hóa)
-- ============================================================
-- Danh mục sản phẩm/ấn phẩm/văn phòng phẩm
-- FK tham chiếu đến categories (thay vì product_groups + units)
-- Entity: Product.java extends BaseEntity
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    ma_so            VARCHAR(50)    NOT NULL,       -- Mã số hàng hóa
    ten_hang         VARCHAR(300)   NOT NULL,       -- Tên hàng hóa
    product_group_id BIGINT         NOT NULL,       -- FK → categories (type=PRODUCT_GROUP)
    unit_id          BIGINT         NOT NULL,       -- FK → categories (type=UNIT)
    don_gia_nhap     DECIMAL(18, 0) NOT NULL DEFAULT 0,  -- Đơn giá nhập
    trang_thai       VARCHAR(20)    NOT NULL DEFAULT 'HOAT_DONG',
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),                  -- JPA Auditing
    updated_by       VARCHAR(100),                  -- JPA Auditing

    UNIQUE KEY uq_products_ma_so (ma_so),
    INDEX idx_products_status (trang_thai),
    INDEX idx_products_group (product_group_id),
    INDEX idx_products_ten_hang (ten_hang(100)),     -- Prefix index cho tên dài

    CONSTRAINT fk_prod_group FOREIGN KEY (product_group_id) REFERENCES categories(id),
    CONSTRAINT fk_prod_unit  FOREIGN KEY (unit_id)          REFERENCES categories(id),
    CONSTRAINT chk_products_status CHECK (trang_thai IN ('HOAT_DONG', 'NGUNG_SU_DUNG')),
    CONSTRAINT chk_products_price  CHECK (don_gia_nhap >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. INVENTORY (tồn kho hiện tại — 1:1 với product)
-- ============================================================
-- Bảng tồn kho realtime, cập nhật sau mỗi phiếu nhập/xuất
-- Công thức:
--   ending_quantity = opening_quantity + import_quantity - export_quantity
--   ending_amount   = opening_amount   + import_amount   - export_amount
--   average_price   = ROUND(ending_amount / ending_quantity, 0) nếu ending_quantity > 0
-- Entity: Inventory.java
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT         NOT NULL,
    opening_quantity DECIMAL(18, 3) NOT NULL DEFAULT 0,
    opening_amount   DECIMAL(18, 0) NOT NULL DEFAULT 0,
    import_quantity  DECIMAL(18, 3) NOT NULL DEFAULT 0,
    import_amount    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    export_quantity  DECIMAL(18, 3) NOT NULL DEFAULT 0,
    export_amount    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    ending_quantity  DECIMAL(18, 3) NOT NULL DEFAULT 0,
    ending_amount    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    average_price    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_inv_product (product_id),
    INDEX idx_inv_ending_qty (ending_quantity),       -- Tìm SP còn tồn kho

    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inv_quantities CHECK (
        opening_quantity >= 0 AND import_quantity >= 0 AND
        export_quantity >= 0 AND ending_quantity >= 0 AND average_price >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. INVENTORY_TRANSACTIONS (phiếu giao dịch nhập/xuất thống nhất)
-- ============================================================
-- Thay thế 2 bảng imports + exports bằng 1 bảng duy nhất
-- Phân biệt nhờ cột `type`: IMPORT / EXPORT / ADJUSTMENT / CHECK
-- Entity: InventoryTransaction.java
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
    transaction_code    VARCHAR(50)    NOT NULL,       -- Số phiếu (format: STT/GROUP-YYYY)
    type                VARCHAR(20)    NOT NULL,       -- IMPORT | EXPORT | ADJUSTMENT | CHECK
    transaction_date    DATE           NOT NULL,       -- Ngày giao dịch
    source              VARCHAR(300),                  -- Nơi mua (IMPORT)
    destination         VARCHAR(300),                  -- Nơi nhập/nơi nhận
    reason              VARCHAR(500),                  -- Lý do xuất (EXPORT)
    invoice_number      VARCHAR(100),                  -- Số hóa đơn
    invoice_date        DATE,                          -- Ngày hóa đơn
    debit_account       VARCHAR(20),                   -- TK Nợ
    credit_account      VARCHAR(20),                   -- TK Có
    total_amount        DECIMAL(18, 0) NOT NULL DEFAULT 0,
    status              VARCHAR(20)    NOT NULL DEFAULT 'HOAN_THANH',
    group_representative VARCHAR(10),                  -- Nhóm đại diện
    workflow_key        VARCHAR(100),                  -- Camunda workflow key
    workflow_status     VARCHAR(20)    DEFAULT 'COMPLETED',
    created_by          VARCHAR(100),
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_tx_type_code (type, transaction_code),
    INDEX idx_tx_type (type),
    INDEX idx_tx_date (transaction_date),
    INDEX idx_tx_status (status),
    -- Composite index: phục vụ dashboard query đếm phiếu theo type + khoảng ngày
    INDEX idx_tx_type_date (type, transaction_date),
    -- Composite index: tìm phiếu theo trạng thái + loại
    INDEX idx_tx_status_type (status, type),

    CONSTRAINT chk_tx_type   CHECK (type IN ('IMPORT', 'EXPORT', 'ADJUSTMENT', 'CHECK')),
    CONSTRAINT chk_tx_status CHECK (status IN ('HOAN_THANH', 'HUY')),
    CONSTRAINT chk_tx_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. INVENTORY_TRANSACTION_DETAILS (chi tiết phiếu giao dịch)
-- ============================================================
-- Thay thế 2 bảng import_details + export_details
-- Entity: InventoryTransactionDetail.java
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_transaction_details (
    id                 BIGINT         AUTO_INCREMENT PRIMARY KEY,
    transaction_id     BIGINT         NOT NULL,
    product_id         BIGINT         NOT NULL,
    product_group_code VARCHAR(10)    NOT NULL,        -- Mã nhóm hàng (denormalized for reporting)
    quantity           DECIMAL(18, 3) NOT NULL,
    unit_price         DECIMAL(18, 0) NOT NULL,
    amount             DECIMAL(18, 0) NOT NULL,
    created_at         DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_txd_transaction (transaction_id),
    INDEX idx_txd_product (product_id),
    -- Composite: truy vấn sổ thẻ kho (stock card) theo product + thứ tự thời gian
    INDEX idx_txd_product_tx (product_id, transaction_id),

    CONSTRAINT fk_txd_transaction FOREIGN KEY (transaction_id) REFERENCES inventory_transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_txd_product     FOREIGN KEY (product_id)     REFERENCES products(id),
    CONSTRAINT chk_txd_quantity   CHECK (quantity > 0),
    CONSTRAINT chk_txd_price      CHECK (unit_price >= 0),
    CONSTRAINT chk_txd_amount     CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. DOCUMENT_SEQUENCES (cấp số phiếu tự động)
-- ============================================================
-- Quản lý số thứ tự phiếu theo nhóm hàng, loại phiếu, và năm
-- Format: STT/GROUP-YYYY  (e.g. 60/VPP-2026)
-- Dùng PESSIMISTIC_WRITE lock khi cấp số để tránh race condition
-- Entity: DocumentSequence.java
-- ============================================================
CREATE TABLE IF NOT EXISTS document_sequences (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    year          SMALLINT    NOT NULL,
    group_code    VARCHAR(10) NOT NULL,
    doc_type      VARCHAR(20) NOT NULL DEFAULT 'EXPORT',
    last_sequence INT         NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_ds_year_group_type (year, group_code, doc_type),

    CONSTRAINT chk_ds_doc_type  CHECK (doc_type IN ('IMPORT', 'EXPORT')),
    CONSTRAINT chk_ds_sequence  CHECK (last_sequence >= 0),
    CONSTRAINT chk_ds_year      CHECK (year >= 2020 AND year <= 2099)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. MONTHLY_CLOSINGS (khóa sổ kỳ kế toán)
-- ============================================================
-- Chỉ CONTROLLER mới được khóa/mở khóa
-- Khi khóa: không cho nhập/xuất/hủy phiếu trong tháng đó
-- Entity: MonthlyClosing.java
-- ============================================================
CREATE TABLE IF NOT EXISTS monthly_closings (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    year        SMALLINT    NOT NULL,
    month       TINYINT     NOT NULL,
    is_locked   BOOLEAN     NOT NULL DEFAULT FALSE,
    locked_at   DATETIME(6),
    locked_by   VARCHAR(100),
    unlocked_at DATETIME(6),
    unlocked_by VARCHAR(100),
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_mc_year_month (year, month),
    INDEX idx_mc_locked (is_locked),

    CONSTRAINT chk_mc_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_mc_year  CHECK (year >= 2020 AND year <= 2099)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. KKTS — Kiểm Kê Tài Sản (bảng kê tồn kho theo kỳ tháng/năm)
-- ============================================================
-- Mỗi product có 1 dòng KKTS cho mỗi kỳ (month/year)
-- Dùng cùng công thức tính toán như Inventory
-- Entity: Kkts.java
-- ============================================================
CREATE TABLE IF NOT EXISTS kkts (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT         NOT NULL,
    year             SMALLINT       NOT NULL,
    month            TINYINT        NOT NULL,
    opening_quantity DECIMAL(18, 3) NOT NULL DEFAULT 0,
    opening_amount   DECIMAL(18, 0) NOT NULL DEFAULT 0,
    import_quantity  DECIMAL(18, 3) NOT NULL DEFAULT 0,
    import_amount    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    export_quantity  DECIMAL(18, 3) NOT NULL DEFAULT 0,
    export_amount    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    ending_quantity  DECIMAL(18, 3) NOT NULL DEFAULT 0,
    ending_amount    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    average_price    DECIMAL(18, 0) NOT NULL DEFAULT 0,
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_kkts_product_period (product_id, year, month),
    INDEX idx_kkts_period (year, month),

    CONSTRAINT fk_kkts_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_kkts_month  CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_kkts_year   CHECK (year >= 2020 AND year <= 2099)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. AUDIT_LOGS (nhật ký thao tác)
-- ============================================================
-- Ghi lại mọi thao tác quan trọng: CREATE, UPDATE, DELETE,
-- CANCEL, LOCK_MONTH, UNLOCK_MONTH, LOGIN, DEACTIVATE
-- Entity: AuditLog.java
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    entity_name VARCHAR(100),
    entity_id   VARCHAR(50),
    old_value   TEXT,
    new_value   TEXT,
    description TEXT,
    ip_address  VARCHAR(50),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_al_username (username),
    INDEX idx_al_action (action),
    INDEX idx_al_entity (entity_name, entity_id),
    INDEX idx_al_created_at (created_at),
    -- Composite: tìm log theo user + thời gian (phổ biến nhất)
    INDEX idx_al_user_time (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- SEED DATA — Dữ liệu danh mục cơ bản
-- ============================================================

-- Vai trò hệ thống
INSERT IGNORE INTO roles (name, description) VALUES
    ('ADMIN',      'Quản trị hệ thống'),
    ('USER',       'Nhân viên quản lý kho'),
    ('CONTROLLER', 'Kiểm soát viên');

-- Nhóm hàng (PRODUCT_GROUP)
INSERT IGNORE INTO categories (type, code, name, is_active) VALUES
    ('PRODUCT_GROUP', 'VPP',  'Văn phòng phẩm',        TRUE),
    ('PRODUCT_GROUP', 'ACTT', 'Ấn chỉ thông thường',   TRUE),
    ('PRODUCT_GROUP', 'ACQT', 'Ấn chỉ quan trọng',     TRUE),
    ('PRODUCT_GROUP', 'VLK',  'Vật liệu khác',         TRUE);

-- Đơn vị tính (UNIT)
INSERT IGNORE INTO categories (type, code, name, is_active) VALUES
    ('UNIT', 'CAI',   'Cái',   TRUE),
    ('UNIT', 'HOP',   'Hộp',   TRUE),
    ('UNIT', 'QUYEN', 'Quyển', TRUE),
    ('UNIT', 'BO',    'Bộ',    TRUE),
    ('UNIT', 'REAM',  'Ream',  TRUE),
    ('UNIT', 'CUON',  'Cuộn',  TRUE),
    ('UNIT', 'BINH',  'Bình',  TRUE),
    ('UNIT', 'GOI',   'Gói',   TRUE),
    ('UNIT', 'THUNG', 'Thùng', TRUE);
