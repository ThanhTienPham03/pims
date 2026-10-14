CREATE DATABASE IF NOT EXISTS ims
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ims;

-- 1. ROLES
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200),

    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. USERS
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

-- 3. USER_ROLES
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. PRODUCT_GROUPS
CREATE TABLE IF NOT EXISTS product_groups (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. UNITS
CREATE TABLE IF NOT EXISTS units (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. PRODUCTS
CREATE TABLE IF NOT EXISTS products (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    ma_so            VARCHAR(50)    NOT NULL,
    ten_hang         VARCHAR(300)   NOT NULL,
    product_group_id BIGINT         NOT NULL,
    unit_id          BIGINT         NOT NULL,
    don_gia_nhap     DECIMAL(18, 0) NOT NULL DEFAULT 0,
    trang_thai       VARCHAR(20)    NOT NULL DEFAULT 'HOAT_DONG',
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),

    UNIQUE KEY uq_products_ma_so (ma_so),
    INDEX idx_products_status (trang_thai),
    INDEX idx_products_group (product_group_id),
    INDEX idx_products_unit (unit_id),
    INDEX idx_products_ten_hang (ten_hang(100)),

    CONSTRAINT fk_prod_group FOREIGN KEY (product_group_id) REFERENCES product_groups(id),
    CONSTRAINT fk_prod_unit  FOREIGN KEY (unit_id)          REFERENCES units(id),
    CONSTRAINT chk_products_status CHECK (trang_thai IN ('HOAT_DONG', 'NGUNG_SU_DUNG')),
    CONSTRAINT chk_products_price  CHECK (don_gia_nhap >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. INVENTORY
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
    INDEX idx_inv_ending_qty (ending_quantity),

    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_inv_quantities CHECK (
        opening_quantity >= 0 AND import_quantity >= 0 AND
        export_quantity >= 0 AND ending_quantity >= 0 AND average_price >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. INVENTORY_TRANSACTIONS
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id                   BIGINT         AUTO_INCREMENT PRIMARY KEY,
    transaction_code     VARCHAR(50)    NOT NULL,
    type                 VARCHAR(20)    NOT NULL,
    transaction_date     DATE           NOT NULL,
    source               VARCHAR(300),
    destination          VARCHAR(300),
    reason               VARCHAR(500),
    invoice_number       VARCHAR(100),
    invoice_date         DATE,
    debit_account        VARCHAR(20),
    credit_account       VARCHAR(20),
    total_amount         DECIMAL(18, 0) NOT NULL DEFAULT 0,
    status               VARCHAR(20)    NOT NULL DEFAULT 'HOAN_THANH',
    group_representative VARCHAR(10),
    workflow_key         VARCHAR(100),
    workflow_status      VARCHAR(20)    DEFAULT 'COMPLETED',
    created_by           VARCHAR(100),
    created_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE KEY uq_tx_type_code (type, transaction_code),
    INDEX idx_tx_type (type),
    INDEX idx_tx_date (transaction_date),
    INDEX idx_tx_status (status),
    INDEX idx_tx_type_date (type, transaction_date),
    INDEX idx_tx_status_type (status, type),

    CONSTRAINT chk_tx_type   CHECK (type IN ('IMPORT', 'EXPORT', 'ADJUSTMENT', 'CHECK')),
    CONSTRAINT chk_tx_status CHECK (status IN ('HOAN_THANH', 'HUY')),
    CONSTRAINT chk_tx_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. INVENTORY_TRANSACTION_DETAILS
CREATE TABLE IF NOT EXISTS inventory_transaction_details (
    id                 BIGINT         AUTO_INCREMENT PRIMARY KEY,
    transaction_id     BIGINT         NOT NULL,
    product_id         BIGINT         NOT NULL,
    product_group_code VARCHAR(10)    NOT NULL,
    quantity           DECIMAL(18, 3) NOT NULL,
    unit_price         DECIMAL(18, 0) NOT NULL,
    amount             DECIMAL(18, 0) NOT NULL,
    created_at         DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_txd_transaction (transaction_id),
    INDEX idx_txd_product (product_id),
    INDEX idx_txd_product_tx (product_id, transaction_id),

    CONSTRAINT fk_txd_transaction FOREIGN KEY (transaction_id) REFERENCES inventory_transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_txd_product     FOREIGN KEY (product_id)     REFERENCES products(id),
    CONSTRAINT chk_txd_quantity   CHECK (quantity > 0),
    CONSTRAINT chk_txd_price      CHECK (unit_price >= 0),
    CONSTRAINT chk_txd_amount     CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. DOCUMENT_SEQUENCES
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

-- 11. MONTHLY_CLOSINGS
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

-- 12. KKTS
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

-- 13. AUDIT_LOGS
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
    INDEX idx_al_user_time (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SEED DATA
INSERT IGNORE INTO roles (name, description) VALUES
    ('ADMIN',      'Quản trị hệ thống'),
    ('USER',       'Nhân viên quản lý kho'),
    ('CONTROLLER', 'Kiểm soát viên');

INSERT IGNORE INTO product_groups (code, name, is_active) VALUES
    ('VPP',  'Văn phòng phẩm',        TRUE),
    ('ACTT', 'Ấn chỉ thông thường',   TRUE),
    ('ACQT', 'Ấn chỉ quan trọng',     TRUE),
    ('VLK',  'Vật liệu khác',         TRUE);

INSERT IGNORE INTO units (code, name, is_active) VALUES
    ('CAI',   'Cái',   TRUE),
    ('HOP',   'Hộp',   TRUE),
    ('QUYEN', 'Quyển', TRUE),
    ('BO',    'Bộ',    TRUE),
    ('REAM',  'Ream',  TRUE),
    ('CUON',  'Cuộn',  TRUE),
    ('BINH',  'Bình',  TRUE),
    ('GOI',   'Gói',   TRUE),
    ('THUNG', 'Thùng', TRUE);
