-- =====================================================================
--  MediTrace - database schema (MySQL 8)
--  Run:  mysql -u root -p < sql/schema.sql      then   sql/seed.sql
--  WARNING: drops and re-creates all MediTrace tables.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS meditrace
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE meditrace;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS reports, scans, recalls, transactions, blocks, packs, batches, users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. users : every supply-chain participant (one table, role column)
--    password_hash = PBKDF2-HMAC-SHA512, format  pbkdf2_sha512$iter$salt$hash
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id        BIGINT        NOT NULL AUTO_INCREMENT,
    username       VARCHAR(30)   NOT NULL,
    email          VARCHAR(120)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    full_name      VARCHAR(100)  NOT NULL,
    role           ENUM('MANUFACTURER','DISTRIBUTOR','PHARMACY') NOT NULL,
    company        VARCHAR(120)  NOT NULL,
    city           VARCHAR(60)   NOT NULL,
    license_no     VARCHAR(40)   NULL COMMENT 'Drug licence / manufacturing licence number',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role (role)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. batches : one production batch of a medicine, created by a manufacturer
-- ---------------------------------------------------------------------
CREATE TABLE batches (
    batch_id         BIGINT         NOT NULL AUTO_INCREMENT,
    batch_no         VARCHAR(20)    NOT NULL COMMENT 'Printed batch number, e.g. B1023',
    medicine_name    VARCHAR(100)   NOT NULL,
    strength         VARCHAR(30)    NOT NULL,
    dosage_form      VARCHAR(30)    NOT NULL,
    manufacturer_id  BIGINT         NOT NULL,
    mfg_date         DATE           NOT NULL,
    expiry_date      DATE           NOT NULL,
    mrp              DECIMAL(10, 2) NOT NULL COMMENT 'Maximum Retail Price in INR',
    pack_count       INT            NOT NULL,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (batch_id),
    UNIQUE KEY uq_batches_batch_no (batch_no),
    KEY idx_batches_manufacturer (manufacturer_id),
    CONSTRAINT fk_batches_manufacturer FOREIGN KEY (manufacturer_id) REFERENCES users (user_id),
    CONSTRAINT chk_batches_dates CHECK (expiry_date > mfg_date),
    CONSTRAINT chk_batches_mrp   CHECK (mrp > 0),
    CONSTRAINT chk_batches_count CHECK (pack_count > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. packs : one row per physical pack; pack_code is what the QR encodes
-- ---------------------------------------------------------------------
CREATE TABLE packs (
    pack_id            BIGINT       NOT NULL AUTO_INCREMENT,
    pack_code          VARCHAR(40)  NOT NULL COMMENT 'e.g. MT-B1023-7F3A9C',
    batch_id           BIGINT       NOT NULL,
    status             ENUM('IN_CHAIN','SOLD') NOT NULL DEFAULT 'IN_CHAIN',
    current_holder_id  BIGINT       NULL COMMENT 'Who holds the pack now',
    sold_city          VARCHAR(60)  NULL,
    sold_at            DATETIME     NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pack_id),
    UNIQUE KEY uq_packs_code (pack_code),
    KEY idx_packs_batch (batch_id),
    KEY idx_packs_holder_status (current_holder_id, status),
    CONSTRAINT fk_packs_batch  FOREIGN KEY (batch_id) REFERENCES batches (batch_id),
    CONSTRAINT fk_packs_holder FOREIGN KEY (current_holder_id) REFERENCES users (user_id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4. blocks : the persisted blockchain (append-only)
--    hash = SHA-256(block_index + prev_hash + timestamp + data_json + nonce)
--    data_json is TEXT, NOT the MySQL JSON type: JSON columns re-format the text
--    (key order / spacing), which would change the hash and break verification.
-- ---------------------------------------------------------------------
CREATE TABLE blocks (
    block_index  INT           NOT NULL,
    prev_hash    CHAR(64)      NOT NULL,
    hash         CHAR(64)      NOT NULL,
    nonce        BIGINT        NOT NULL,
    `timestamp`  BIGINT        NOT NULL COMMENT 'Epoch milliseconds',
    data_json    TEXT          NOT NULL,
    signature    VARCHAR(512)  NULL COMMENT 'Reserved for digital signatures (roadmap)',
    PRIMARY KEY (block_index),
    UNIQUE KEY uq_blocks_hash (hash)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 5. transactions : supply-chain events, each sealed into exactly one block
--    block_index stays NULL until the Miner thread mines the block.
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    tx_id         BIGINT        NOT NULL AUTO_INCREMENT,
    batch_id      BIGINT        NOT NULL,
    tx_type       ENUM('REGISTER','HANDOVER') NOT NULL,
    from_user_id  BIGINT        NOT NULL,
    to_user_id    BIGINT        NULL COMMENT 'NULL for REGISTER',
    quantity      INT           NOT NULL,
    tx_time       DATETIME      NOT NULL,
    block_index   INT           NULL,
    remarks       VARCHAR(255)  NULL,
    PRIMARY KEY (tx_id),
    UNIQUE KEY uq_transactions_block (block_index),
    KEY idx_transactions_batch (batch_id),
    KEY idx_transactions_from (from_user_id),
    KEY idx_transactions_to (to_user_id),
    CONSTRAINT fk_tx_batch FOREIGN KEY (batch_id) REFERENCES batches (batch_id),
    CONSTRAINT fk_tx_from  FOREIGN KEY (from_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_tx_to    FOREIGN KEY (to_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_tx_block FOREIGN KEY (block_index) REFERENCES blocks (block_index),
    CONSTRAINT chk_tx_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 6. scans : every customer verification, including unknown codes
-- ---------------------------------------------------------------------
CREATE TABLE scans (
    scan_id     BIGINT       NOT NULL AUTO_INCREMENT,
    pack_code   VARCHAR(40)  NOT NULL COMMENT 'As entered; no FK so unknown codes are logged too',
    pack_id     BIGINT       NULL,
    result      ENUM('GENUINE','TAMPERED','POSSIBLE_FAKE','NOT_FOUND') NOT NULL,
    city        VARCHAR(60)  NULL,
    ip_address  VARCHAR(45)  NULL COMMENT 'IPv4 or IPv6',
    user_agent  VARCHAR(255) NULL,
    scanned_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scan_id),
    KEY idx_scans_pack_code (pack_code),
    KEY idx_scans_scanned_at (scanned_at),
    CONSTRAINT fk_scans_pack FOREIGN KEY (pack_id) REFERENCES packs (pack_id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 7. reports : customer complaints about suspected fakes
-- ---------------------------------------------------------------------
CREATE TABLE reports (
    report_id       BIGINT         NOT NULL AUTO_INCREMENT,
    pack_code       VARCHAR(40)    NOT NULL,
    scan_id         BIGINT         NULL,
    reporter_name   VARCHAR(100)   NULL,
    reporter_phone  VARCHAR(15)    NULL,
    city            VARCHAR(60)    NULL,
    description     VARCHAR(1000)  NOT NULL,
    status          ENUM('OPEN','INVESTIGATING','CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    KEY idx_reports_city (city),
    KEY idx_reports_status (status),
    CONSTRAINT fk_reports_scan FOREIGN KEY (scan_id) REFERENCES scans (scan_id) ON DELETE SET NULL
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 8. recalls : batches withdrawn from the market
-- ---------------------------------------------------------------------
CREATE TABLE recalls (
    recall_id    BIGINT        NOT NULL AUTO_INCREMENT,
    batch_id     BIGINT        NOT NULL,
    issued_by    BIGINT        NOT NULL,
    reason       VARCHAR(500)  NOT NULL,
    severity     ENUM('CLASS_I','CLASS_II','CLASS_III') NOT NULL,
    recall_date  DATE          NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (recall_id),
    KEY idx_recalls_batch_active (batch_id, active),
    CONSTRAINT fk_recalls_batch  FOREIGN KEY (batch_id) REFERENCES batches (batch_id),
    CONSTRAINT fk_recalls_issuer FOREIGN KEY (issued_by) REFERENCES users (user_id)
) ENGINE = InnoDB;
