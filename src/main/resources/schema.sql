-- ========================================
-- 银行资产托管 5 业务表 DDL (MySQL 9.x)
-- 兼容: MySQL 8.0+ / 9.0+
-- 注意: MySQL 不支持 CREATE INDEX IF NOT EXISTS,
--       我们用 UNIQUE 约束自动建索引, 不写显式 INDEX
-- ========================================
-- 注意: Flowable 会自动建 23 张 ACT_* 表
-- 本文件只建业务表 + 种子数据

-- ===== 1. 凭证 (Voucher) - 登账流程 =====
CREATE TABLE IF NOT EXISTS voucher (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_key VARCHAR(64) NOT NULL UNIQUE,
    amount       DECIMAL(18,2) NOT NULL,
    account_no   VARCHAR(32) NOT NULL,
    trade_date   DATE NOT NULL,
    operator     VARCHAR(32) NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===== 2. 清算批次 (Clearing Batch) - 清算流程 =====
CREATE TABLE IF NOT EXISTS clearing_batch (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no      VARCHAR(32) NOT NULL UNIQUE,
    trade_count   INT NOT NULL DEFAULT 0,
    total_amount  DECIMAL(18,2) NOT NULL DEFAULT 0,
    operator      VARCHAR(32) NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at  DATETIME
);

-- ===== 3. 对账日志 (Reconciliation Log) - 对账流程 =====
CREATE TABLE IF NOT EXISTS reconciliation_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_key VARCHAR(64) NOT NULL UNIQUE,
    diff_count   INT NOT NULL DEFAULT 0,
    diff_amount  DECIMAL(18,2) NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL,
    operator     VARCHAR(32),
    note         VARCHAR(500),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===== 4. 持仓 (Holding) - 估值流程前置数据 =====
CREATE TABLE IF NOT EXISTS holding (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_code      VARCHAR(16) NOT NULL,
    security_code  VARCHAR(16) NOT NULL,
    quantity       DECIMAL(18,2) NOT NULL,
    market_price   DECIMAL(18,4) NOT NULL,
    market_value   DECIMAL(18,2) NOT NULL,
    valuation_date DATE NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===== 5. 基金估值 (Fund Valuation) - 估值流程 =====
CREATE TABLE IF NOT EXISTS fund_valuation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_key VARCHAR(64) NOT NULL UNIQUE,
    fund_code    VARCHAR(16) NOT NULL,
    total_asset  DECIMAL(18,2) NOT NULL,
    share_count  BIGINT NOT NULL,
    nav          DECIMAL(18,4) NOT NULL,
    nav_date     DATE NOT NULL,
    operator     VARCHAR(32) NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at DATETIME
);

-- ===== 6. 初始测试数据 (持仓 - 估值用) =====
INSERT INTO holding (fund_code, security_code, quantity, market_price, market_value, valuation_date)
SELECT 'FUND001', 'SH600519', 1000, 1680.50, 1680500.00, CURRENT_DATE
WHERE NOT EXISTS (SELECT 1 FROM holding WHERE fund_code = 'FUND001' AND security_code = 'SH600519');

INSERT INTO holding (fund_code, security_code, quantity, market_price, market_value, valuation_date)
SELECT 'FUND001', 'SZ000858', 2000, 165.30, 330600.00, CURRENT_DATE
WHERE NOT EXISTS (SELECT 1 FROM holding WHERE fund_code = 'FUND001' AND security_code = 'SZ000858');

INSERT INTO holding (fund_code, security_code, quantity, market_price, market_value, valuation_date)
SELECT 'FUND001', 'SH601318', 5000, 48.20, 241000.00, CURRENT_DATE
WHERE NOT EXISTS (SELECT 1 FROM holding WHERE fund_code = 'FUND001' AND security_code = 'SH601318');

-- 期望: FUND001 总市值 ≈ 225.21 万
